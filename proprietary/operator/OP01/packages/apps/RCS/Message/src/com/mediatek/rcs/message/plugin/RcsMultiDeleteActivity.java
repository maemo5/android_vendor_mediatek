package com.mediatek.rcs.message.plugin;


import com.mediatek.mms.ipmessage.IpMultiDeleteActivity;
import com.mediatek.mms.ipmessage.IpMultiDeleteActivityCallback;
import com.mediatek.rcs.message.provider.FavoriteMsgProvider;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

//favorite
import android.content.ContentUris;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.pdu.GenericPdu;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;

import com.mediatek.rcs.message.R;

public class RcsMultiDeleteActivity extends IpMultiDeleteActivity {
    private static String TAG = "RcsMultiDeleteActivity";
    
    public static final String IPMSG_IDS = "forward_ipmsg_ids";
    private static final int MENU_FAVORITE = 1001;
    /// M: add for ipmessage, record the ipmessage id.
    private Map<Long, Long> mSelectedIpMessageIds = new HashMap<Long, Long>();
    private Activity mContext;
    private Context mPluginContext;
    private long mThreadId;
    private int mForwardMenuId;
    private String mChatId;
    
    //favorite
    private String COLUMN_NAME_ID = "_id";
    private String COLUMN_NAME_TYPE = "m_type";
    private String COLUMN_NAME_DATE = "date";
    private String COLUMN_NAME_MESSAGE_BOX = "msg_box";
    private String COLUMN_NAME_READ = "read";
    private String FILE_EXT_PDU = ".pdu";
    
    private ProgressDialog mProgressDialog;
    
    private static final String[] SMS_PROJECTION =
    { "_id", "body", "address", "ipmsg_id"};
    
    private IpMultiDeleteActivityCallback mCallback;
    
    @Override
    public boolean MultiDeleteActivityInit(Activity context, IpMultiDeleteActivityCallback callback) {
        mContext = context;
        mCallback = callback;
        mPluginContext = ContextCacher.getPluginContext();
        mThreadId = mContext.getIntent().getLongExtra("thread_id", 0);
        mChatId = RcsMessageUtils.getGroupChatIdByThread(mThreadId);
        return true;
    }
    
    @Override
    public boolean onIpMultiDeleteClick(boolean deleteLocked) {
        /// M: delete ipmessage
        if (mSelectedIpMessageIds.size() > 0) {
            Iterator<Entry<Long, Long>> iter = mSelectedIpMessageIds.entrySet().iterator();
            HashSet<Long> mIpMessageIds = new HashSet<Long>();
            while (iter.hasNext()) {
                 Map.Entry<Long, Long> entry = iter.next();
                 mIpMessageIds.add(entry.getValue());
            }
            RCSMessageManager.getInstance(mContext).deleteMultiRCSMsg(mThreadId, mIpMessageIds);
            Log.d(TAG, "delete ipmessage, id:" + mSelectedIpMessageIds.size());
            mSelectedIpMessageIds.clear();
            return true;
        }
        return false;
    }

	@Override
    public boolean onIpDeleteLockedIpMsg(long msgId) {
        if (mSelectedIpMessageIds.size() > 0 && mSelectedIpMessageIds.containsKey(msgId)) {
            mSelectedIpMessageIds.remove(msgId);
			return true;
		}
        return false;
    }

    @Override
    public boolean onIpHandleItemClick(long ipMessageId, boolean isSelected, long msgId) {
        if (ipMessageId != 0) {
            if (isSelected) {
                mSelectedIpMessageIds.put(msgId, ipMessageId);
            } else {
                mSelectedIpMessageIds.remove(msgId);
            }
        }
        return true;
    }

    @Override
    public boolean onIpMarkCheckedState() {
        // / M: add for ipmessage
        mSelectedIpMessageIds.clear();
        return true;
    }

    @Override
    public boolean onAddSelectedIpMessageId(boolean checkedState, long msgId, long ipMessageId) {
        /// M: add for ipmessage
        if (checkedState && ipMessageId != 0) {
            mSelectedIpMessageIds.put(msgId, ipMessageId);
        }
        return true;
    }

    @Override
    public boolean onIpDeleteThread(Collection<Long> threads, int maxSmsId) {
        Log.d(TAG, "onIpDeleteThread, threads = " + threads);
        Iterator<Long> iter = threads.iterator();
        while (iter.hasNext()) {
            long threadId = iter.next();
            RCSMessageManager.getInstance(mContext).deleteThreadFromMulti(threadId, maxSmsId);
        }
        return true;
    }


