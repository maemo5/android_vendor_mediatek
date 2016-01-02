package com.mediatek.rcs.message.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mtkex.chips.MTKRecipientEditTextView;
import com.android.mtkex.chips.RecipientEntry;

import com.mediatek.internal.telephony.CellConnMgr;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.aidl.IMessageSender;
import com.mediatek.rcs.message.data.Contact;
import com.mediatek.rcs.message.data.ContactList;
import com.mediatek.rcs.message.data.ForwardSendData;
import com.mediatek.rcs.message.proxy.RcsProxyManager;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Set;
import com.mediatek.rcs.common.utils.RCSUtils;
import android.media.MediaFile;

import com.mediatek.rcs.message.plugin.RCSVCardAttachment;

import com.mediatek.rcs.common.utils.ContextCacher;


public class ForwardActivity extends ListActivity {
    private RecentChatAdapter mRecentChatAdapter;
    private Cursor mRecentChatCursor;
    private Context mContext;
    private RecipientsEditor mRecipientsEditor;
    private ImageButton mRecipientsPicker;
    private static final int RECIPIENTS_LIMIT_FOR_SMS = getSmsRecipientLimit();
    private static final int MAX_SEND_NUM = 10;
    private boolean mIsRecipientHasIntentNotHandle = false;
    private Intent mIntent = null;
    private boolean mIsPickContatct = false;
    private static final String TAG = "Rcs/ForwardActivity";
    private static final boolean DBG = true;
    //private IMmsUtilsExt mMmsUtils = null;
    //private IMmsTextSizeAdjustExt mMmsTextSizeAdjustPlugin = null;
    //private IStringReplacementExt mStringReplacementPlugin;
    private ProgressDialog mContactPickDialog;

    private static final int UPDATE_LIMIT_LANDSCAPE_TABLET = 30;
    private static final int UPDATE_LIMIT_PORTRAIT_TABLET = 30;
    private static final int UPDATE_LIMIT_LANDSCAPE = 20;
    private static final int UPDATE_LIMIT_PORTRAIT = 20;


    public static final int REQUEST_CODE_ATTACH_IMAGE     = 100;
    public static final int REQUEST_CODE_TAKE_PICTURE     = 101;
    public static final int REQUEST_CODE_ATTACH_VIDEO     = 102;
    public static final int REQUEST_CODE_TAKE_VIDEO       = 103;
    public static final int REQUEST_CODE_ATTACH_SOUND     = 104;
    public static final int REQUEST_CODE_RECORD_SOUND     = 105;
    public static final int REQUEST_CODE_CREATE_SLIDESHOW = 106;
    public static final int REQUEST_CODE_ECM_EXIT_DIALOG  = 107;
    public static final int REQUEST_CODE_ADD_CONTACT      = 108;
    public static final int REQUEST_CODE_PICK             = 109;

    public static final int REQUEST_CODE_IPMSG_PICK_CONTACT  = 210;
    private static final int MSG_DISMISS_CONTACT_PICK_DIALOG = 9009;
    //private WorkingMessage mWorkingMessage;         // The message currently being composed.

    private static final String USING_COLON = "USE_COLON";
    private static final String NUMBERS_SEPARATOR_COLON = ":";
    private static final String NUMBERS_SEPARATOR_SIMCOLON = ";";
    private static final String NUMBERS_SEPARATOR_COMMA = ",";
    private static final String ACTION_RCS_MESSAGING_SEND = "android.intent.action.ACTION_RCS_MESSAGING_SEND";
    private ForwardSendAsyncTask mForwardSendAsyncTask;

    //public MessageListAdapter mMsgListAdapter = null;
    private int mLastRecipientCount = 0;
    private InputMethodManager mInputMethodManager = null;
    private ForwardData mForwardData;
    private PortraitService mPortraitService;

