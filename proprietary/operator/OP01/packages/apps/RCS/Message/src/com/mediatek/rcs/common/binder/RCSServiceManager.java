package com.mediatek.rcs.common.binder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.gsma.joyn.JoynServiceConfiguration;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.common.IBurnMessageCapabilityListener;
import com.mediatek.rcs.common.INotifyListener;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.IpMessageConsts.FeatureId;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.utils.Logger;


public class RCSServiceManager {

    private static final String TAG = "RCSServiceManager";

    private final String RCS_CONFIGURATION_CHANGED_ACTION = "com.orangelabs.rcs.CONFIGURATION_STATUS_TO_APP";
    private final String RCS_SERVICE_STATUS_CHANGED_ON_ACTION = "com.mediatek.intent.rcs.stack.LaunchService";
    private final String RCS_SERVICE_STATUS_CHANGED_OFF_ACTION = "com.mediatek.intent.rcs.stack.StopService";
    private final String RCS_SERVICE_NOTIFY_ACTION = "org.gsma.joyn.action.VIEW_SETTINGS";
    public static final String RCS_SERVICE_MANAGER_READY
                                                   = "com.mediatek.rcs.message.service.initiated";
    public static final String RCS_SERVICE_MANAGER_READY_CONFIGRUATION_STATE_KEY = "configured";

    private final static int RCS_CORE_LOADED = 0;
    private final static int RCS_CORE_STARTED = 2;
    private final static int RCS_CORE_STOPPED = 3;
    private final static int RCS_CORE_IMS_CONNECTED = 4;
    private final static int RCS_CORE_IMS_DISCONNECTED = 9;

    private boolean mServiceActivated = false; //whether service is on or off
    private boolean mServiceConfigured = false;//whether configuration completed
    private boolean mServiceRegistered = false; // whether service registered
    private String mNumber = null;

    private static RCSServiceManager sInstance = null;
    private IRCSChatService mChatService = null;
    private Context mContext = null;
    private IRCSChatServiceListener mListener = null;

    private HashSet<INotifyListener> mNotifyListeners = new HashSet<INotifyListener>();
    
    private HashSet<IBurnMessageCapabilityListener> mBurnMsgCapListeners =
                                                new HashSet<IBurnMessageCapabilityListener>();

    private Map<String, GroupInfo> mInvitingGroup = new ConcurrentHashMap<String, GroupInfo>();
    private HashSet<OnServiceChangedListener> mServiceChangedListeners = 
            new HashSet<RCSServiceManager.OnServiceChangedListener>();

    private RCSServiceManager(Context context) {
        Logger.d(TAG, "call constructor");
        mContext = context;
        bindRemoteService();
        mServiceActivated = JoynServiceConfiguration.isServiceActivated(mContext);
        String number = getMyNumber();
        mServiceConfigured = !TextUtils.isEmpty(number);
        IntentFilter filter = new IntentFilter(RCS_CONFIGURATION_CHANGED_ACTION);
        filter.addAction(RCS_SERVICE_STATUS_CHANGED_ON_ACTION);
        filter.addAction(RCS_SERVICE_STATUS_CHANGED_OFF_ACTION);
        filter.addAction(RCS_SERVICE_NOTIFY_ACTION);
        context.registerReceiver(mRecever, filter);
        Intent intent = new Intent(RCS_SERVICE_MANAGER_READY);
        intent.putExtra(RCS_SERVICE_MANAGER_READY_CONFIGRUATION_STATE_KEY, mServiceConfigured);
        mContext.sendBroadcast(intent);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d(TAG, "service connect!!");
            mChatService = IRCSChatService.Stub.asInterface(service);
            mListener = new RCSChatServiceListener();
            try {
                mChatService.addRCSChatServiceListener(mListener);
                List<String> chatIds = RCSDataBaseUtils.getAvailableGroupChatIds();
                mChatService.startGroups(chatIds);
                RCSMessageManager.getInstance(mContext).deleteLastBurnedMessage();
                updateServiceRegistrationState();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            Logger.d(TAG, "service disconnect!!");
            mChatService = null;
            bindRemoteService();
        }
    };