    //favorite
    @Override
    public boolean onCreateIpActionMode(final ActionMode mode, Menu menu) {
      /// M: add for ipmessage menu
        //if (RCSServiceManager.getInstance().serviceIsReady()) {
            MenuItem item = menu
                    .add(0, MENU_FAVORITE, 0, mPluginContext.getString(R.string.menu_favorite))
                    .setTitle(mPluginContext.getString(R.string.menu_favorite));
            item.setVisible(true);
        //}
        return true;
    }

    @Override
    public boolean onPrepareIpActionMode(ActionMode mode, Menu menu, int selectNum, int ForwardMenuId) {
        mForwardMenuId = ForwardMenuId;
        MenuItem favoriteItem = menu.findItem(MENU_FAVORITE);
        if (favoriteItem == null) {
            return true;
        }
        if (RCSServiceManager.getInstance().serviceIsReady()) {
            if (selectNum > 0) {
                favoriteItem.setVisible(true);
            } else {
                favoriteItem.setVisible(false);
            }
        } else {
            //favoriteItem.setVisible(false);
            //for IT
            if (selectNum > 0) {
                favoriteItem.setVisible(true);
            } else {
                favoriteItem.setVisible(false);
            }
        }
        return true;
    }
    
    @Override
    public boolean onIpActionItemClicked(ActionMode mode, MenuItem item, long[][] selectedIds,
            String[] contacts, Cursor cursor) {
        if (item.getItemId() == MENU_FAVORITE || item.getItemId() == mForwardMenuId) {
            Log.d(TAG, "thiss onIpActionItemClicked");
            HashSet<Long> mSmsIds = null;
            HashSet<Long> mMmsIds = null;
            boolean mHasMms = false;
            boolean mHasSms = false;
            boolean mHasUnDownloadMsg = false;
            if (selectedIds[0] != null && selectedIds[0].length > 0) {
                mSmsIds = collectIds(selectedIds[0]);
                if (mSmsItem == null) {
                    mSmsItem = new HashMap<Long, smsBodyandAddress>();
                } else {
                    mSmsItem.clear();
                }
                if (mSmsIds.size() > 0) {
                	mHasSms = true;
                }
            }
            if (selectedIds[1] != null && selectedIds[1].length > 0) {
                mMmsIds = collectIds(selectedIds[1]);
                if (mMmsItem == null) {
                    mMmsItem = new HashMap<Long, mmsSubjectandType>();
                } else {
                    mMmsItem.clear();
                }
                if (mMmsIds.size() > 0) {
                	mHasMms = true;
                }
            }

            if (item.getItemId() == MENU_FAVORITE) {
                Log.d(TAG, "thiss onIpActionItemClicked MENU_FAVORITE");
                if (cursor != null && cursor.getCount() > 0) {
                    try {
                        if (cursor.moveToFirst()) {
                            do {
                                String type = cursor.getString(0);
                                long msgId = cursor.getLong(1);
                                if (type.equals("mms")) {
                                    if (mHasMms && mMmsIds.contains(msgId)) {
                                        int mBoxId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_MESSAGE_BOX));
                                        int messageType = cursor.getInt(cursor.getColumnIndexOrThrow("m_type"));
                                        
                                        if (mBoxId == Mms.MESSAGE_BOX_INBOX && messageType == MESSAGE_TYPE_NOTIFICATION_IND) {
                                            mHasUnDownloadMsg  = true;
                                            continue;
                                        }
                                        String mSub = cursor.getString(cursor.getColumnIndexOrThrow("sub"));
                                        int mSub_cs = 0;
                                        if (!TextUtils.isEmpty(mSub)) {
                                            mSub_cs = cursor.getInt(cursor.getColumnIndexOrThrow("sub_cs"));
                                        }
                                        mmsSubjectandType st = new mmsSubjectandType(mBoxId, messageType, mSub, mSub_cs);
                                        mMmsItem.put(msgId, st);
                                    }
                                } else {
                                    if (mHasSms && mSmsIds.contains(msgId)) {
                                        long ipMsgid = cursor.getLong(cursor.getColumnIndexOrThrow("ipmsg_id"));
                                        if (ipMsgid < 0) {
                                            IpMessage ipMessage = RCSMessageManager.getInstance(mContext)
                                            .getIpMsgInfo(mThreadId, ipMsgid);
                                            if ((ipMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                                                            ((IpAttachMessage)ipMessage).getRcsStatus() != Status.FINISHED)) {
                                                mHasUnDownloadMsg  = true;
                                                continue;
                                            }
                                        }
                                        String smsBody    = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                                        String smsAddress = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                                        int boxId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_MESSAGE_BOX));
                                        smsBodyandAddress  ba = new smsBodyandAddress(smsAddress, smsBody, ipMsgid, boxId);
                                        mSmsItem.put(msgId, ba);
                                    }
                                }
                            } while (cursor.moveToNext());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    final String mAddress = getStringContact(contacts);
                    final boolean hasMms = mHasMms;
                    final boolean hasSms = mHasSms;
                    if (mHasUnDownloadMsg) {
                        showMmsTipsDialog(mAddress, hasMms, hasSms);
                    } else {
                        startFavorite(mAddress, hasMms, hasSms);
                    }
                }
                return true;
            } else if (item.getItemId() == mForwardMenuId) {
                Log.d(TAG, "thiss onIpActionItemClicked mForwardMenuId");
                //forward single ft
                int ftCount = getFtCount();
                if (!mHasMms && mSmsIds.size() == 1 && ftCount == 1) {
                    Iterator<Long> it = mSmsIds.iterator();
                    long ftId = 0;
                    while (it.hasNext()) {
                        ftId = it.next();
                    }
                    long ipMsgId = mSelectedIpMessageIds.get(ftId);
                    IpMessage ftMessage = RCSMessageManager.getInstance(mContext)
                    .getIpMsgInfo(mThreadId, ipMsgId);
                    IpAttachMessage attMessage = (IpAttachMessage) ftMessage;
                    if (ftMessage.getStatus() == Sms.MESSAGE_TYPE_INBOX &&
                            attMessage.getRcsStatus() != Status.FINISHED){//undownload
                        //undownloadFtmsg  add toast
                        showFtDialog();
                    } else {
                        if (RCSServiceManager.getInstance().serviceIsReady()) {
                            Intent forwardintent = RcsMessageUtils.createForwordIntentFromIpmessage(mContext, attMessage);
                            mContext.startActivity(forwardintent);
                        } else {
                            Toast.makeText(
                                mPluginContext,
                                R.string.toast_sms_unable_forward,
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true;
                }
                
                //forward single mms and text
                if (mHasMms && ftCount <= 0) {
                    return false;
                }
                
                //has FT or have (FT and MMS)
                if (ftCount > 0) {
                    //showdialog and call back
                    showMmsFtDialog();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean needShowNoTextToast(int mSize) {
        int ftCount = getFtCount();
        if (ftCount == mSize) {
            return true;
        }
        return false;
    }
    
    public boolean needShowReachLimit() {
    	if (RcsMessageUtils.getConfigStatus()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private int getFtCount() {
        int ftCount = 0;
        if (mSelectedIpMessageIds.size() > 0) {
            Iterator<Entry<Long, Long>> iter = mSelectedIpMessageIds.entrySet().iterator();
            while (iter.hasNext()) {
                 Map.Entry<Long, Long> entry = iter.next();
                 if (entry.getValue() < 0) {
                     ftCount += 1;
                 }
            }
        }
        return ftCount;
    }
    
//    public boolean onIpPrepareToForwardMessage(ArrayList<Long> mSelectSms, ArrayList<Long> mSelectMms) {
//        return true;
//    }
    
    private void showMmsFtDialog() {
        Log.d(TAG, "thiss showMmsFtDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mPluginContext.getString(R.string.discard_mmsft_title))
               .setIconAttribute(android.R.attr.alertDialogIcon)
               .setCancelable(true)
               .setMessage(mPluginContext.getString(R.string.discard_mmsft_content))
               .setPositiveButton(mPluginContext.getString(R.string.dialog_continue), new DialogInterface.OnClickListener() {
                   public final void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       mCallback.onForwardActionItemClick();
                   }
               })
               .setNegativeButton(mPluginContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       if (mSmsItem != null) {
                           mSmsItem.clear();
                       }
                       if (mMmsItem != null) {
                           mMmsItem.clear();
                       }
                   }
               })//Cancel need to clear hashmap cache
               .show();
    }
    
    private void showFtDialog() {
        Log.d(TAG, "thiss showFtDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mPluginContext.getString(R.string.forward_tips_title))
               .setIconAttribute(android.R.attr.alertDialogIcon)
               .setCancelable(true)
               .setMessage(mPluginContext.getString(R.string.forward_tips_body))
               .setPositiveButton(mPluginContext.getString(R.string.dialog_continue), new DialogInterface.OnClickListener() {
                   public final void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       Toast.makeText(
                           mPluginContext,
                           R.string.toast_sms_forward,
                           Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(mPluginContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       if (mSmsItem != null) {
                           mSmsItem.clear();
                       }
                       if (mMmsItem != null) {
                           mMmsItem.clear();
                       }
                   }
               })//Cancel need to clear hashmap cache
               .show();
    }
    
    
    
    private void startFavorite(final String addresses, final boolean favoriteMms, final boolean favoriteSms) {
        showProgressIndication();
        new Thread(new Runnable() {
            public void run() {
                if (favoriteSms) {
                    setSmsFavorite();//sms
                }
                if (favoriteMms) {
                    setMmsFavorite(addresses);//mms  need address
                }
                mContext.runOnUiThread(new Runnable() {
                    public void run() {
                        dismissProgressIndication();
                    }
                });
            }
        }).start();    
    }

    private void showProgressIndication() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(mPluginContext.getString(R.string.please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.show();
    }

    private void dismissProgressIndication() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        Toast.makeText(
                mPluginContext,
                R.string.toast_favorite_success,
                Toast.LENGTH_SHORT).show();
    }

    private Map<Long, smsBodyandAddress> mSmsItem;
    private Map<Long, mmsSubjectandType> mMmsItem;
    class smsBodyandAddress {
        int boxId;
        long ipmsgid;
        String mAddress;
        String mBody;
        public smsBodyandAddress(String mAddress, String mBody, long ipmsgid, int boxId) {
            super();
            this.ipmsgid = ipmsgid;
            this.mAddress = mAddress;
            this.mBody = mBody;
            this.boxId = boxId;
        }
    }

    class mmsSubjectandType {
        int boxId;
        int mMessageType;
        String mSubject;
        int sub_cs;
        public mmsSubjectandType(int boxId, int mMessageType, String mSubject, int sub_cs) {
            super();
            this.boxId = boxId;
            this.mMessageType = mMessageType;
            this.mSubject = mSubject;
            this.sub_cs = sub_cs;
        }
    }

    private String getStringId(final long Ids[]) {
        String idSelect = null;
        if (Ids != null && Ids.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            for (long id : Ids) {
                strBuf.append(id + ",");
            }
            String str = strBuf.toString();
            idSelect = str.substring(0, str.length() - 1);
        }
        return idSelect;
    }

    private String getStringContact(String Contacts[]) {
        String ContactsSelect = null;
        if (Contacts != null && Contacts.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            for (String contact : Contacts) {
                strBuf.append(contact + ",");
            }
            String str = strBuf.toString();
            ContactsSelect = str.substring(0, str.length() - 1);
        }
        return ContactsSelect;
    }

    private HashSet<Long> collectIds(long Ids[]) {
        HashSet<Long> idSet = new HashSet<Long>();
        for (long id : Ids) {
        	if (id > 0) {
        		idSet.add(id);
        	}
        }
        return idSet;
    }

    private boolean setTextIpmessageFavorite(long id, int ct, String mBody, String mAddress) {
        ContentValues values = new ContentValues();
        values.put("favoriteid", id);
        values.put("type", 3);//Ipmessage
        values.put("address", mAddress);
        values.put("body", mBody);
        values.put("ct", "text/plain");
        values.put("date", System.currentTimeMillis());
        if (mChatId != null) {
            values.put("chatid", mChatId);
        }
        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
        return true;
    }
    
    private boolean setAttachIpmessageFavorite(long id, int ct, String mPath, String mAddress) {
        ContentValues values = new ContentValues();
        values.put("favoriteid", id);
        values.put("type", 3);//Ipmessage
        values.put("address", mAddress);
        values.put("date", System.currentTimeMillis());
        if (mChatId != null) {
            values.put("chatid", mChatId);
        }
        if (mPath != null) {
            String mImFileName = mPluginContext.getDir("favorite", 0).getPath() + "/Ipmessage";
            File path = new File(mImFileName);
            Log.d(TAG, "thiss mImFileName =" + mImFileName);
            if (!path.exists()) {
                path.mkdirs();
            }
            String mFileName = mImFileName + "/" + getFileName(mPath);
            String mNewpath = RcsMessageUtils.getUniqueFileName(mFileName);
            Log.d(TAG, "thiss mNewpath =" + mNewpath);
            copyFile(mPath,mNewpath);
            values.put("path", mNewpath);
            String mimeType = RCSUtils.getFileType(getFileName(mPath));
            if (mimeType != null) {
            	values.put("ct", mimeType);
            }
        }
        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
        return true;
    }
    
//    private boolean setSmsFavorite(String selection) {
//        Cursor mSmsCursor = null;
//        if (selection == null) {
//            Log.d(TAG, "thiss setSmsFavorite selection = null");
//            return false;
//        }
//        selection = "_id IN " + selection;
//        mSmsCursor = mContext.getContentResolver().query(Sms.CONTENT_URI,
//            SMS_PROJECTION, selection, null, null);
//        if (mSmsCursor != null && mSmsCursor.getCount() > 0) {
//            try {
//                while (mSmsCursor.moveToNext()) {
//                    int ipMsgId = mSmsCursor.getInt(3);
//                    int id = mSmsCursor.getInt(0);
//                    String mAddress = mSmsCursor.getString(2);
//                    String mBody = mSmsCursor.getString(1);
//                    if (ipMsgId != 0) {
//                        IpMessage ipMessageForFavorite = RCSMessageManager.getInstance(mContext)
//                        .getIpMsgInfo(mThreadId, ipMsgId);
//                        if (ipMsgId < 0) {
//                            IpAttachMessage attachMessage = (IpAttachMessage) ipMessageForFavorite;
//                            setAttachIpmessageFavorite(id, attachMessage.getType(), attachMessage.getPath(), mAddress);
//                        } else {
//                            IpTextMessage textMessage = (IpTextMessage) ipMessageForFavorite;
//                            setTextIpmessageFavorite(id, textMessage.getType(), textMessage.getBody(), mAddress);
//                        }
//                    } else {
//                        ContentValues values = new ContentValues();
//                        values.put("favoriteid", id);
//                        values.put("type", 1);//sms
//                        values.put("address", mAddress);
//                        values.put("body", mBody);
//                        values.put("date", System.currentTimeMillis());
//                        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                return false;
//            } finally {
//                mSmsCursor.close();
//            }
//            return true;
//        }
//        return false;
//    }

//    private boolean setMmsFavorite(String selection, String[] Contacts) {
//        Cursor mMmsCursor = null;
//        byte[] pduMid;
//        if (selection == null) {
//            Log.d(TAG, "thiss setMmsFavorite selection = null");
//            return false;
//        }
//        String mAddress = getStringContact(Contacts);
//        selection = "_id IN " + selection;
//        mMmsCursor = mContext.getContentResolver().query(Mms.CONTENT_URI,
//            null, selection, null, null);
//        String FILE_NAME_PDU = mContext.getDir("favorite", 0).getPath() + "/pdu";
//        File path = new File(FILE_NAME_PDU);
//        Log.d(TAG, "thiss FILE_NAME_PDU =" + FILE_NAME_PDU);
//        Log.d(TAG, "thiss time1 =" + System.currentTimeMillis() + "; Address = " + mAddress);
//        if (!path.exists()) {
//            path.mkdirs();
//        }
//        if (mMmsCursor != null && mMmsCursor.getCount() > 0) {
//            Log.d(TAG, "thiss mMmsCursor.getCount() =" + mMmsCursor.getCount());
//            try {
//                while (mMmsCursor.moveToNext()) {
//                    int id = mMmsCursor.getInt(mMmsCursor.getColumnIndex(COLUMN_NAME_ID));
//                    Log.d(TAG, "thiss msgId =" + id);
//                    int mBoxId = mMmsCursor.getInt(mMmsCursor.getColumnIndex(COLUMN_NAME_MESSAGE_BOX));
//                    String mSubject = mMmsCursor.getString(mMmsCursor.getColumnIndex("sub"));
//                    if (!TextUtils.isEmpty(mSubject)) {
//                        int charset = mMmsCursor.getInt(mMmsCursor.getColumnIndex("sub_cs"));
//                        EncodedStringValue v = new EncodedStringValue(charset,
//                                PduPersister.getBytes(mSubject));
//                        mSubject = v.getString();
//                    }
//                    
//                    Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, id);
//                    Log.d(TAG, "thiss realUri =" + realUri);
//                    PduPersister p = PduPersister.getPduPersister(mContext);
//                    Log.d(TAG, "thiss mBoxId =" + mBoxId);
//                    if (mBoxId == Mms.MESSAGE_BOX_INBOX) {
//                        int type = mMmsCursor.getInt(mMmsCursor.getColumnIndex(COLUMN_NAME_TYPE));
//                        if (type == MESSAGE_TYPE_NOTIFICATION_IND) {
////                            NotificationInd nPdu = (NotificationInd) p.load(realUri);
////                            pduMid = new PduComposer(mContext, nPdu).make(true);
//                            pduMid = null;
//                        } else if (type == MESSAGE_TYPE_RETRIEVE_CONF) {
//                            RetrieveConf rPdu = (RetrieveConf) p.load(realUri, true);
//                            pduMid = new PduComposer(mContext, rPdu).make(true);
//                        } else {
//                            pduMid = null;
//                        }
//                    } else {
//                        SendReq sPdu = (SendReq) p.load(realUri);
//                        pduMid = new PduComposer(mContext, sPdu).make();
//                        Log.d(TAG, "thiss SendReq pduMid =" + pduMid);
//                    }
//                    String mFile = FILE_NAME_PDU + "/" + System.currentTimeMillis() + FILE_EXT_PDU;
//                    if (pduMid != null) {
//                        byte[] pduByteArray = pduMid;
//                        Log.d(TAG, "thiss fileName =" + mFile);
//                        writeToFile(mFile, pduByteArray);
//                    }
//                    if (pduMid != null) {
//                        ContentValues values = new ContentValues();
//                        values.put("favoriteid", id);
//                        values.put("path", mFile);
//                        values.put("date", System.currentTimeMillis());
//                        values.put("type", 2);//mms
//                        values.put("address", mAddress);
//                        if (mSubject != null) {
//                            values.put("body", mSubject);
//                        }
//                        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                Log.d(TAG, "thiss time2 =" + System.currentTimeMillis());
//                mMmsCursor.close();
//            }
//        }
//        return true;
//    }
    
    private String getFileName(String mFile) {
        return mFile.substring(mFile.lastIndexOf("/") + 1);
    }

    private void writeToFile(String fileName, byte[] buf) {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            // byte[] buf = inBuf.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(String oldPath, String newPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) {
                inStream = new FileInputStream(oldPath);
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[3000];
                while ( (byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
            if (null != fs) {
                try {
                    fs.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
        }
    }

    private void showMmsTipsDialog(final String address, final boolean isHasMms, final boolean isHasSms) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mPluginContext.getString(R.string.forward_tips_title))
               .setIconAttribute(android.R.attr.alertDialogIcon)
               .setCancelable(true)
               .setPositiveButton(mPluginContext.getString(R.string.dialog_continue), new DialogInterface.OnClickListener() {
                   public final void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       startFavorite(address, isHasMms, isHasSms);
                   }
               })
               .setNegativeButton(mPluginContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       if (mSmsItem != null) {
                           mSmsItem.clear();
                       }
                       if (mMmsItem != null) {
                           mMmsItem.clear();
                       }
                   }
               })//Cancel need to clear hashmap cache
               .setMessage(mPluginContext.getString(R.string.forward_tips_body))
               .show();
    }

    private boolean setSmsFavorite() {
        if (mSmsItem != null && mSmsItem.size() > 0) {
            Iterator<Entry<Long, smsBodyandAddress>> iter = mSmsItem.entrySet().iterator();
            while (iter.hasNext()) {
                 Map.Entry<Long, smsBodyandAddress> entry = iter.next();
                 long ipMsgid = entry.getValue().ipmsgid;
                 long id = entry.getKey();
                 String mAddress = null;
                 if (entry.getValue().boxId == 1) {
                     mAddress = entry.getValue().mAddress;
                 }
                 String mBody = entry.getValue().mBody;
                 if (ipMsgid > 0) {
                     setTextIpmessageFavorite(id, 10, mBody, mAddress);//10 is defined by self
                 } else if (ipMsgid < 0) {
                     IpMessage ipMessageForFavorite = RCSMessageManager.getInstance(mContext)
                     .getIpMsgInfo(mThreadId, ipMsgid);
                     IpAttachMessage attachMessage = (IpAttachMessage) ipMessageForFavorite;
                     setAttachIpmessageFavorite(id, attachMessage.getType(), attachMessage.getPath(), mAddress);
                 } else {
                     ContentValues values = new ContentValues();
                     values.put("favoriteid", id);
                     values.put("type", 1);//sms
                     values.put("address", mAddress);
                     values.put("body", mBody);
                     values.put("date", System.currentTimeMillis());
                     mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
                 }
            }
            mSmsItem.clear();
        }
        return true;
    }
    
    private boolean setMmsFavorite(final String Contacts) {
        byte[] pduMid;

        String mPduFileName = mPluginContext.getDir("favorite", 0).getPath() + "/pdu";
        File path = new File(mPduFileName);
        Log.d(TAG, "thiss mPduFileName =" + mPduFileName);
        Log.d(TAG, "thiss time1 =" + System.currentTimeMillis() + "; Contacts = " + Contacts);
        if (!path.exists()) {
            path.mkdirs();
        }
        
        if (mMmsItem != null && mMmsItem.size() > 0) {
            Iterator<Entry<Long, mmsSubjectandType>> iter = mMmsItem.entrySet().iterator();
            try {
                while (iter.hasNext()) {
                    Map.Entry<Long, mmsSubjectandType> entry = iter.next();
                    long id = entry.getKey();
                    int mBoxId = entry.getValue().boxId;
                    int type = entry.getValue().mMessageType;
                    String mSubject = entry.getValue().mSubject;
                    
                    if (!TextUtils.isEmpty(mSubject)) {
                        int charset = entry.getValue().sub_cs;
                        EncodedStringValue v = new EncodedStringValue(charset,
                                PduPersister.getBytes(mSubject));
                        mSubject = v.getString();
                    }
                    
                    Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, id);
                    Log.d(TAG, "thiss realUri =" + realUri);
                    PduPersister p = PduPersister.getPduPersister(mContext);
                    Log.d(TAG, "thiss mBoxId =" + mBoxId);
                    if (mBoxId == Mms.MESSAGE_BOX_INBOX) {
                        if (type == MESSAGE_TYPE_NOTIFICATION_IND) {
//                            NotificationInd nPdu = (NotificationInd) p.load(realUri);
//                            pduMid = new PduComposer(mContext, nPdu).make(true);
                            pduMid = null;
                        } else if (type == MESSAGE_TYPE_RETRIEVE_CONF) {
                            RetrieveConf rPdu = (RetrieveConf) p.load(realUri, true);
                            pduMid = new PduComposer(mContext, rPdu).make(true);
                        } else {
                            pduMid = null;
                        }
                    } else {
                        SendReq sPdu = (SendReq) p.load(realUri);
                        pduMid = new PduComposer(mContext, sPdu).make();
                        Log.d(TAG, "thiss SendReq pduMid =" + pduMid);
                    }
                    String mFile = mPduFileName + "/" + System.currentTimeMillis() + FILE_EXT_PDU;
                    if (pduMid != null) {
                        byte[] pduByteArray = pduMid;
                        Log.d(TAG, "thiss fileName =" + mFile);
                        writeToFile(mFile, pduByteArray);
                    }
                    if (pduMid != null) {
                        ContentValues values = new ContentValues();
                        values.put("favoriteid", id);
                        values.put("path", mFile);
                        values.put("date", System.currentTimeMillis());
                        values.put("type", 2);//mms
                        if (mBoxId == Mms.MESSAGE_BOX_INBOX) {
                            values.put("address", Contacts);
                        }
                        if (mSubject != null) {
                            values.put("body", mSubject);
                        }
                        mContext.getContentResolver().insert(FavoriteMsgProvider.CONTENT_URI, values);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } 
        }
        return true;
    }

    public boolean ipForwardOneMms(Uri mUri) {
        if (!RcsMessageUtils.getConfigStatus()) {
            return false;
        }
        Intent mIntent = RcsMessageUtils.createForwardIntentFromMms(mContext, mUri);
        if (mIntent != null) {
            mContext.startActivity(mIntent);
            mContext.finish();
            return true;
        }
        return false;
    }

    public boolean forwardTextMessage(String mBody) {
    	if (!RcsMessageUtils.getConfigStatus()) {
            return false;
        }
    	Intent mIntent = RcsMessageUtils.createForwordIntentFromSms(mContext, mBody);
        if (mIntent != null) {
            mContext.startActivity(mIntent);
            mContext.finish();
            return true;
        }
        return false;
    }
}