    /**
     * Describe <code>onCreate</code> method here.
     *
     * @param bundle a <code>Bundle</code> value
     */
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.forward_send_activity);

        initRecipientsEditor(savedInstanceState);


        mForwardSendAsyncTask = (ForwardSendAsyncTask)getLastNonConfigurationInstance();
        if (mForwardSendAsyncTask != null) {
            mForwardSendAsyncTask.attach(this);
        }
        // todo: should place a another place, discuss with chuangjie, remove to other
        // Contact.init(getApplicationContext());

        parseIntent(getIntent());

        mPortraitService = PortraitService.getInstance(this.getApplicationContext(),
                R.drawable.ic_contact_picture,
                R.drawable.contact_blank_avatar);
        mPortraitUpdateListener = new PortraitUpdateListener();
        mPortraitService.addListener(mPortraitUpdateListener);
        //mRecentChatAdapter = new RecentChatAdapter(this);
        //this.setListAdapter(mRecentChatAdapter);

        Uri uri = Uri.parse("content://mms-sms/conversations/extend").buildUpon()
            .appendQueryParameter("simple", "true").build();
        mRecentChatCursor = getContentResolver().query(uri,
                                                       ALL_THREADS_PROJECTION_EXTEND,
                                                       Telephony.Threads.STATUS + " >= ?",
                                                       new String[]{"0"},
                                                       null,
                                                       null);
        if (mRecentChatCursor != null) {
            mRecentChatAdapter = new RecentChatAdapter(this, mRecentChatCursor);
            this.setListAdapter(mRecentChatAdapter);            
        }
    }

    /**
     * Describe <code>onResume</code> method here.
     *
     */
    public final void onResume() {
        mShowingContactPicker = false;
        super.onResume();

        if (mForwardSendAsyncTask != null) {
            if (mForwardSendAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                showProgressDialog();
            }

            Log.d(TAG, "onResume() mForwardSendAsyncTask.getStates()" + mForwardSendAsyncTask.getStatus());
        } else {
            Log.d(TAG, "onResume() mForwardSendAsyncTask is null");
        }
    }

    /**
     * Describe <code>onRetainNonConfigurationInstance</code> method here.
     *
     * @return an <code>Object</code> value
     */
    public final Object onRetainNonConfigurationInstance() {
        if (mForwardSendAsyncTask != null) {
            mForwardSendAsyncTask.detach();
        }

        return mForwardSendAsyncTask;
    }

    /**
     * Describe <code>onDestroy</code> method here.
     *
     */
    public final void onDestroy() {
        // todo: dismiss diallog mode
        dismissProgressDialog();
        if (mRecentChatCursor != null) {
            mRecentChatCursor.close();
        }
        if (mPortraitService != null && mPortraitUpdateListener != null) {
            mPortraitService.removeListener(mPortraitUpdateListener);
        }
        super.onDestroy();
    }


    MenuItem mSendMenu;
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rcs_forward_send_list_menu, menu);
        mSendMenu = menu.findItem(R.id.rcs_action_send);
        return true;
    }

    /**
     * Describe <code>onOptionsItemSelected</code> method here.
     *
     * @param menuItem a <code>MenuItem</code> value
     * @return a <code>boolean</code> value
     */
    public final boolean onOptionsItemSelected(final MenuItem menuItem) {
        Log.d(TAG, "onOptionsItemSelected() id:" + menuItem.getItemId());
        switch (menuItem.getItemId()) {
        case android.R.id.home:
            this.finish();
            return true;
        case R.id.rcs_action_send:
            if (mRecipientsEditor.getRecipientCount() > 0) {
                //ForwardSendAsyncTask forwardSendAsyncTask = new ForwardSendAsyncTask(this);
                //forwardSendAsyncTask.execute();
                //mForwardData.setMTo(mRecipientsEditor.getNumbers());
                List<String> numbers = mRecipientsEditor.getNumbers();
                StringBuilder builder = new StringBuilder();
                builder.append(numbers.get(0));
                for (int i = 1; i < numbers.size(); i++) {
                    builder.append(",");
                    builder.append(numbers.get(i));
                }
                mForwardData.setMTo(builder.toString());
                checkConditionsAndSendMessage(true);
            }
            return true;

        default:
            return super.onOptionsItemSelected(menuItem);
        }
    }


    private class ForwardData {
        private long mThreadId = 0;
        private String mTo;
        private List<DataTypeContent> mData = new ArrayList<DataTypeContent>();
        public ForwardData() {
        }

        /**
         * Gets the value of mThreadId
         *
         * @return the value of mThreadId
         */
        public final long getMThreadId() {
            return this.mThreadId;
        }

        /**
         * Sets the value of mThreadId
         *
         * @param argMThreadId Value to assign to this.mThreadId
         */
        public final void setMThreadId(final long argMThreadId) {
            this.mThreadId = argMThreadId;
        }

        /**
         * Gets the value of mTo
         *
         * @return the value of mTo
         */
        public final String getMTo() {
            return this.mTo;
        }

        /**
         * Sets the value of mTo
         *
         * @param argMTo Value to assign to this.mTo
         */
        public final void setMTo(final String argMTo) {
            this.mTo = argMTo;
        }

        public void addData(String mineType, String content) {
            mData.add(new DataTypeContent(mineType, content));
        }

        public String getDataMineType(int i) {
            if (i < mData.size()) {
                return mData.get(i).getMineType();
            }

            return null;
        }

        public String getDataContent(int i) {
            if (i < mData.size()) {
                return mData.get(i).getContent();
            }

            return null;
        }

        public int getDataSize() {
            return mData.size();
        }

        private class DataTypeContent {
            private String mMineType;
            private String mContent;
            public  DataTypeContent(String mineType, String content) {
                mMineType = mineType;
                mContent = content;
            }


            public String getMineType() {
                return mMineType;
            }

            public void setMineType(String mineType) {
                mMineType = mineType;
            }

            public String  getContent() {
                return mContent;
            }

            public void setContent(String content) {
                mContent = content;
            }
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("To:" + mTo + "\n" + "threadId:" + mThreadId + ",mData.size():" + mData.size() + "\n");
            for (DataTypeContent data : mData) {
                builder.append("mineType:" +  data.getMineType() + ",content:" + data.getContent() + "\n");
            }

            return builder.toString();
        }
    }
    private void initRecipientsEditor(Bundle bundle) {
        View view = findViewById(R.id.rcs_forward_recipients_editor_stub);
        mRecipientsEditor = (RecipientsEditor) view.findViewById(R.id.recipients_editor);
        mRecipientsPicker = (ImageButton) view.findViewById(R.id.recipients_picker);

        //mRecipientsPicker.setOnClickListener(this);
        // porting roll back
        mRecipientsEditor.removeChipChangedListener(mChipWatcher);
        mRecipientsEditor.addChipChangedListener(mChipWatcher);

        mRecipientsEditor.setEnabled(true);
        mRecipientsEditor.setFocusableInTouchMode(true);
        mRecipientsEditor.setIsTouchable(true);
        mRecipientsPicker.setVisibility(View.VISIBLE);
        mRecipientsPicker.setEnabled(true);
        mRecipientsPicker.setOnClickListener(new RecipientsPickerClickListener());

        ChipsRecipientAdapter chipsAdapter = new ChipsRecipientAdapter(this);
        chipsAdapter.setShowEmailAddress(false);
        mRecipientsEditor.setAdapter(chipsAdapter);

        ContactList recipients = getRecipients();
        if (bundle == null) {
            mRecipientsEditor.populate(new ContactList());
            mRecipientsEditor.populate(recipients);
        }

        // mRecipientsEditor.setOnSelectChipRunnable(new Runnable() {
        //         public void run() {
        //             // After the user selects an item in the pop-up contacts list, move the
        //             // focus to the text editor if there is only one recipient.  This helps
        //             // the common case of selecting one recipient and then typing a message,
        //             // but avoids annoying a user who is trying to add five recipients and
        //             // keeps having focus stolen away.
        //             if (mRecipientsEditor.getRecipientCount() == 1) {
        //                 // if we're in extract mode then don't request focus
        //                 final InputMethodManager inputManager = mInputMethodManager;
        //                 if (inputManager == null || !inputManager.isFullscreenMode()) {
        //                     if (mBottomPanel != null && mBottomPanel.getVisibility() == View.VISIBLE) {
        //                         mTextEditor.requestFocus();
        //                     }
        //                 }
        //             }
        //         }
        //     });


        mRecipientsEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        /// M: add for ip message
                        Log.d(TAG, "onFocusChange(): mRecipientsEditor get focus.");
                        // showSharePanel(false);
                        // if (mIsLandscape) {
                        //     mTextEditor.setMaxHeight(
                        //                              mReferencedTextEditorTwoLinesHeight *
                        //                              mCurrentMaxHeight / mReferencedMaxHeight);
                        // } else {
                        //     updateTextEditorHeightInFullScreen();
                        // }
                    }
                    /// M: Fix ipmessage bug @{
                    //  updateCurrentChatMode(null) ;
                    /// @}
                }
            });
    }


    private static ContactList sEmptyContactList;
    private ContactList getRecipients() {
        // If the recipients editor is visible, the conversation has
        // not really officially 'started' yet.  Recipients will be set
        // on the conversation once it has been saved or sent.  In the
        // meantime, let anyone who needs the recipient list think it
        // is empty rather than giving them a stale one.

        if (sEmptyContactList == null) {
            sEmptyContactList = new ContactList();
        }
        return sEmptyContactList;
    }


    private void parseIntent(Intent intent) {
        boolean exceedMaxSendNum = false;
        mForwardData = new ForwardData();
        if (intent != null) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String mimeType = intent.getType();

            if (ACTION_RCS_MESSAGING_SEND.equals(action)) {
                try {
                    if (extras.containsKey(Intent.EXTRA_TEXT)) {
                        String extraText = extras.getString(Intent.EXTRA_TEXT);
                        if ((mimeType != null) && (extraText != null)) {
                            mForwardData.addData(mimeType, extraText);
                        }
                        Log.d(TAG, "parseIntent()mimeType:" + mimeType + ",extraText:" + extraText);
                    } else if (extras.containsKey(Intent.EXTRA_STREAM)){
                        String extraStream = intent.getStringExtra(Intent.EXTRA_STREAM);
                        if ((mimeType != null) && (extraStream != null)) {
                            mForwardData.addData(mimeType, extraStream);
                        }
                        Log.d(TAG, "parseIntent()mimeType:" + mimeType + ",extraStream:" + extraStream);
                    }

                    ArrayList<ForwardSendData> dataList = intent
                        .getParcelableArrayListExtra("android.intent.RCS_MULTI_SEND");
                    if (dataList != null) {
                        int size = dataList.size();
                        if (size > MAX_SEND_NUM) {
                            exceedMaxSendNum = true;
                            size = MAX_SEND_NUM;
                        }
                        for (int i = 0; i < size; i++) {
                            ForwardSendData data = dataList.get(i);
                            Log.d(TAG, "dataList " + i + ":mimeType:" + data.getMineType()
                                  + ",content:" + data.getContent());
                            mForwardData.addData(data.getMineType(), data.getContent());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (Intent.ACTION_SEND.equals(action)) {
                if (extras.containsKey(Intent.EXTRA_STREAM)) {
                    final Uri uri = (Uri)extras.getParcelable(Intent.EXTRA_STREAM);
                    if (uri == null) {
                        Log.i(TAG, "parseIntent() uri==null");
                        return;
                    }

                    Log.d(TAG, "parseIntent() mimeType:" + mimeType + ",uri:" + uri);
                    mForwardData.addData(mimeType, uri.toString());
                } else if (extras.containsKey(Intent.EXTRA_TEXT)) {
                    String extraText = extras.getString(Intent.EXTRA_TEXT);
                    Log.d(TAG, "parseIntent() mimeType:" + mimeType + ",extraText:" + extraText);
                    mForwardData.addData(mimeType, extraText);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                if (extras.containsKey(Intent.EXTRA_STREAM)) {
                    final ArrayList<Parcelable> uris = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
                    int importCount = uris.size();
                    if (importCount > MAX_SEND_NUM) {
                        exceedMaxSendNum = true;
                        importCount = MAX_SEND_NUM;
                    }
                    for (int i = 0; i < importCount; i++) {
                        Uri uri = (Uri)uris.get(i);
                        Log.d(TAG, "parseIntent() i:" + i + ",mimeType:" + mimeType + ",uri:" + uri);
                        mForwardData.addData(mimeType, uri.toString());
                    }
                }
            }
        }

        if (exceedMaxSendNum) {
            showDialog(DIALOG_EXCEED_MAX_NUMBER_CONFIRM);
        }
    }


    // private boolean mPossiblePendingNotification;
    // private final MessageListAdapter.OnDataSetChangedListener
    // mDataSetChangedListener = new MessageListAdapter.OnDataSetChangedListener() {
    //         @Override
    //         public void onDataSetChanged(MessageListAdapter adapter) {
    //             mPossiblePendingNotification = true;
    //         }

    //         @Override
    //         public void onContentChanged(MessageListAdapter adapter) {
    //             /// M: @{
    //             if (mMsgListAdapter != null &&
    //                 mMsgListAdapter.getOnDataSetChangedListener() != null) {
    //                 Log.d(TAG, "OnDataSetChangedListener is not cleared");
    //                 /// M: add for ip message, unread divider
    //                 mShowUnreadDivider = false;
    //                 startMsgListQuery();
    //             } else {
    //                 Log.d(TAG, "OnDataSetChangedListener is cleared");
    //             }
    //             /// @}
    //         }
    //     };


    // private void checkPendingNotification() {
    //     if (mPossiblePendingNotification && hasWindowFocus()) {
    //         /// M: add for ip message, remove mark as read
    //         //            mConversation.markAsRead();
    //         mPossiblePendingNotification = false;
    //     }
    // }

    private int getLimitedContact() {
        //boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        return UPDATE_LIMIT_PORTRAIT;
    }

    private final MTKRecipientEditTextView.ChipWatcher mChipWatcher = new MTKRecipientEditTextView.ChipWatcher() {
            public void onChipChanged(ArrayList<RecipientEntry> allChips,
            ArrayList<String> changedChipAddresses, String lastString) {
                Log.i(TAG, "ChipWatcher onChipChanged begin.");
                ContactList LastContacts = mRecipientsEditor.getContactsFromChipWatcher();
                int updateLimit = getLimitedContact();
                mRecipientsEditor.parseRecipientsFromChipWatcher(allChips, changedChipAddresses, lastString, updateLimit);
                List<String> numbers = mRecipientsEditor.getNumbersFromChipWatcher();
                // google steps in textchange
                //mWorkingMessage.setWorkingRecipients(numbers);
                /// M: google JB.MR1 patch, group mms
                //boolean multiRecipients = numbers != null && numbers.size() > 1;
                //boolean isGroupMms = MmsPreferenceActivity.getIsGroupMmsEnabled(ComposeMessageActivity.this) && multiRecipients;
                //mMsgListAdapter.setIsGroupConversation(isGroupMms);
                //mWorkingMessage.setHasMultipleRecipients(multiRecipients, true);
                //mWorkingMessage.setHasEmail(mRecipientsEditor.containsEmailFromChipWatcher(), true);
                int recipientCount = numbers.size();
                checkForTooManyRecipients(recipientCount);
                // google steps end
                // ContactList contacts = mRecipientsEditor.getContactsFromChipWatcher();
                // if (!contacts.equals(LastContacts) || (changedChipAddresses != null && changedChipAddresses.size() > 0)) {
                //     updateTitle(contacts);
                // }
                // updateSendButtonState();
                updateTitle();
                Log.i(TAG, "ChipWatcher onChipChanged end.");
            }
        };

    private void updateTitle() {
        if (mSendMenu != null) {
            if (mRecipientsEditor.getRecipientCount() > 0) {
                mSendMenu.setIcon(getResources().getDrawable(R.drawable.ic_menu_forward));
            } else {
                mSendMenu.setIcon(getResources().getDrawable(R.drawable.ic_menu_forward_disable));
            }
        }
    }

    private void checkForTooManyRecipients(int recipientCount) {
        /// M: Code analyze 056,Now,the sms recipient limit is different from mms.
        /// We can set limit for sms or mms individually. @{
        final int recipientLimit = getSmsRecipientLimit();
        /// @}
        if (recipientLimit != Integer.MAX_VALUE) {

            //final int recipientCount = recipientCount();
            boolean tooMany = recipientCount > recipientLimit;

            if (recipientCount != mLastRecipientCount) {
                // Don't warn the user on every character they type when they're over the limit,
                // only when the actual # of recipients changes.
                mLastRecipientCount = recipientCount;
                if (tooMany) {
                    String tooManyMsg = getString(R.string.too_many_recipients, recipientCount,
                                                  recipientLimit);
                    Toast.makeText(ForwardActivity.this, tooManyMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private class RecipientsPickerClickListener implements View.OnClickListener {
        public void onClick(View v) {
            //if (v == mRecipientsPicker) {
                // if (mRecipientsEditor.getRecipientCount() >= RECIPIENTS_LIMIT_FOR_SMS) {
                //     Toast.makeText(ForwardActivity.this, R.string.cannot_add_recipient, Toast.LENGTH_SHORT).show();
                // } else {
                //     try {
                //         //Intent intent = new Intent(RemoteActivities.CONTACT);
                //         Intent intent = new Intent("android.intent.action.contacts.list.PICKMULTICONTACTS");
                //         intent.putExtra(RemoteActivities.KEY_REQUEST_CODE, REQUEST_CODE_IPMSG_PICK_CONTACT);
                //         intent.putExtra(RemoteActivities.KEY_TYPE, SelectContactType.ALL);
                //         IpMessageUtils.startRemoteActivityForResult(ForwardActivity.this, intent);
                //     } catch (ActivityNotFoundException e) {
                //         mIsPickContatct = false;
                //         Toast.makeText(ForwardActivity.this,
                //         ForwardActivity.this.getString(R.string.no_application_response),
                //         Toast.LENGTH_SHORT).show();
                //         Log.e(TAG, e.getMessage());
                //     }
                // }
                //}
            if (!mShowingContactPicker) {
                addContacts(mRecipientsEditor != null ?
                            (RECIPIENTS_LIMIT_FOR_SMS - mRecipientsEditor.getNumbers().size()) :
                            RECIPIENTS_LIMIT_FOR_SMS, REQUEST_CODE_PICK);
            }
        }
    }

    private boolean mShowingContactPicker = false;
    //private boolean mIsPickContatct = false;
    private static final String PICK_CONTACT_NUMBER_BALANCE = "NUMBER_BALANCE";

    private void addContacts(int pickCount, int requestCode) {
        /// M: @{
        /*Intent intent = new Intent("android.intent.action.CONTACTSMULTICHOICE");
          intent.setType(Phone.CONTENT_ITEM_TYPE);
          intent.putExtra("request_email", true);
          intent.putExtra("pick_count", pickCount);
          mIsPickContatct = true;
          startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);*/
        try {
            /// M: fix bug ALPS00444752, set true to disable to Show ContactPicker
            mShowingContactPicker = true;
            mIsPickContatct = true;
            Intent intent = new Intent(ACTION_CONTACT_SELECTION);
            intent.setType(Phone.CONTENT_TYPE);

            if (mRecipientsEditor.getRecipientCount() > 0) {
                List<String> numbers = mRecipientsEditor.getNumbers();
                String[] existNumbers = numbers.toArray(new String[numbers.size()]);
                intent.putExtra("ExistNumberArray", existNumbers);
            }
            intent.putExtra("Group", true);

            /// M: OP09 add For pick limit: set number balance for picking contacts; As a common function
            intent.putExtra(PICK_CONTACT_NUMBER_BALANCE, pickCount);
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            mShowingContactPicker = false;
            mIsPickContatct = false;
            Toast.makeText(this, this.getString(R.string.no_application_response), Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
        }
        /// @}
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onIpMsgActivityResult(): requestCode = " + requestCode + ", resultCode = " + resultCode
                 + ", data = " + data);
        if (resultCode != RESULT_OK) {
            Log.d(TAG, "bail due to resultCode=" + resultCode);
            return;
        }

        /// M: add for ip message
        switch (requestCode) {
        case REQUEST_CODE_PICK:
            if (data != null) {
                if (mRecipientsEditor != null) {
                    processPickResult(data);
                } else {
                    mIsRecipientHasIntentNotHandle = true;
                    mIntent = data;
                }
            }
            //mShowingContactPicker = false
            mIsPickContatct = false;
            break;

        case REQUEST_CODE_IPMSG_PICK_CONTACT:
            if (null != data) {
                processPickResult(data);
            } else {
                mIsRecipientHasIntentNotHandle = true;
                mIntent = data;
            }
            mIsPickContatct = false;
            break;

        default:
            break;
        }
    }

    private Runnable mContactPickRunnable = new Runnable() {
            public void run() {
                if (mContactPickDialog != null) {
                    mContactPickDialog.show();
                }
            }
        };

    boolean mIsDuplicate = false;
    private boolean mIsPopulatingRecipients = false;
    public static final String SELECTION_CONTACT_RESULT = "contactId";
    private void processPickResult(final Intent data) {
        final RecipientsEditor editor;
        editor = mRecipientsEditor;

        final long[] contactsId = data.getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
        String numbersSelectedFromRecent = getNumbersFromIntent(data);

        if (numbersSelectedFromRecent == null || numbersSelectedFromRecent.length() < 1) {
            /// M: add for ip message {@
            numbersSelectedFromRecent = data.getStringExtra(SELECTION_CONTACT_RESULT);
        }
        final String mSelectContactsNumbers = numbersSelectedFromRecent;
        /// @}
        if ((contactsId == null || contactsId.length <= 0) && TextUtils.isEmpty(mSelectContactsNumbers)) {
            return;
        }
        int recipientCount = editor.getRecipientCount();
        if (!TextUtils.isEmpty(mSelectContactsNumbers)) {
            recipientCount += mSelectContactsNumbers.split(";").length;
        } else {
            recipientCount += contactsId.length;
        }
        /// @}
        /// M: Code analyze 056,Now,the sms recipient limit is different from mms.
        /// We can set limit for sms or mms individually. @{
        final int recipientLimit = getSmsRecipientLimit();
        /// @}
        if (recipientLimit != Integer.MAX_VALUE && recipientCount > recipientLimit) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.pick_too_many_recipients)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(getString(R.string.too_many_recipients, recipientCount, recipientLimit))
                .setPositiveButton(android.R.string.ok, null)
                .create().show();
            return;
        }

        /// M: @{
        //        final Handler handler = new Handler();
        if (mContactPickDialog == null) {
            mContactPickDialog = new ProgressDialog(this);
            mContactPickDialog.setMessage(getText(R.string.adding_recipients));
            mContactPickDialog.setIndeterminate(true);
            mContactPickDialog.setCancelable(false);
        }

        // Only show the progress dialog if we can not finish off parsing the return data in 1s,
        // otherwise the dialog could flicker.
        mUiHandler.postDelayed(mContactPickRunnable, 500);


        new Thread(new Runnable() {
                public void run() {
                    final ContactList list = new ContactList();
                    final ContactList allList = new ContactList();
                    try {
                        /// M: @{
                        //list = ContactList.blockingGetByUris(uris);
                        /// M: add for ip message
                        /// M: To append recipients into RecipientsEditor, no need to load avatar,
                        /// because Editor will query and notify avatar info to MMS later. If append
                        /// 100 recipients, will saving almost 3s.
                        Contact.sNeedLoadAvatar = false;
                        ContactList selected = TextUtils.isEmpty(mSelectContactsNumbers) ?
                            ContactList.blockingGetByIds(contactsId) :
                            ContactList.getByNumbers(mSelectContactsNumbers, false, false);
                        Contact.sNeedLoadAvatar = true;
                        final List<String> numbers = editor.getNumbers();

                        /** M: better merge strategy.
                         * Avoid the use of mRecipientsEditor.contrcutionContactsFromInput()
                         * all Contacts in selected list should be added.
                         * */
                        /// M: remove duplicated numbers and format
                        List<String> selectedNumbers = Arrays.asList(selected.getProtosomaitcNumbers());
                        if (selectedNumbers.size() < selected.size()) {
                            mIsDuplicate = true;
                        }
                        String selectedNumberAfterFormat = "";
                        if (numbers.size() > 0) {
                            for (String number : numbers) {
                                if (!number.trim().equals("")) {
                                    Contact c = Contact.get(number, false);
                                    allList.add(c);
                                }
                            }
                            /// M: format existing numbers(remove "-" and " ")
                            List<String> formatedNumbers = Arrays.asList(allList.getNumbers(true));
                            for (String selectedNumber : selectedNumbers) {
                                selectedNumberAfterFormat = parseMmsAddress(selectedNumber);
                                if (selectedNumberAfterFormat != null && !selectedNumberAfterFormat.trim().equals("")) {
                                    if (!formatedNumbers.contains(selectedNumberAfterFormat)) {
                                        Contact c = Contact.get(selectedNumber, false);
                                        list.add(c);
                                    } else {
                                        mIsDuplicate = true;
                                    }
                                }
                            }
                            allList.addAll(list);
                        } else {
                            for (String selectedNumber : selectedNumbers) {
                                selectedNumberAfterFormat = parseMmsAddress(selectedNumber);
                                if (selectedNumberAfterFormat != null && !selectedNumber.trim().equals("")) {
                                    Contact c = Contact.get(selectedNumber, false);
                                    list.add(c);
                                }
                            }
                            allList.addAll(list);
                        }
                        /// @}
                    } finally {
                        Message msg = mUiHandler.obtainMessage();
                        msg.what = MSG_DISMISS_CONTACT_PICK_DIALOG;
                        mUiHandler.sendMessage(msg);
                    }
                    // TODO: there is already code to update the contact header widget and recipients
                    // editor if the contacts change. we can re-use that code.
                    final Runnable populateWorker = new Runnable() {
                            public void run() {
                                //mConversation.setRecipients(allList);
                                if (list.size() > 0) {
                                    // Fix ALPS01594370, if has attachment, recipient eidtor always on focus.
                                    // And adding a recipient bebind the editable recipient is not allowed.
                                    // if ((mBottomPanel == null) || (mBottomPanel.getVisibility() != View.VISIBLE)) {
                                    //     mRecipientsEditor.clearFocus();
                                    // }
                                    mIsPopulatingRecipients = true;
                                    mRecipientsEditor.populate(list);
                                    mRecipientsEditor.requestFocus();
                                }
                                if (mIsDuplicate) {
                                    Toast.makeText(ForwardActivity.this,
                                                   R.string.add_duplicate_recipients,
                                                   Toast.LENGTH_SHORT).show();
                                    mIsDuplicate = false;
                                }
                            }
                        };

                    mUiHandler.post(populateWorker);
                }
            }, "ComoseMessageActivity.processPickResult").start();
    }


    private Handler mUiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_DISMISS_CONTACT_PICK_DIALOG:
                    mUiHandler.removeCallbacks(mContactPickRunnable);
                    if (mContactPickDialog != null && mContactPickDialog.isShowing()) {
                        mContactPickDialog.dismiss();
                    }
                    mContactPickDialog = null;
                    break;

                default:
                    Log.d(TAG, "inUIHandler msg unhandled.");
                    break;
                }
            }
        };

    // ==================================================
    /// M: use this instead of the google default to query more columns in thread_settings
    public static final String[] ALL_THREADS_PROJECTION_EXTEND = {
        Threads._ID,
        Threads.DATE,
        Threads.MESSAGE_COUNT,
        Threads.RECIPIENT_IDS,
        Threads.SNIPPET,
        Threads.SNIPPET_CHARSET,
        Threads.READ,
        Threads.ERROR,
        Threads.HAS_ATTACHMENT,
        /// M:
        Threads.TYPE,
        Telephony.Threads.READ_COUNT,
        Telephony.Threads.STATUS,
        Telephony.ThreadSettings._ID, /// M: add for common
        Telephony.ThreadSettings.NOTIFICATION_ENABLE,
        Telephony.ThreadSettings.SPAM, Telephony.ThreadSettings.MUTE,
        Telephony.ThreadSettings.MUTE_START,
        Telephony.Threads.DATE_SENT
    };

    public class RecentChatMember {
        private String mPhoneNumber;
        private String mName;
        private String mChatId;
        private long mThreadId;
        private Bitmap mBitmap;

        RecentChatMember(String number, String name, String chatId, long threadId) {
            mPhoneNumber = number;
            mName = name;
            mChatId = chatId;
            mThreadId = threadId;
        }

        /**
         * Gets the value of mPhoneNumber
         *
         * @return the value of mPhoneNumber
         */
        public final String getMPhoneNumber() {
            return this.mPhoneNumber;
        }

        /**
         * Sets the value of mPhoneNumber
         *
         * @param argMPhoneNumber Value to assign to this.mPhoneNumber
         */
        public final void setMPhoneNumber(final String argMPhoneNumber) {
            this.mPhoneNumber = argMPhoneNumber;
        }

        /**
         * Gets the value of mName
         *
         * @return the value of mName
         */
        public final String getMName() {
            return this.mName;
        }

        /**
         * Sets the value of mName
         *
         * @param argMName Value to assign to this.mName
         */
        public final void setMName(final String argMName) {
            this.mName = argMName;
        }

        /**
         * Gets the value of mChatId
         *
         * @return the value of mChatId
         */
        public final String getMChatId() {
            return this.mChatId;
        }

        /**
         * Sets the value of mChatId
         *
         * @param argMChatId Value to assign to this.mChatId
         */
        public final void setMChatId(final String argMChatId) {
            this.mChatId = argMChatId;
        }

        /**
         * Gets the value of mThreadId
         *
         * @return the value of mThreadId
         */
        public final long getMThreadId() {
            return this.mThreadId;
        }

        /**
         * Sets the value of mThreadId
         *
         * @param argMThreadId Value to assign to this.mThreadId
         */
        public final void setMThreadId(final long argMThreadId) {
            this.mThreadId = argMThreadId;
        }
        
        /**
         * Gets the value of mBitmap
         *
         * @return the value of mBitmap
         */
        public final Bitmap getMBitmap() {
            return this.mBitmap;
        }

        /**
         * Sets the value of mBitmap
         *
         * @param argMBitmap Value to assign to this.mBitmap
         */
        public final void setMBitmap(final Bitmap argMBitmap) {
            this.mBitmap = argMBitmap;
        }
    }

    private class RecentChatAdapter extends CursorAdapter {
        private static final int ID             = 0;
        //private static final int DATE           = 1;
        //private static final int MESSAGE_COUNT  = 2;
        private static final int RECIPIENT_IDS  = 3;
        //private static final int SNIPPET        = 4;
        //private static final int SNIPPET_CS     = 5;
        //private static final int READ           = 6;
        //private static final int ERROR          = 7;
        //private static final int HAS_ATTACHMENT = 8;
        private static final int STATUS         = 11;

        public RecentChatAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        /**
         * Describe <code>newView</code> method here.
         *
         * @param context a <code>Context</code> value
         * @param cursor a <code>Cursor</code> value
         * @param viewGroup a <code>ViewGroup</code> value
         * @return a <code>View</code> value
         */
        public final View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.forward_send_item, parent, false);
        }

        /**
         * Describe <code>bindView</code> method here.
         *
         * @param view a <code>View</code> value
         * @param context a <code>Context</code> value
         * @param cursor a <code>Cursor</code> value
         */
        public final void bindView(final View view, final Context context, final Cursor cursor) {
            RecentChatMember chatMember = null;
            if (cursor != null) {
                long threadId = cursor.getLong(ID);
                String recipientIds = cursor.getString(RECIPIENT_IDS);
                int status = cursor.getInt(STATUS);
                Log.d(TAG, "bindView()threadId:" + threadId
                      + ",recipientIds:" + recipientIds
                      + ",status:" + status);

                if (status > 0) {
                    ThreadMapCache cache = ThreadMapCache.getInstance();
                    MapInfo info = cache.getInfoByThreadId(threadId);
                    if (info != null) {
                        chatMember = new RecentChatMember("", info.getSubject(), info.getChatId(), threadId);
                    }
                } else if (status == 0) {
                    ContactList recipients = ContactList.getByIds(recipientIds, true); // todo:
                    int size = recipients.size();
                    if (size > 0) {
                        StringBuilder numberBuilder = new StringBuilder();
                        StringBuilder nameBuilder = new StringBuilder();
                        numberBuilder.append(recipients.get(0).getNumber());
                        nameBuilder.append(recipients.get(0).getName());
                        for (int i = 1; i < size; i++) {
                            numberBuilder.append(",");
                            numberBuilder.append(recipients.get(i).getNumber());
                            nameBuilder.append(", ");
                            nameBuilder.append(recipients.get(i).getName());
                        }

                        chatMember = new RecentChatMember(numberBuilder.toString(), nameBuilder.toString(), null, 0);
                    }
                }
                if (chatMember != null) {
                    Log.d(TAG, "RecentChatAdapter()"
                          + ",mName:" + chatMember.getMName()
                          + ",mPhoneNumber:" + chatMember.getMPhoneNumber()
                          + ",mChatId:" + chatMember.getMChatId());
                } else {
                    Log.d(TAG, "RecentChatAdapter() chatMember is null");
                }
            }

            if (chatMember == null) {
                return;
            }

            String chatId = chatMember.getMChatId();
            ImageView imgView = (ImageView) view.findViewById(R.id.rcs_forward_item_image);
            if (chatId != null) {
                Bitmap groupBitmap = mPortraitService.requestGroupThumbnail(chatId);
                imgView.setImageBitmap(groupBitmap);
                view.setTag(chatId);
            } else {
                final Portrait portrait = mPortraitService.requestPortrait(chatMember.getMPhoneNumber());
                imgView.setImageBitmap(mPortraitService.decodeString(portrait.mImage));
                view.setTag(chatMember.getMPhoneNumber());
            }
            mRecentChatViewManager.addView(view);
            TextView textView = (TextView) view.findViewById(R.id.rcs_forward_item_text);
            textView.setText(chatMember.getMName());

            final RecentChatMember tmpChatMember = chatMember;
            final ImageView tmpImgView = imgView;
            view.setOnClickListener(new OnClickListener() {
                    public void onClick(View tmpView) {
                        //finish();
                        //mConfirmImageId = mImageIds[position];
                        //mConfirmText = mTexts[position];
                        mSelectChatMember = tmpChatMember;
                        mSelectChatMember.setMBitmap(((BitmapDrawable)tmpImgView.getDrawable()).getBitmap());
                        showDialog(DIALOG_SEND_CONFIRM);
                    }
                });

        }

    }



    //==================================================
    private static final int DIALOG_SEND_CONFIRM = 1;
    private static final int DIALOG_EXCEED_MAX_NUMBER_CONFIRM = 2;
    //private int mConfirmImageId = 0;
    //private String mConfirmText = "empty";
    private RecentChatMember mSelectChatMember;
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog dialogDetails = null;
        switch (id) {
        case DIALOG_SEND_CONFIRM:
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogview = inflater.inflate(R.layout.forward_send_confirm_dialog, null);
            AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
            dialogbuilder.setTitle(R.string.rcs_confirm_forward);
            dialogbuilder.setView(dialogview);
            dialogbuilder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface arg0, final int arg1) {
                        mForwardData.setMThreadId(mSelectChatMember.getMThreadId());
                        mForwardData.setMTo(mSelectChatMember.getMPhoneNumber());
                        determinIfNeddResize(mForwardData);

                    }
                });
            dialogbuilder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface arg0, final int arg1) {
                    }
                });
            dialogDetails = dialogbuilder.create();
            break;

        case DIALOG_EXCEED_MAX_NUMBER_CONFIRM:
            String msg = getString(R.string.forward_send_exceed_max_number, MAX_SEND_NUM);
            Dialog dialog = new AlertDialog.Builder(ForwardActivity.this).setMessage(msg)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface arg0, final int arg1) {
                        }
                    }).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface arg0, final int arg1) {
                                ForwardActivity.this.finish();
                            }
                        }).create();
            dialog.show();
            break;

        default:
            break;
        }
 
        return dialogDetails;
    }
 
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case DIALOG_SEND_CONFIRM:
            final AlertDialog alertDialog = (AlertDialog) dialog;
            ImageView imgView = (ImageView) alertDialog.findViewById(R.id.forward_user_icon);
            imgView.setImageBitmap(mSelectChatMember.getMBitmap());

            TextView bodyName = (TextView) alertDialog.findViewById(R.id.tv_confirm_message);
            bodyName.setText(mSelectChatMember.getMName());
            break;

        default:
            break;
        }
    }

    private void determinIfNeddResize(ForwardData forwardData) {
        String content;
        String filePath = "";
        boolean isSendable = true;
        long maxSize = RCSUtils.getFileTransferMaxSize() * 1024;
        boolean isSendOrgPic = RcsSettingsActivity.getSendMSGStatus(ContextCacher.getPluginContext());

        Log.d(TAG, "determinIfNeddResize() enter.");
        for (int i = 0; i < forwardData.getDataSize(); i++) {
            
             String mineType = forwardData.getDataMineType(i);

             content = forwardData.getDataContent(i);
             Uri uri = Uri.parse(content);
             String scheme = uri.getScheme();
             if ((uri != null) && (scheme != null)) {
                if (scheme.equals("content")) {
                    if (mineType.matches("image/.*")) {
                        Log.d(TAG, "determinIfNeddResize, it is a picture,and content uri");
                        content = forwardData.getDataContent(i);
                        
                        String[] selectColumn = {"_data"};
                        Cursor cursor = mContext.getContentResolver().query(uri, selectColumn, null, null, null);
                        try {
                            if (cursor != null && cursor.moveToFirst()) {
                                filePath = cursor.getString(0);
                                Log.d(TAG, "path is: " + filePath);                 
                            }
                            } catch (Exception e) {
                                // TODO: handle exception
                                e.printStackTrace();
                            }finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        
                        long picSize = RCSUtils.getFileSize(filePath);
                        Log.d(TAG, "maxSize = "+ maxSize + "pic size = " + picSize + "filePath = " + filePath);

                        if (picSize > maxSize && isSendOrgPic && !RcsMessageUtils.isGif(filePath)) {
                            Log.d(TAG, "RCSUtils.getFileSize(filePath) > maxSize, showSendPicAlertDialog(), maxSize = "+ maxSize);
                            showSendPicAlertDialog();
                            isSendable = false;
                            break;
                        }
                     }   
                  } else if (scheme.equals("file")) {
                        Log.d(TAG, "determinIfNeddResize, it is a picture,and file uri");
                        long picSize = RCSUtils.getFileSize(filePath); 
                        Log.d(TAG, "maxSize = "+ maxSize + "pic size = " + picSize + "filePath = " + filePath);
                        if (picSize > maxSize && isSendOrgPic && !RcsMessageUtils.isGif(filePath)) {
                            Log.d(TAG, "RCSUtils.getFileSize(filePath) > maxSize, showSendPicAlertDialog(), maxSize = "+ maxSize);
                            showSendPicAlertDialog();
                            isSendable = false;
                            break;
                        }
                  }
             }     
        }

        if (isSendable) {
            Log.d(TAG, "disSendable = true, call checkConditionsAndSendMessage");
            checkConditionsAndSendMessage(true);
        }
    }

    private void showSendPicAlertDialog() {
        Log.d(TAG, "showSendPicAlertDialog() enter.");
        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        b.setTitle("Picture is exceed maxsize")
            .setMessage("If resize the pic and send ?");
        b.setCancelable(true);
        b.setPositiveButton(android.R.string.ok,
           new DialogInterface.OnClickListener() {
                public final void onClick(DialogInterface dialog, int which) {
                    // need resize
                    checkConditionsAndSendMessage(true);
                }   
           }
        ); 

        b.setNegativeButton(android.R.string.cancel,
            new DialogInterface.OnClickListener() {
                public final void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        b.create().show();
    }

    //==================================================
    //send result
    private void showSendResult() {
        //Toast.makeText(ForwardActivity.this, R.string.rcs_forward_already_sent, Toast.LENGTH_LONG).show();
        ForwardActivity.this.finish();
    }

    // ==================================================
    // progress dialog
    private ProgressDialog mProgressDialog = null;
    private ProgressDialog createProgressDlg() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(ForwardActivity.this);
            //mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage(getString(R.string.forward_is_sending));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setIndeterminate(true);
            //mProgressDialog.setCancelMessage(mHandler.obtainMessage(MessageID.PRESS_BACK));
        }
        return mProgressDialog;
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = createProgressDlg();
        }

        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            try {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mProgressDialog = null;
            }
        }
    }

    //==================================================
    //==================================================
    private void onPreSend() {
        showProgressDialog();
    }

    private void onPostSend() {
        dismissProgressDialog();
        showSendResult();
        ForwardActivity.this.finish();
    }

    //==================================================
    private static final class ForwardSendAsyncTask extends AsyncTask<ForwardData, Void, Void> {
        private ForwardActivity mActivity;
        private boolean mSendMmsFlag = false;
        private Object mLock = new Object();
        private IMessageSender mService;
        //private boolean mIsServiceConnected = false;
        // private ServiceConnection mConn = new ServiceConnection() {
        //         @Override
        //         public void onServiceConnected(final ComponentName name, final IBinder service) {
        //             Log.d(TAG, "onServiceConnected" + name);
        //             mService = IMessageSender.Stub.asInterface(service);
        //             synchronized (mLock) {
        //                 mIsServiceConnected = true;
        //                 mLock.notifyAll();
        //             }
        //         }

        //         @Override
        //         public void onServiceDisconnected(final ComponentName name) {
        //             Log.d(TAG, "onServiceDisconnected" + name);
        //             mIsServiceConnected = false;
        //             mService = null;
        //         }
        //     };

        public ForwardSendAsyncTask(ForwardActivity activity) {
            attach(activity);
        }

        @Override
        protected void onPreExecute() {
            if (mActivity != null) {
                mActivity.onPreSend();
            }
        }

        @Override
        protected Void doInBackground(ForwardData... params) {
            ForwardData forwardData = params[0];
            if (DBG) {
                Log.d(TAG, "doInBackground() forwardData:" + forwardData.toString());
            }

            if (forwardData.getDataSize() > 0) {
                String mimeType = forwardData.getDataMineType(0);
                if (mimeType.equals("mms/pdu") ||
                    mimeType.equals("favorite/pdu")) {
                    mSendMmsFlag = true;

                    //Intent intent = new Intent("com.mediatek.rcs.EmptyService");
                    // Intent intent = new Intent();
                    // intent.setClassName("com.android.mms", "com.mediatek.rcs.EmptyService");
                    // intent.putExtra("TO", forwardData.getMTo());
                    // intent.putExtra("MINETYPE", mimeType);
                    // intent.putExtra("URI", forwardData.getDataContent(0));
                    // try {
                    //     mActivity.startService(intent);
                    //     Log.d(TAG, "doInBackground() startService");
                    //     mActivity.bindService(intent, mConn, Service.BIND_AUTO_CREATE);
                    //     Log.d(TAG, "doInBackground() bindService");
                    // } catch (Exception e) {
                    //     mIsServiceConnected = true;
                    //     e.printStackTrace();
                    // }

                    // synchronized (mLock) {
                    //     while (!mIsServiceConnected) {
                    //         try {
                    //             Log.d(TAG, "doInBackground() waiting for service connected...");
                    //             mLock.wait();
                    //         } catch (InterruptedException e) {
                    //             e.printStackTrace();
                    //         }
                    //     }
                    // }

                    for (int i = 0; i < 10; i++) {
                        mService = RcsProxyManager.getMessageSender();
                        if (mService != null) {
                            break;
                        }
                        Log.d(TAG, "doInBackground() service is null, retry:" + i);
                    }

                    Log.d(TAG, "doInBackground() service connected!");
                }
            }

            for (int i = 0; i < forwardData.getDataSize(); i++) {
                String mimeType = forwardData.getDataMineType(i);
                if (mimeType.equals("mms/pdu") ||
                    mimeType.equals("favorite/pdu")) {
                    if (mService != null) {
                        try {
                            if (mimeType.equals("mms/pdu")) {
                                mService.sendMms(RcsMessageUtils.getSendSubid(mActivity),
                                                 Arrays.asList(forwardData.getMTo().split(",")),
                                                 Uri.parse(forwardData.getDataContent(i)));
                                Log.d(TAG, "doInBackground, sendMms:" + i);
                            } else {
                                mService.sendFavoriteMms(RcsMessageUtils.getSendSubid(mActivity),
                                                         Arrays.asList(forwardData.getMTo().split(",")),
                                                         forwardData.getDataContent(i));
                                Log.d(TAG, "doInBackground, sendFavoriteMms:" + i);
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d(TAG, "doInBackground, sendMms fail, stop sending, mService is null!");
                        break;
                    }
                } else if (mimeType.matches("image/.*") ||
                           mimeType.matches("video/.*") ||
                           mimeType.equals("text/x-vcard") ||
                           mimeType.matches("geo/.*") ||
                           mimeType.matches("audio/.*")) {
                    //IpAttachMessage attachMessage = new IpAttachMessage();
                    IpAttachMessage attachMessage = generateIpMessage(mimeType);
                    attachMessage.setTo(forwardData.getMTo());
                    if (attachMessage instanceof IpImageMessage || attachMessage instanceof IpVoiceMessage || 
                            attachMessage instanceof IpVideoMessage || attachMessage instanceof IpVCardMessage) {
                        processAttachMessageContent(attachMessage, forwardData.getDataContent(i));
                    } else {
                        attachMessage.setPath(forwardData.getDataContent(i));
                    }
                    //add by feng
                    if (!isSentableAttach(attachMessage.getPath())) {
                        processIpMessageAttach(attachMessage);
                    }
                    int result = RCSMessageManager.getInstance(mActivity).sendRCSMessage(forwardData.getMThreadId(), attachMessage);
                    if (result == RCSMessageManager.ERROR_CODE_EXCEED_MAXSIZE) {
                        //TODO: file is bigger
                    }
                } else if (mimeType.matches("text/.*")) {
                    IpTextMessage textMessage = new IpTextMessage();
                    textMessage.setTo(forwardData.getMTo());
                    textMessage.setBody(forwardData.getDataContent(i));
                    RCSMessageManager.getInstance(mActivity).sendRCSMessage(forwardData.getMThreadId(), textMessage);
                }
            }
            try {
                new Thread().sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        private boolean isSentableAttach(String filePath) {
            if (filePath.contains(RcsMessageUtils.IP_MESSAGE_FILE_PATH)) {
                return true;
            } 
            return false;
        }

        private void processIpMessageAttach(IpAttachMessage ipMessage) {
            Log.d(TAG, "processIpMessageAttach() enter.");
            String filePath = ipMessage.getPath();
            int index = filePath.lastIndexOf("/");
            String fileName = filePath.substring(index + 1);
            String mimeType = MediaFile.getMimeTypeForFile(fileName);
            int fileSize = 0;

            if (mimeType != null) {
                fileSize = RCSUtils.getFileSize(filePath);
                if (mimeType.contains(RCSUtils.FILE_TYPE_IMAGE)) {
                    String newPath = RcsMessageUtils.getPhotoDstFilePath(filePath,mActivity);
                    RcsMessageUtils.copyFileToDst(filePath,newPath);
                    ipMessage.setPath(newPath);

                    //boolean isSendOrgPic = RcsSettingsActivity.getSendMSGStatus(RCSUtils.getRcsMessagePluginAppContext());
                    //Log.d(TAG, " isSendOrgPic = " + isSendOrgPic);
                    long maxSize = RCSUtils.getFileTransferMaxSize() * 1024;
                    Log.d(TAG, "maxSize = "+ maxSize + "pic size = " + fileSize + "filePath = " + filePath);
                    if (fileSize > maxSize && !RcsMessageUtils.isGif(filePath)) {
                        // exceed max size and don't org pic sent
                        Log.d(TAG, " photoSize > RCSMaxSize and not org pic sent ");
                        reSizePic(newPath, maxSize);
                    }
                } else if (mimeType.contains(RCSUtils.FILE_TYPE_AUDIO)) {
                    String newPath = RcsMessageUtils.getAudioDstPath(filePath,mActivity);
                    RcsMessageUtils.copyFileToDst(filePath,newPath);
                    ipMessage.setPath(newPath);
                } else if (mimeType.contains(RCSUtils.FILE_TYPE_VIDEO)) {
                    String newPath = RcsMessageUtils.getVideoDstFilePath(filePath,mActivity);
                    RcsMessageUtils.copyFileToDst(filePath,newPath);
                    ipMessage.setPath(newPath);
                } else if (filePath.toLowerCase().endsWith(".vcf")) {
                    String newPath = RcsMessageUtils.getCachePath(mActivity)
                        + filePath;
                    RcsMessageUtils.copyFileToDst(filePath,newPath);
                    ipMessage.setPath(newPath);
                }
                ipMessage.setSize(fileSize);
            }  
        }

        private void reSizePic(String dstPath, long maxSize) {
            Log.d(TAG, "mResizePic(): start resize pic.");
            long maxLen = 
                maxSize > RcsMessageUtils.getPhotoSizeLimit()? RcsMessageUtils.getPhotoSizeLimit():maxSize;
            byte[] img = RcsMessageUtils.resizeImg(dstPath, (float) maxLen);
            if (null == img) {
                return;
            }
            Log.d(TAG, "mResizePic(): put stream to file.");
            try {
                RcsMessageUtils.nmsStream2File(img, dstPath);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        private IpAttachMessage generateIpMessage(String mimeType) {
            IpAttachMessage ipMsg = null;
            if (mimeType.matches("image/.*")) {
                ipMsg = new IpImageMessage();
                ipMsg.setType(IpMessageType.PICTURE);
                return ipMsg;
            } else if (mimeType.matches("video/.*")) {
                ipMsg = new IpVideoMessage();
                ipMsg.setType(IpMessageType.VIDEO);
                return ipMsg;
            } else if (mimeType.equals("text/x-vcard")) {
                ipMsg = new IpVCardMessage();
                ipMsg.setType(IpMessageType.VCARD);
                return ipMsg;
            } else if (mimeType.matches("geo/.*")) {
                ipMsg = new IpGeolocMessage();
                ipMsg.setType(IpMessageType.GEOLOC);
                return ipMsg;
            } else if (mimeType.matches("audio/.*")) {
                ipMsg = new IpVoiceMessage();
                ipMsg.setType(IpMessageType.VOICE);
                return ipMsg;
            }
            return null;
        }
        private void processAttachMessageContent(IpAttachMessage message, String content) {
            boolean processed = false;
            Uri uri = Uri.parse(content);
            String scheme = uri.getScheme();
            if ((uri != null) && (scheme != null)) {
                if (scheme.equals("content")) {
                    Cursor cursor = null;

                    if (message instanceof IpVCardMessage) {
                        Log.d(TAG, "processAttachMessageContent vcard");
                        RCSVCardAttachment va = new RCSVCardAttachment(mActivity);
                        String fileName = va.getVCardFileNameByUri(uri);
                        message.setPath(fileName);
                        Log.d(TAG, "processAttachMessageContent fileName = " + fileName);
                        processed = true;
                    } else if (message instanceof IpImageMessage) {
                        final String[] selectColumn = {
                                "_data"
                        };
                        cursor = mActivity.getContentResolver().query(uri, selectColumn, null, null, null);
                    } else {
                        final String[] selectColumn = {
                                "_data", "duration"
                        };
                        cursor = mActivity.getContentResolver().query(uri, selectColumn, null, null, null);
                    }
                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            String path = cursor.getString(0);
                            Log.d(TAG, "path is: " + path);
                            message.setPath(path);
                            processed = true;
                            if (message instanceof IpVoiceMessage) {
                                IpVoiceMessage voiceMsg = (IpVoiceMessage) message;
                                int duration = cursor.getInt(cursor.getColumnIndex("duration"));
                                duration = duration / 1000 == 0 ? 1 : duration / 1000;
                                voiceMsg.setDuration(duration);
                            } else if (message instanceof IpVideoMessage) {
                                IpVideoMessage videoMsg = (IpVideoMessage) message;
                                int duration = cursor.getInt(cursor.getColumnIndex("duration"));
                                duration = duration / 1000 == 0 ? 1 : duration / 1000;
                                videoMsg.setDuration(duration);
                            }
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } else if (scheme.equals("file")) {
                    message.setPath(uri.getPath());
                    processed = true;
                }
            }
            if (!processed) {
                Log.d(TAG, "processAttachMessageContent: content is: " + content);
                message.setPath(content);
            }
        }

        @Override
        protected void onPostExecute(Void arg0) {
            // if (mService != null) {
            //     try {
            //         mActivity.unbindService(mConn);
            //         //Intent intent = new Intent("com.mediatek.rcs.EmptyService");
            //         Intent intent = new Intent();
            //         intent.setClassName("com.android.mms", "com.mediatek.rcs.EmptyService");
            //         mActivity.stopService(intent);
            //     } catch (Exception e) {
            //         e.printStackTrace();
            //     }
            // }

            super.onPostExecute(arg0);
            if (mActivity != null) {
                mActivity.onPostSend();
            }
        }
        public void detach() {
            mActivity = null;
        }

        public void attach(ForwardActivity activity) {
            mActivity = activity;
        }
    }

    // ==================================================
    // private void forwardMessage(Uri msgUri) {
    //     SendReq sendReq = new SendReq();
    //     String subject = getString(R.string.forward_prefix);
    //     // todo: fill in subject
    //     //PduPersister persister = PduPersister.getPduPersister(ForwardActivity.this);
    //     PduPersister persister = PduPersister.getPduPersister(this);
    //     try {
    //         GenericPdu pdu = persister.load(msgUri);
    //         if (pdu instanceof MultimediaMessagePdu) {
    //             subject += ((MultimediaMessagePdu)pdu).getSubject();
    //             sendReq.setSubject(new EncodedStringValue(subject));

    //             sendReq.setBody(((MultimediaMessagePdu)pdu).getBody());
    //         }

    //     } catch (MmsException e) {
    //         e.printStackTrace();
    //     }

    //     Uri mTempMmsUri = null;
    //     //long mTempThreadId;
    //     try {
    //         mTempMmsUri = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);
    //         // MmsPreferenceActivity.getIsGroupMmsEnabled(ForwardActivity.this)
    //         //mTempThreadId = MessagingNotification.getThreadId(ForwardActivity.this, mTempMmsUri);

    //         long messageSize = 300;
    //         long subId = SubscriptionManager.getDefaultDataSubId();
    //         MmsMessageSender sender = new MmsMessageSender(this, mTempMmsUri, messageSize, subId);
    //         if (!sender.sendMessage(/* mTempThreadId */ 0)) {
    //             SqliteWrapper.delete(this, getContentResolver(), mTempMmsUri, null, null);
    //         }
    //     } catch (MmsException e) {
    //         Log.e(TAG, "Failed to copy message: " + msgUri);
    //         // runOnUiThread(new Runnable() {
    //         //         public void run() {
    //         //             Toast.makeText(ForwardActivity.this, R.string.cannot_save_message, Toast.LENGTH_SHORT).show();
    //         //         }
    //         //     });
    //     }
    // }


    // private void forwardFavoriteMessage(String fileName) {
    //     SendReq sendReq = new SendReq();
    //     String subject = getString(R.string.forward_prefix);
    //     // todo: fill in subject
    //     //PduPersister persister = PduPersister.getPduPersister(ForwardActivity.this);
    //     PduPersister persister = PduPersister.getPduPersister(this);
    //     try {
    //         byte[] pduByte = readFileContent(fileName);
    //         GenericPdu pdu;
    //         try {
    //             pdu = new PduParser(pduByte).parse();

    //             if (pdu instanceof MultimediaMessagePdu) {
    //                 subject += ((MultimediaMessagePdu)pdu).getSubject();
    //                 sendReq.setSubject(new EncodedStringValue(subject));

    //                 sendReq.setBody(((MultimediaMessagePdu)pdu).getBody());
    //             }
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //         }

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }

    //     Uri mTempMmsUri = null;
    //     //long mTempThreadId;
    //     try {
    //         mTempMmsUri = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);
    //         // MmsPreferenceActivity.getIsGroupMmsEnabled(ForwardActivity.this)
    //         //mTempThreadId = MessagingNotification.getThreadId(ForwardActivity.this, mTempMmsUri);

    //         long messageSize = 300;
    //         long subId = SubscriptionManager.getDefaultDataSubId();
    //         MmsMessageSender sender = new MmsMessageSender(this, mTempMmsUri, messageSize, subId);
    //         if (!sender.sendMessage(/* mTempThreadId */ 0)) {
    //             SqliteWrapper.delete(this, getContentResolver(), mTempMmsUri, null, null);
    //         }
    //     } catch (MmsException e) {
    //     }
    // }

    private byte[] readFileContent(String fileName) {
        try {
            InputStream is = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            while ((len = is.read(buffer, 0, 512)) != -1) {
                baos.write(buffer, 0, len);
            }

            is.close();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ==================================================
    private void checkConditionsAndSendMessage(final boolean bCheckEcmMode) {
        // check pin
        final int selectedSubId = RcsMessageUtils.getSendSubid(this);
        if (selectedSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            String msg = getString(R.string.rcs_forward_not_allowed_send);
            Toast.makeText(ForwardActivity.this, msg, Toast.LENGTH_LONG).show();
            Log.d(TAG, "error:selectedSubId is " + selectedSubId);
            return;
        }
        // add CellConnMgr feature
        final CellConnMgr cellConnMgr = new CellConnMgr(getApplicationContext());
        final int state = cellConnMgr.getCurrentState(selectedSubId, CellConnMgr.STATE_FLIGHT_MODE
                                                      | CellConnMgr.STATE_SIM_LOCKED | CellConnMgr.STATE_RADIO_OFF);
        Log.d(TAG, "CellConnMgr, state is " + state);
        if (((state & CellConnMgr.STATE_FLIGHT_MODE) == CellConnMgr.STATE_FLIGHT_MODE) ||
            ((state & CellConnMgr.STATE_RADIO_OFF) == CellConnMgr.STATE_RADIO_OFF)) {
            final ArrayList<String> stringArray = cellConnMgr.getStringUsingState(selectedSubId, state);
            Log.d(TAG, "CellConnMgr, stringArray length is " + stringArray.size());
            if (stringArray.size() == 4) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(stringArray.get(0));
                builder.setMessage(stringArray.get(1));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            //mTextEditor.requestFocus();
                            //updateSendButtonState(true);
                        }
                    });
                builder.show();
            }
        } else if ((state & CellConnMgr.STATE_SIM_LOCKED) == CellConnMgr.STATE_SIM_LOCKED) {
            final ArrayList<String> stringArray = cellConnMgr.getStringUsingState(selectedSubId, state);
            Log.d(TAG, "CellConnMgr, stringArray length is " + stringArray.size());
            if (stringArray.size() == 4) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(stringArray.get(0));
                builder.setCancelable(true);
                builder.setMessage(stringArray.get(1));
                builder.setPositiveButton(stringArray.get(2), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            cellConnMgr.handleRequest(selectedSubId, state);
                            //mTextEditor.requestFocus();
                            //updateSendButtonState(true);
                        }
                    });
                builder.setNegativeButton(stringArray.get(3), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            //mTextEditor.requestFocus();
                            //updateSendButtonState(true);
                        }
                    });
                builder.show();
            }
        } else {
            //checkIpMessageBeforeSendMessage(true);
            mForwardSendAsyncTask = new ForwardSendAsyncTask(ForwardActivity.this);
            mForwardSendAsyncTask.execute(mForwardData);
        }
    }


    Handler mHandler = new Handler();
    PortraitUpdateListener mPortraitUpdateListener;
    RecentChatViewManager mRecentChatViewManager = new RecentChatViewManager();
    private class PortraitUpdateListener implements PortraitService.UpdateListener {
        // will pass the result to caller
        //        public void onPortraitUpdate(String number, String name, String p.mImage) {
        public void onPortraitUpdate(final Portrait p, String chatId) {
            mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //mRecentChatAdapter.notifyDataSetChanged();
                        View view = mRecentChatViewManager.getView(p.mNumber);
                        if (view != null) {
                            Log.d(TAG, "onPortraitUpdate() getTag():" + (String)view.getTag() + ",p.mNumber:" + p.mNumber);
                            ImageView imgView = (ImageView)view.findViewById(R.id.rcs_forward_item_image);
                            if (imgView != null) {
                                imgView.setImageBitmap(PortraitService.decodeString(p.mImage));
                            }
                        }
                    }
                });
        }

        // after query group info done, notify caller the group member number set
        public void onGroupUpdate(String chatId, Set<String> numberSet) {
        }

        public void onGroupThumbnailUpdate(final String chatId, final Bitmap thumbnail) {
            mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //mRecentChatAdapter.notifyDataSetChanged();
                        View view = mRecentChatViewManager.getView(chatId);
                        if (view != null) {
                            Log.d(TAG, "onGroupThumbnailUpdate() getTag():" + (String)view.getTag() + ",chatId:" + chatId);
                            ImageView imgView = (ImageView)view.findViewById(R.id.rcs_forward_item_image);
                            if (imgView != null) {
                                imgView.setImageBitmap(thumbnail);
                            }
                        }
                    }
                });
        }
    }

    class RecentChatViewManager {
        private List<View> mListView = new ArrayList<View>(10);
        public void addView(View view) {
            if (view != null && (getView((String)view.getTag()) == null)) {
                if (mListView.size() > 10) {
                    mListView.remove(0);
                    Log.d(TAG, "RecentChatViewManager.addView(), reach 10, remove 0 firstly");
                }

                mListView.add(view);
                Log.d(TAG, "RecentChatViewManager.addView(), view.getTag():" + view.getTag());
            }
        }

        public View getView(String tag) {
            for (View view : mListView) {
                if (((String)view.getTag()).equals(tag)) {
                    return view;
                }
            }

            return null;
        }
    }


    // ==================================================
    // sms host method
    //public static final String ACTION_CONTACT_SELECTION = "android.intent.action.contacts.list.PICKMULTIPHONEANDEMAILS";
    public static final String ACTION_CONTACT_SELECTION = "android.intent.action.contacts.list.PICKMULTIPHONES";
    public static int getSmsRecipientLimit() {
        return 100;             // todo, confirm the value
    }

    /**
     * parse the input address to be a valid MMS address.
     * - if the address is an email address, leave it as is.
     * - if the address can be parsed into a valid MMS phone number, return the parsed number.
     * - if the address is a compliant alias address, leave it as is.
     */
    public static String parseMmsAddress(String address) {
        // if it's a valid Email address, use that.
        if (Mms.isEmailAddress(address)) {
            return address;
        }

        // if we are able to parse the address to a MMS compliant phone number, take that.
        String retVal = parsePhoneNumberForMms(address);
        if (retVal != null) {
            return retVal;
        }

        // if it's an alias compliant address, use that.
        if (isAlias(address)) {
            return address;
        }

        // it's not a valid MMS address, return null
        return null;
    }

    /**
     * Given a phone number, return the string without syntactic sugar, meaning parens,
     * spaces, slashes, dots, dashes, etc. If the input string contains non-numeric
     * non-punctuation characters, return null.
     */
    private static String parsePhoneNumberForMms(String address) {
        StringBuilder builder = new StringBuilder();
        int len = address.length();

        for (int i = 0; i < len; i++) {
            char c = address.charAt(i);

            // accept the first '+' in the address
            if (c == '+' && builder.length() == 0) {
                builder.append(c);
                continue;
            }

            if (Character.isDigit(c)) {
                builder.append(c);
                continue;
            }

            if (sNumericSugarMap.get(c) == null) {
                return null;
            }
        }
        return builder.toString();
    }


    public static boolean isAlias(String string) {
        int len = string == null ? 0 : string.length();

        if (len < getAliasMinChars() || len > getAliasMaxChars()) {
            return false;
        }

        if (!Character.isLetter(string.charAt(0))) {    // Nickname begins with a letter
            return false;
        }
        for (int i = 1; i < len; i++) {
            char c = string.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '.')) {
                return false;
            }
        }

        return true;
    }

    public static int getAliasMinChars() {
        return 2;               // todo:
    }

    public static int getAliasMaxChars() {
        return 48;              // todo
    }

    // allowable phone number separators
    private static final char[] NUMERIC_CHARS_SUGAR = {
        '-', '.', ',', '(', ')', ' ', '/', '\\', '*', '#', '+'
    };
    private static HashMap sNumericSugarMap = new HashMap(NUMERIC_CHARS_SUGAR.length);
    static {
        for (int i = 0; i < NUMERIC_CHARS_SUGAR.length; i++) {
            sNumericSugarMap.put(NUMERIC_CHARS_SUGAR[i], NUMERIC_CHARS_SUGAR[i]);
        }
    }

 
    public String getNumbersFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        boolean usingColon = intent.getBooleanExtra(USING_COLON, false);
        String selectContactsNumbers = intent.getStringExtra(SELECTION_CONTACT_RESULT);
        if (usingColon) {
            if (selectContactsNumbers == null || selectContactsNumbers.length() < 1) {
                return null;
            }
            String[] numberArray = selectContactsNumbers.split(NUMBERS_SEPARATOR_COLON);
            String numberTempl = "";
            int simcolonIndex = -1;
            int colonIndex = -1;
            int separatorIndex = -1;
            for (int index = 0; index < numberArray.length; index++) {
                numberTempl = numberArray[index];
                simcolonIndex = numberTempl.indexOf(NUMBERS_SEPARATOR_SIMCOLON);
                colonIndex = numberTempl.indexOf(NUMBERS_SEPARATOR_COMMA);
                if (simcolonIndex > 0) {
                    if (colonIndex < 0) {
                        separatorIndex = simcolonIndex;
                    } else if (simcolonIndex < colonIndex) {
                        separatorIndex = simcolonIndex;
                    } else if (colonIndex > 0) {
                        separatorIndex = colonIndex;
                    }
                } else {
                    if (colonIndex > 0) {
                        separatorIndex = colonIndex;
                    }
                }
                if (separatorIndex > 0) {
                    numberArray[index] = numberTempl.substring(0, separatorIndex);
                }
                simcolonIndex = -1;
                colonIndex = -1;
                separatorIndex = -1;
            }
            return TextUtils.join(NUMBERS_SEPARATOR_SIMCOLON, numberArray);
        }

        return selectContactsNumbers;
    }

   // ==================================================
}