    private BroadcastReceiver mRecever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mRecever: " + action);
            if (action == null) {
                return;
            }
            if (action.equals(RCS_CONFIGURATION_CHANGED_ACTION)) {
                updateServiceConfigurationState();
            } else if (action.equals(RCS_SERVICE_STATUS_CHANGED_ON_ACTION)) {
                setServiceActivated(true);
            } else if (action.equals(RCS_SERVICE_STATUS_CHANGED_OFF_ACTION)) {
                setServiceActivated(false);
            } else if (action.equals(RCS_SERVICE_NOTIFY_ACTION)) {
                int state = intent.getIntExtra("label_enum", -1);
                Logger.d(TAG, "state = " + state);
                if (state == RCS_CORE_LOADED || state == RCS_CORE_STARTED || state == RCS_CORE_STOPPED ||
                        state == RCS_CORE_IMS_CONNECTED ||state == RCS_CORE_IMS_DISCONNECTED) {
                    updateServiceState();
                }
            }
        }
    };

    /**
     * Get service Listeners.
     * @return IRCSChatServiceListener
     */
    public IRCSChatServiceListener getServiceListener() {
        return mListener;
    }

    /**
     * Regist notify listener.
     * @param listener INotifyListener
     */
    public void registNotifyListener(INotifyListener listener) {
        mNotifyListeners.add(listener);
    }

    /**
     * UnregistNotifyListener.
     * @param listener INotifyListener
     */
    public void unregistNotifyListener(INotifyListener listener) {
        mNotifyListeners.remove(listener);
    }

    /**
     * registBurnMsgCapListener.
     * @param listener IBurnMessageCapabilityListener
     */
    public void registBurnMsgCapListener(IBurnMessageCapabilityListener listener) {
        mBurnMsgCapListeners.add(listener);
    }

    /**
     * unregistBurnMsgCapListener.
     * @param listener IBurnMessageCapabilityListener
     */
    public void unregistBurnMsgCapListener(IBurnMessageCapabilityListener listener) {
        mBurnMsgCapListeners.remove(listener);
    }

    /**
     * callNotifyListeners.
     * @param intent
     */
    public void callNotifyListeners(Intent intent) {
        Logger.d(TAG, "callNotifyListeners, action=" + intent.getAction());
        for (INotifyListener listener : mNotifyListeners) {
            listener.notificationsReceived(intent);
        }
    }

    /**
     * callBurnCapListener.
     * @param contact
     * @param result
     */
    public void callBurnCapListener(String contact, boolean result) {
        for (IBurnMessageCapabilityListener listener : mBurnMsgCapListeners) {
            listener.onRequestBurnMessageCapabilityResult(contact, result);
        }
    }

    /**
     * service is ready.
     * @return  return true if service is registered or return false.
     */
    public boolean serviceIsReady() {
        Log.d(TAG, "serviceIsReady() entry");
        boolean status = false;
        try {
            if (mChatService != null) {
                status = mChatService.getRegistrationStatus();
            }
            setServiceRegistrationState(status);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return status;
    }

    /**
     * get own number.
     * @return null if not configured
     */
    public String getMyNumber() {
        if (TextUtils.isEmpty(mNumber)) {
            String publicUri = new JoynServiceConfiguration().getPublicUri(mContext);
            if (TextUtils.isEmpty(publicUri)) {
                return null;
            }
            if (publicUri.indexOf('@') != -1) {
                mNumber = publicUri.substring(publicUri.indexOf(':') + 1, publicUri.indexOf('@'));
            } else {
                mNumber = publicUri.substring(publicUri.indexOf(':') + 1);
            }
        }
        return mNumber;
    }

    private void updateServiceActivatedState() {
        boolean activate = JoynServiceConfiguration.isServiceActivated(mContext);;
        if (mServiceActivated != activate) {
            mServiceActivated = activate;
            notifyServiceStateChanged();
        }
    }

    private void updateServiceConfigurationState() {
//        boolean configured = new JoynServiceConfiguration().getConfigurationState(mContext);
        //TODO should replace the code instead of above after autoConfiguration @{
        boolean configured = !TextUtils.isEmpty(getMyNumber());
        //@}
        if (mServiceConfigured != configured) {
            mServiceConfigured = configured;
            notifyServiceStateChanged();
        }
    }

    private void updateServiceRegistrationState() {
        boolean registered = false;
        try {
            if (mChatService != null) {
                registered = mChatService.getRegistrationStatus();
            }
            setServiceRegistrationState(registered);
        } catch (RemoteException e) {
            e.printStackTrace();
            Logger.d(TAG, "updateServiceRegistrationState: e = " + e);
        }
    }

    private void updateServiceState() {
        boolean needNotify = false;
        boolean activated = JoynServiceConfiguration.isServiceActivated(mContext);
        if (mServiceActivated != activated) {
            mServiceActivated = activated;
            needNotify = true;
        }

        String number = getMyNumber();
        boolean configured = !TextUtils.isEmpty(number);
        if (mServiceConfigured != configured) {
            mServiceConfigured = configured;
            needNotify = true;
        }

        try {
            if (mChatService != null) {
                boolean registed = mChatService.getRegistrationStatus();
                if (mServiceRegistered != registed) {
                    needNotify = true;
                }
                //TODO: temp process, should remove the code after auto configuration @{
                if (mServiceRegistered == true) {
                    if (mServiceConfigured != true) {
                        mServiceConfigured = true;
                        needNotify = true;
                    }
                }
                // @}
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Logger.d(TAG, "updateServiceState: e = " + e);
        }
        Log.d(TAG, "updateServiceState: needNotify = " + needNotify);
        if (needNotify) {
            notifyServiceStateChanged();
        }
    }

    /**
     * whether service is enabled.
     * @return true if service is activated and configured
     */
    public boolean isServiceEnabled() {
        return (mServiceConfigured && mServiceActivated);
    }

    private void bindRemoteService() {
        Logger.d(TAG, "bindRemoteService");
        Intent intent = new Intent("com.mediatek.rcs.RemoteService");
        intent.setPackage("com.android.mms");
        mContext.startService(intent);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void setServiceActivated(boolean activated) {
        Logger.d(TAG, "setServiceActivated: " + activated);
        if (mServiceActivated != activated) {
            mServiceActivated = activated;
            notifyServiceStateChanged();
        }
    }
    public boolean isServiceActivated() {
        return mServiceActivated;
    }

    private void setServiceRegistrationState(boolean registered) {
        Logger.d(TAG, "setServiceRegistrationState: " + registered);
        if (mServiceRegistered != registered) {
            mServiceRegistered = registered;
            notifyServiceStateChanged();
        }
    }

    private void notifyServiceStateChanged() {
        Logger.d(TAG, "notifyServiceStateChanged: mServiceActivated = " + mServiceActivated +
                ", mServiceConfigured = " + mServiceConfigured + 
                ", mServiceRegistered = " + mServiceRegistered);
        for (OnServiceChangedListener l : mServiceChangedListeners) {
            l.onServiceStateChanged(mServiceActivated, mServiceConfigured, mServiceRegistered);
        }
    }

    /**
     *  Is Service configured.
     * @return return true is configuration success full or turn false
     */
    public boolean isServiceConfigured() {
        return mServiceConfigured;
    }

    /**
     * Is service turn on or turn off.
     * @param subId : subid
     * @return true if service turn on or return false
     */
    public boolean isActivated(int subId) {
        Log.d(TAG, "isActivated(int subId) entry with simId is " + subId);
        return true;
    }

    public boolean isFeatureSupported(int featureId) {
        Log.d(TAG, "isFeatureSupported() featureId is " + featureId);
        switch (featureId) {
            case FeatureId.FILE_TRANSACTION:
            case FeatureId.EXTEND_GROUP_CHAT:
            case FeatureId.GROUP_MESSAGE:
                return true;
            case FeatureId.PARSE_EMO_WITHOUT_ACTIVATE:
            case FeatureId.CHAT_SETTINGS:
            case FeatureId.ACTIVITION:
            case FeatureId.ACTIVITION_WIZARD:
            case FeatureId.MEDIA_DETAIL:
            case FeatureId.TERM:
             case FeatureId.EXPORT_CHAT:
            case FeatureId.APP_SETTINGS:
                  return false;
            default:
                return false;
        }
    }

    /**
     * CreateManager instance.
     * @param context Context
     */
    public static void createManager(Context context) {
        Logger.d(TAG, "createManager, entry");
        if (sInstance == null) {
            sInstance = new RCSServiceManager(context);
        }
    }

    /**
     * Get instance.
     * @return RCSServiceManager instance
     */
    public static RCSServiceManager getInstance() {
        return sInstance;
    }

    /**
     * Get chat Service.
     * @return IRCSChatService
     */
    public IRCSChatService getChatService() {
        int i = 0;
        while (mChatService == null && i++ < 3) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mChatService;
    }

    /**
     * GetBurnMsgCap.
     * @param contact
     * @return
     */
    public boolean getBurnMsgCap(String contact) {
        Logger.d(TAG, "getBurnMsgCap for " + contact);
        boolean cap = false;
        try {
            if (mChatService != null) {
                mChatService.getBurnMessageCapability(contact);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return cap;
    }

    /**
     *Record inviting Group.
     * @param chatId
     * @param subject
     * @param participants
     */
    public void recordInvitingGroup(String chatId, String subject, List<String> participants) {
        mInvitingGroup.put(chatId, new GroupInfo(chatId, subject, participants));
    }

    /**
     * Get inviting group.
     * @param chatId
     * @return
     */
    public GroupInfo getInvitingGroup(String chatId) {
        return mInvitingGroup.get(chatId);
    }

    /**
     * Remove inviting group.
     * @param chatId
     */
    public void removeInvitingGroup(String chatId) {
        mInvitingGroup.remove(chatId);
    }

    /**
     *  Class GroupInfo.
     * @author
     *
     */
    class GroupInfo {
        private List<String> mParticipants;
        private String mSubject;
        private String mChatId;
        
        public GroupInfo(String chatId, String subject, List<String> participants) {
            mParticipants = participants;
            mSubject = subject;
            mChatId = chatId;
        }

        public String getSubject() {
            return mSubject;
        }

        public List<String> getParticipants() {
            return mParticipants;
        }
    }

    @Override
    public void finalize() {
        try {
            Logger.d(TAG, "finalize()!!");
            mContext.unbindService(mConnection);
            mContext.unregisterReceiver(mRecever);
            super.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen rcs service state changed.
     * @author
     *
     */
    public interface OnServiceChangedListener {
        /**
         * 
         * @param activated  whether service is turn on, true is on, false is off
         * @param configured whether service is configured, true is configured
         * @param registered whether service is registered 
         */
        public void onServiceStateChanged(boolean activated, boolean configured, boolean registered);
    }

    /**
     * Add service changed listener.
     * @param l OnServiceChangedListener
     * @return  return true if add successful or return false
     */
    public boolean addOnServiceChangedListener(OnServiceChangedListener l) {
        return mServiceChangedListeners.add(l);
    }

    /**
     * Remove service changed listener.
     * @param l OnServiceChangedListener
     * @return  return true if remove successful or return false
     */
    public boolean removeOnServiceChangedListener(OnServiceChangedListener l) {
        return mServiceChangedListeners.remove(l);
    }
}
