package com.mediatek.rcs.message.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaFile;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Telephony.Mms;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;

import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.common.service.PortraitService.UpdateListener;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.data.ForwardSendData;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.proxy.RcsProxyManager;
import com.mediatek.rcs.message.ui.FavoriteDataItem.Constants;
import com.mediatek.rcs.message.ui.FavoriteDataItem.GeolocationData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.MmsData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.MusicData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.PictureData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.TextData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.VcardData;
import com.mediatek.rcs.message.ui.FavoriteDataItem.VideoData;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mtk81368
 */
public class FavoritesActivity extends ListActivity {
    private static final String TAG = "com.mediatek.rcsmessage.favspam/FavoritesActivity";
    private static final String STATE_CHECKED_ITEMS = "checkedItems";
    private static final Uri FAVOTIRE_URI = Uri.parse("content://com.mediatek.message.favorite");
    private static final String STATE_ACTION_MODE = "ActionMode";
    private static final int START_ACTION_MODE_DELAY_TIME = 500;

    private CarrymoreActionMode mCarryMoreActionMode;
    private ActionMode mActionMode;
    private HashSet<Integer> mCheckedItemIds;
    private ListView mListView;
    private FavoriteCursorAdapter mListAdapter = null;
    private boolean mIsInActionMode = false;
    private Handler mHandler;
    private Uri mMmsInsertUri = null;
    protected PortraitService mPortraitService;
    private PortraitUpdateListener mPortraitUpdateListener;
    private Context mContext;
    private FavoriteDbObserver mFavoriteDbObserver;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.favorites_list_view);
        View emptyView = findViewById(R.id.empty);
        mListView = getListView();
        mListView.setEmptyView(emptyView);
        mContext = this;
        init();
        mPortraitService = PortraitService.getInstance(this.getApplicationContext(),
                R.drawable.ic_contact_picture,
                R.drawable.contact_blank_avatar);
        mPortraitUpdateListener = new PortraitUpdateListener();
        mPortraitService.addListener(mPortraitUpdateListener);
        mFavoriteDbObserver = new FavoriteDbObserver(mHandler);
        getContentResolver().registerContentObserver(FAVOTIRE_URI, true, mFavoriteDbObserver);
        registerForContextMenu(mListView);

        if (savedInstanceState != null) {
            boolean isActionMode = savedInstanceState.getBoolean(STATE_ACTION_MODE, false);
            if (isActionMode) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActionMode = FavoritesActivity.this.startActionMode(mCarryMoreActionMode);
                        mCarryMoreActionMode.restoreState(savedInstanceState);
                    }
                }, START_ACTION_MODE_DELAY_TIME);
            }
        }
    }

    private void init() {
        initListAdapter();
        initHandler();
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {

                default:
                    break;
                }
            }
        };
    }

    public boolean isInActionMode() {
        return mIsInActionMode;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionMode != null) {
            Log.v(TAG, " onSaveInstanceState mActionMode != null");
            outState.putBoolean(STATE_ACTION_MODE, true);
            mCarryMoreActionMode.saveState(outState);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        Log.v(TAG, "onCreateContextMenu");
        menu.setHeaderTitle(R.string.operation);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.favorite_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
            menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        Cursor cursor = (Cursor) mListView.getItemAtPosition(menuInfo.position);
        int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
        HashSet<Integer> checkedItemIds = new HashSet<Integer>();
        checkedItemIds.add(msgId);
        Log.v(TAG, "menuInfo.position = " + menuInfo.position);
        Log.v(TAG, "select.getMsgId() = " + msgId);

        switch (item.getItemId()) {
        case R.id.menu_delete:
            deleteCheckItem(checkedItemIds);
            return true;

        case R.id.menu_foward:
            if (RcsMessageUtils.isSupportRcsForward(this)) {
                ArrayList<ForwardSendData> dataList = new ArrayList<ForwardSendData>();
                ForwardSendData sendData = getForwardIntent(cursor);
                dataList.add(sendData);
                Intent sendIntent = new Intent();
                sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
                sendIntent.setType("multisend/favorite");
                sendIntent.putParcelableArrayListExtra("android.intent.RCS_MULTI_SEND", dataList);
                startActivity(sendIntent);
                return true;
            } else {
                forwordInNoRcsStatus(cursor);
                return true;
            }

        case R.id.menu_info:
            showMessageDetails(cursor);
            return true;

        case R.id.menu_more:
            Log.v(TAG, "menu_more is pressed, then will open action mode");
            mCarryMoreActionMode = new CarrymoreActionMode();
            mActionMode = this.startActionMode(mCarryMoreActionMode);
            mListAdapter.notifyDataSetChanged();
            return true;

        default:
            Log.e(TAG, "conetext item select error!");
            return super.onContextItemSelected(item);
        }
    }

    private void forwordInNoRcsStatus(Cursor cursor) {
        int msgType = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        String path = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_PATH));
        Log.d(TAG, "menu_foward msgType = " + msgType);

        if (msgType == Constants.MSG_TYPE_MMS) {
            String mmsSubject = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            String pduPath = path;
            Log.d(TAG, "havent config rcs, forward mms, insert pdu first. pdu path = " + pduPath);
            Intent intent = new Intent();
            intent.setClassName("com.android.mms", "com.android.mms.ui.ForwardMessageActivity");
            intent.putExtra("subject", mmsSubject);

            Uri uri = insertMmsFromPdu(pduPath);
            Log.d(TAG, "forwordInNoRcsStatus, uri = " + uri);
            if (uri == null) {
                Log.d(TAG, "forwordInNoRcsStatus, mUri = null, return");
                return;
            }
            Log.d(TAG, "uri = " + uri);

            intent.putExtra("msg_uri", uri);
            this.startActivity(intent);
        } else if (msgType == Constants.MSG_TYPE_SMS
                || ((msgType == Constants.MSG_TYPE_IPMSG) && path == null)) {
            String textBody = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            Intent intent = new Intent();
            intent.setClassName("com.android.mms", "com.android.mms.ui.ForwardMessageActivity");
            intent.putExtra("sms_body", textBody);
            this.startActivity(intent);
        } else {
            Toast.makeText(this, R.string.toast_sms_unable_forward, Toast.LENGTH_SHORT).show();
        }
    }

    private void initListAdapter() {
        Log.v(TAG, " initListAdapter()");
        Cursor cursor = this.getContentResolver()
                .query(FAVOTIRE_URI, null, null, null, "date DESC");
        cursor.moveToFirst();
        mListAdapter = new FavoriteCursorAdapter(this, cursor, false);
        setListAdapter(mListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if the insert mms is not delete,deleted.
        // mMmsInsertUri is not null only when the MMSplayer return
        // FavoritesActivity.
        if (mMmsInsertUri != null) {
            int count = this.getContentResolver().delete(mMmsInsertUri, null, null);
            Log.d(TAG, "delete insert mms count = " + count);
            mMmsInsertUri = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.my_favorites);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (mPortraitService != null && mPortraitUpdateListener != null) {
            mPortraitService.removeListener(mPortraitUpdateListener);
        }
        this.getContentResolver().unregisterContentObserver(mFavoriteDbObserver);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void deleteMsg(ArrayList<Integer> delItemIds) {
        // delete favorites
        int delCount = delItemIds.size();
        Log.v(TAG, "deleteMsg delete DataCount = " + delCount);
        if (delCount <= 0) {
            Log.v(TAG, "deleteMsg delete DataCount = 0, return");
            return;
        }
        String selectArg = "_id IN (";
        for (int msgId : delItemIds) {
            selectArg = selectArg + msgId + ",";
        }
        selectArg = selectArg.substring(0, selectArg.length() - 1);
        selectArg = selectArg + ")";
        Log.d(TAG, "selectArg = " + selectArg);
        int count = this.getContentResolver().delete(FAVOTIRE_URI, selectArg, null);
        Log.d(TAG, "have delete count = " + count);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.v(TAG, "onListItemClick");
        super.onListItemClick(l, v, position, id);
        Cursor cursor = (Cursor) l.getItemAtPosition(position);
        int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        String path = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_PATH));

        if (mActionMode == null) {
            if (Constants.MSG_TYPE_MMS == type) {
                // if the item is mms, first need insert message db.
                String pduPath = path;
                Log.d(TAG, "pdu path = " + pduPath);
                openMms(pduPath);
            } else {
                Intent intent = getClickIntent(cursor);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.d(TAG, "ActivityNotFoundException");
                    e.printStackTrace();
                    Toast.makeText(FavoritesActivity.this, R.string.cant_open_item,
                            Toast.LENGTH_SHORT).show();
                }
            }
        } else if (mCarryMoreActionMode != null) {
            Log.v(TAG, "onListItemClick, (mCarryMoreActionMode != null");
            mCarryMoreActionMode.setItemChecked(v, position,
                    !(((FavoriteListItem) v).isItemChecked()));
        }
    }

    private void deleteCheckItem(HashSet<Integer> checkedItemIds) {
        ArrayList<Integer> delItemIds = new ArrayList<Integer>();
        for (int id : checkedItemIds) {
            delItemIds.add(id);
        }

        Log.d(TAG, "deleteCheckItem checkedItemIds size = " + checkedItemIds.size());
        Log.d(TAG, "deleteCheckItem delItemIds size = " + delItemIds.size());
        DeleteCheckItemTask deleteTask = new DeleteCheckItemTask();
        deleteTask.execute(delItemIds);
    }

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

    /**
     * When user click "more" menuItem on context menu, will open this action
     * mode. It used to select item that want to operator.
     * 
     * @author mtk81368
     */
    class CarrymoreActionMode implements ActionMode.Callback {
        private int mCheckedCount;
        private ActionMode mMode;

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (mCheckedCount == 0) {
                Toast.makeText(FavoritesActivity.this, R.string.no_item_selected,
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            switch (item.getItemId()) {
            case R.id.favorite_foward:
                if (!RcsMessageUtils.isSupportRcsForward(FavoritesActivity.this)) {
                    Log.d(TAG, "forwordInNoRcsStatus");
                    if (mCheckedCount > 1) {
                        Toast.makeText(FavoritesActivity.this, "please foward only one item once.",
                                Toast.LENGTH_SHORT).show();
                        setAllItemChecked(false);
                        return false;
                    } else {
                        for (int id : mCheckedItemIds) {
                            String selection = "_id is " + id;
                            Cursor cursor = mContext.getContentResolver().query(FAVOTIRE_URI, null,
                                    selection, null, null);
                            cursor.moveToFirst();
                            forwordInNoRcsStatus(cursor);
                            cursor.close();
                        }
                        mMode.finish();
                        break;
                    }
                }

                Log.d(TAG, "in rcs status forward");
                if (mCheckedCount > 5) {
                    Toast.makeText(FavoritesActivity.this, "please foward less 5 items once",
                            Toast.LENGTH_SHORT).show();
                    setAllItemChecked(false);
                    return false;
                }
                if (mCheckedItemIds.size() > 1) {
                    for (int id : mCheckedItemIds) {
                        String selection = "_id is " + id;
                        Cursor cursor = mContext.getContentResolver().query(FAVOTIRE_URI, null,
                                selection, null, null);
                        cursor.moveToFirst();
                        int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
                        if (type == Constants.MSG_TYPE_MMS) {
                            Toast.makeText(FavoritesActivity.this,
                                    "mms can't forward with other message together",
                                    Toast.LENGTH_SHORT).show();
                            setAllItemChecked(false);
                            cursor.close();
                            return false;
                        }
                    }
                }

                Log.v(TAG, "onActionItemClicked favorite_foward");
                ArrayList<ForwardSendData> dataList = new ArrayList<ForwardSendData>();
                for (int id : mCheckedItemIds) {
                    String selection = "_id is " + id;
                    Cursor cursor = mContext.getContentResolver().query(FAVOTIRE_URI, null,
                            selection, null, null);
                    cursor.moveToFirst();
                    ForwardSendData sendData = getForwardIntent(cursor);
                    if (sendData != null) {
                        dataList.add(sendData);
                    }
                    cursor.close();
                }
                Intent sendIntent = new Intent();
                sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
                sendIntent.setType("multisend/favorite");
                sendIntent.putParcelableArrayListExtra("android.intent.RCS_MULTI_SEND", dataList);
                startActivity(sendIntent);
                mMode.finish();
                break;

            case R.id.favorite_delete:
                deleteCheckItem(mCheckedItemIds);
                Log.d(TAG, "CarrymoreActionMode favorite_delete size = " + mCheckedItemIds.size());
                mMode.finish();
                break;

            default:
                break;
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onCreateActionMode");
            mIsInActionMode = true;
            mMode = mode;
            mMode.setTitleOptionalHint(false);
            mListView.setLongClickable(false);
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.fwd_del_select_menu, menu);
            mCheckedItemIds = new HashSet<Integer>();
            setAllItemChecked(false);
            updateTitle();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mIsInActionMode = false;
            Log.v(TAG, "onDestroyActionMode");
            setAllItemChecked(false);
            mCheckedItemIds = null;
            mCheckedCount = 0;
            mActionMode = null;
            mListView.setLongClickable(true);
            mListAdapter.notifyDataSetChanged();
        }

        private void updateTitle() {
            StringBuilder builder = new StringBuilder();
            builder.append(mCheckedCount);
            builder.append(" ");
            builder.append(getString(R.string.selected));
            mMode.setTitle(builder.toString());
        }

        public void setItemChecked(View view, int position, final boolean checked) {
            Log.v(TAG, "setItemChecked checked = " + checked);
            if (((FavoriteListItem) view).isItemChecked() != checked) {
                ((FavoriteListItem) view).setChecked(checked);
                Cursor cursor = (Cursor) mListView.getItemAtPosition(position);
                int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
                if (checked) {
                    Log.v(TAG, "onCreateActionMode setItemChecked add msgid = " + msgId);
                    mCheckedItemIds.add(msgId);
                    mCheckedCount++;
                } else {
                    Log.v(TAG, "onCreateActionMode setItemChecked remove msgid = " + msgId);
                    mCheckedItemIds.remove(msgId);
                    mCheckedCount--;
                }
            }
            updateTitle();
        }

        private void setAllItemChecked(boolean checked) {
            Log.d(TAG, "setAllItemChecked checked = " + checked);
            mCheckedCount = 0;
            mCheckedItemIds.clear();
            ListAdapter adapter = mListView.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "show Check box error, adapter null");
                return;
            }
            if (checked) {
                String[] cols = new String[] { Constants.COLUMN_NAME_ID };
                Cursor cursor = mContext.getContentResolver().query(FAVOTIRE_URI, cols, null, null,
                        null);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
                    mCheckedItemIds.add(msgId);
                    mCheckedCount++;
                    cursor.moveToNext();
                }
                cursor.close();
            }

            updateTitle();
            ((BaseAdapter) adapter).notifyDataSetChanged();
        }

        public void saveState(final Bundle outState) {
            ArrayList<Integer> list = new ArrayList<Integer>();
            for (Integer item : mCheckedItemIds) {
                list.add(item);
            }
            outState.putIntegerArrayList(STATE_CHECKED_ITEMS, list);
        }

        public void restoreState(Bundle state) {
            ArrayList<Integer> list = state.getIntegerArrayList(STATE_CHECKED_ITEMS);
            if (list != null && !list.isEmpty()) {
                for (Integer item : list) {
                    mCheckedItemIds.add(item);
                }
            }
            ListAdapter adapter = mListView.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "show Check box error, adapter null");
                return;
            }
            if (adapter.getCount() > 0) {
                confirmSyncCheckedPositons();
            }
        }

        public void confirmSyncCheckedPositons() {
            mCheckedCount = 0;
            HashSet<Integer> tempCheckedIds = new HashSet<Integer>();
            ListAdapter adapter = mListView.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "show Check box error, adapter null");
                return;
            }
            int count = adapter.getCount();
            for (int position = 0; position < count; position++) {
                FavoriteDataItem item = (FavoriteDataItem) adapter.getItem(position);

                if (mCheckedItemIds.contains(item.getMsgId())) {
                    tempCheckedIds.add(item.getMsgId());
                    mCheckedCount++;
                }

            }
            mCheckedItemIds.clear();
            mCheckedItemIds = tempCheckedIds;
            updateTitle();
            ((BaseAdapter) adapter).notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
            mIsInActionMode = true;
            return false;
        }
    }

    private class FavoriteCursorAdapter extends CursorAdapter {

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            Log.d(TAG, "change new cursor");
        }

        public FavoriteCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            Log.d(TAG, "FavoriteCursorAdapter");
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Log.d(TAG, "bindView");
            if (!(view instanceof FavoriteListItem)) {
                Log.e(TAG, "Unexpected bound view: " + view);
                return;
            }
            int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
            FavoriteDataItem dataItem = new FavoriteDataItem(context, cursor);
            dataItem.initData(cursor, mPortraitService);

            ((FavoriteListItem) view).setType(dataItem.getTypeData().getType());
            ((FavoriteListItem) view).setBaseData(dataItem);
            ((FavoriteListItem) view).setTypeData(dataItem, dataItem.getTypeData());
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);

            boolean isActionMode = isInActionMode();
            Log.i(TAG, "getView  isActionMode = + " + isActionMode);
            if (isActionMode) {
                ((FavoriteListItem) view).showCheckBox(true);
                if (mCheckedItemIds != null && mCheckedItemIds.contains(dataItem.getMsgId())) {
                    checkbox.setChecked(true);
                } else {
                    checkbox.setChecked(false);
                }
            } else {
                ((FavoriteListItem) view).showCheckBox(false);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(TAG, "newView");
            return new FavoriteListItem(context);
        }

    }

    protected boolean showMessageDetails(Cursor cursor) {
        String typeName = getTypeName(cursor);
        long date = cursor.getLong(cursor.getColumnIndex(Constants.COLUMN_NAME_DATE));
        String from = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_ADDRESS));
        if (from == null) {
            from = mContext.getString(R.string.me);
        }
        String messageDetails = getTextMessageDetails(FavoritesActivity.this, typeName, date, from);
        Log.d(TAG, "showMessageDetails. messageDetails:" + messageDetails);
        new AlertDialog.Builder(this).setTitle(R.string.message_details_title)
                .setMessage(messageDetails).setCancelable(true).show();
        return true;
    }

    private static String getTextMessageDetails(Context context, String typeName, long date,
            String from) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        details.append(res.getString(R.string.message_type_label));
        details.append(typeName);

        details.append('\n');
        details.append(res.getString(R.string.date_label));
        String strDate = RcsMessageUtils.formatTimeStampString(context, date, false);
        details.append(strDate);

        details.append('\n');
        details.append(res.getString(R.string.from_label));
        details.append(from);
        return details.toString();
    }

    public Uri insertMmsFromPdu(String pduPath) {
        Log.d(TAG, "insertMmsFromPdu");
        Uri ret = null;
        byte[] pduByteArray = readFileContent(pduPath);
        if (pduByteArray == null) {
            Log.e(TAG, "forwordInNoRcsStatus, pduByteArray == null, return null");
            return null;
        }
        SendReq sendReq = new SendReq();
        MultimediaMessagePdu mmsPdu = (MultimediaMessagePdu) new PduParser(pduByteArray, false)
                .parse();
        if (mmsPdu.getSubject() != null) {
            sendReq.setSubject(mmsPdu.getSubject());
        }
        sendReq.setBody(mmsPdu.getBody());
        PduPersister persister = PduPersister.getPduPersister(this);
        try {
            ret = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);
        } catch (MmsException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private boolean openMms(String pduName) {
        Log.d(TAG, "openMms pduName = " + pduName);
        mMmsInsertUri = insertMmsFromPdu(pduName);
        if (mMmsInsertUri == null) {
            Log.e(TAG, "openMms mMmsInsertUri = null");
        }
        Log.d(TAG, "mMmsInsertUri = " + mMmsInsertUri);

        Intent intent = new Intent();
        intent.setClassName("com.android.mms", "com.android.mms.ui.MmsPlayerActivity");
        intent.setData(mMmsInsertUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        return true;
    }

    /**
     * The class is used to execute delete select data from database.
     * 
     * @author mtk81368
     */
    private class DeleteCheckItemTask extends AsyncTask<ArrayList<Integer>, String, Long> {

        private ProgressDialog mDeletingDialog;
        private ArrayList<Integer> mMsgIdList;

        public DeleteCheckItemTask() {
            mDeletingDialog = new ProgressDialog(FavoritesActivity.this);
            mDeletingDialog.setCancelable(false);
            mDeletingDialog.setMessage(getString(R.string.delete_please_wait));
            mDeletingDialog.setIndeterminate(true);
        }

        @Override
        protected void onPostExecute(Long arg0) {
            super.onPostExecute(arg0);
            int delCount = mMsgIdList.size();
            Log.v(TAG, "onPostExecute delCount = " + delCount);

            for (Integer id : mMsgIdList) {
                String selection = "_id is " + id;
                int count = mContext.getContentResolver().delete(FAVOTIRE_URI, selection, null);
            }

            Activity activity = FavoritesActivity.this;
            if (activity != null && mDeletingDialog != null) {
                mDeletingDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = FavoritesActivity.this;
            if (activity != null && mDeletingDialog != null) {
                mDeletingDialog.show();
            }
        }

        @Override
        protected Long doInBackground(ArrayList<Integer>... params) {
            mMsgIdList = params[0];
            int delCount = mMsgIdList.size();
            Log.v(TAG, "doInBackground delCount = " + delCount);
            deleteMsg(mMsgIdList);
            return null;
        }
    }

    /**
     * Implement the portrait updata listener. Adapter will updata the info when
     * the contact name or image is change.
     * 
     * @author mtk81368
     */
    private class PortraitUpdateListener implements UpdateListener {
        public void onPortraitUpdate(Portrait p, String chatId) {
            Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate");
            String number = p.mNumber;
            Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate number = " + number);
            if (mListView == null) {
                return;
            }

            int count = mListView.getCount();
            Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate count = " + count);
            for (int index = 0; index < count; index++) {
                Cursor cursor = (Cursor) mListView.getItemAtPosition(index);
                if (cursor == null) {
                    Log.d(TAG, "PortraitUpdateListener cursor = null, return");
                    return;
                }
                String address = cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_NAME_ADDRESS));
                Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate address  = " + address);
                if (address != null && address.equals(number)) {
                    FavoriteListItem item = (FavoriteListItem) mListView.getChildAt(index);
                    if (item != null) {
                        item.updateView(mPortraitService.decodeString(p.mImage), p.mName);
                    }
                }
            }
        }

        public void onGroupUpdate(String chatId, Set<String> numberSet) {
            Log.d(TAG, "PortraitUpdateListener. onGroupUpdate");
        }

        public void onGroupThumbnailUpdate(String chatId, Bitmap thumbnail) {
            Log.d(TAG, "PortraitUpdateListener. onGroupThumbnailUpdate() ");
        }
    }

    private String getTypeName(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        String path = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_PATH));
        switch (type) {
        case Constants.MSG_TYPE_SMS:
            return mContext.getString(R.string.text_message);

        case Constants.MSG_TYPE_IPMSG:
            if (path == null) {
                return mContext.getString(R.string.imtext_type_name);
            }
            String ct = analysisFileType(path);
            if (ct != null) {
                if (ct.equals(Constants.CT_TYPE_VEDIO)) {
                    return mContext.getString(R.string.video_type_name);
                } else if (ct.equals(Constants.CT_TYPE_AUDIO)) {
                    return mContext.getString(R.string.music_type_name);
                } else if (ct.equals(Constants.CT_TYPE_IMAGE)) {
                    return mContext.getString(R.string.pic_type_name);
                } else if (ct.equals(Constants.CT_TYPE_VCARD)) {
                    return mContext.getString(R.string.vcard_type_name);
                } else if (ct.equals(Constants.CT_TYPE_GEOLOCATION)) {
                    Log.d(TAG, "GeolocationData()");
                    return mContext.getString(R.string.map_type_name);
                } else {
                    return mContext.getString(R.string.unknown);
                }
            }

        default:
            Log.e(TAG, "unknown type = " + type);
            return mContext.getString(R.string.unknown);
        }
    }

    private ForwardSendData getForwardIntent(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        String path = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_PATH));
        Intent intent = new Intent();
        switch (type) {
        case Constants.MSG_TYPE_SMS:
            return new ForwardSendData("text/plain", cursor.getString(cursor
                    .getColumnIndex(Constants.COLUMN_NAME_BODY)));

        case Constants.MSG_TYPE_IPMSG:
            if (path == null) {
                return new ForwardSendData("text/plain", cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_NAME_BODY)));
            }
            String ct = analysisFileType(path);
            if (ct != null) {
                if (ct.equals(Constants.CT_TYPE_VEDIO)) {
                    return new ForwardSendData("video/mp4", path);
                } else if (ct.equals(Constants.CT_TYPE_AUDIO)) {
                    return new ForwardSendData("video/mp4", path);
                } else if (ct.equals(Constants.CT_TYPE_IMAGE)) {
                    return new ForwardSendData("image/jpeg", path);
                } else if (ct.equals(Constants.CT_TYPE_VCARD)) {
                    return new ForwardSendData("text/x-vcard", path);
                } else if (ct.equals(Constants.CT_TYPE_GEOLOCATION)) {
                    Log.d(TAG, "GeolocationData()");
                    return new ForwardSendData("geo/*", path);
                } else {
                    return null;
                }
            }

        default:
            Log.e(TAG, "unknown type = " + type);
            return null;
        }
    }

    private Intent getClickIntent(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        String path = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_PATH));
        switch (type) {
        case Constants.MSG_TYPE_SMS:
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("path",
                    cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY)));
            intent.putExtra("fav_spam", true);
            intent.putExtra("type", 0);
            intent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            return intent;

        case Constants.MSG_TYPE_IPMSG:
            if (path == null) {
                Log.d(TAG, "getClickIntent new TextData(Constants.MSG_TYPE_IPTEXT)");
                Intent ipTextIntent = new Intent();
                ipTextIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ipTextIntent.putExtra("path",
                        cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY)));
                ipTextIntent.putExtra("fav_spam", true);
                ipTextIntent.putExtra("type", 0);
                ipTextIntent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
                return ipTextIntent;
            }
            Log.d(TAG, "getClickIntent path = " + path);
            File file = new File(path);
            if (file == null || !file.exists()) {
                Log.d(TAG, "getClickIntent file is null or file is not exites");
                return null;
            }
            String ct = analysisFileType(path);
            if (ct != null) {
                if (ct.equals(Constants.CT_TYPE_VEDIO)) {
                    Log.d(TAG, "getClickIntent new Video");
                    Intent vidioIntent = new Intent();
                    vidioIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    vidioIntent.putExtra("path", path);
                    vidioIntent.putExtra("fav_spam", true);
                    vidioIntent.putExtra("type", 9);
                    vidioIntent
                            .setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
                    return vidioIntent;
                } else if (ct.equals(Constants.CT_TYPE_AUDIO)) {
                    Log.d(TAG, "getClickIntent audio");
                    Intent audioIntent = new Intent(Intent.ACTION_VIEW);
                    File audioFile = new File(path);
                    Uri audioUri = Uri.fromFile(audioFile);
                    audioIntent.setDataAndType(audioUri, "audio/amr");
                    return audioIntent;
                } else if (ct.equals(Constants.CT_TYPE_IMAGE)) {
                    Log.d(TAG, "getClickIntentnew PictureData()");
                    Intent imageIntent = new Intent();
                    imageIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    imageIntent.putExtra("path", path);
                    imageIntent.putExtra("fav_spam", true);
                    imageIntent.putExtra("type", 4);
                    imageIntent
                            .setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
                    return imageIntent;
                } else if (ct.equals(Constants.CT_TYPE_VCARD)) {
                    Log.d(TAG, "getClickIntentnew Vcard");
                    Intent vcardIntent = new Intent(Intent.ACTION_VIEW);
                    File vcardFile = new File(path);
                    Uri vcardUri = Uri.fromFile(vcardFile);
                    vcardIntent.setDataAndType(vcardUri, "text/x-vCard".toLowerCase());
                    vcardIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    return vcardIntent;
                } else if (ct.equals(Constants.CT_TYPE_GEOLOCATION)) {
                    Log.d(TAG, "getClickIntentnew GeolocationData()");
                    GeoLocXmlParser parser = GeoLocUtils.parseGeoLocXml(path);
                    double latitude = parser.getLatitude();
                    double longitude = parser.getLongitude();
                    Log.d(TAG, "parseGeoLocXml:latitude=" + latitude + ",longitude=" + longitude);
                    if (latitude != 0.0 || longitude != 0.0) {
                        Uri geoUri = Uri.parse("geo:" + latitude + "," + longitude);
                        Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
                        return geoIntent;
                    } else {
                        Toast.makeText(this, this.getString(R.string.geoloc_map_failed),
                                Toast.LENGTH_SHORT).show();
                        return null;
                    }
                } else {
                    return null;
                }
            }

        default:
            Log.e(TAG, "unknown type = " + type);
            return null;
        }
    }

    private String analysisFileType(String filePath) {
        if (filePath != null) {
            String mimeType = MediaFile.getMimeTypeForFile(filePath);
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        RCSUtils.getFileExtension(filePath));
            }
            if (mimeType != null) {
                if (mimeType.contains(Constants.FILE_TYPE_IMAGE)) {
                    return Constants.CT_TYPE_IMAGE;
                } else if (mimeType.contains(Constants.FILE_TYPE_AUDIO)
                        || mimeType.contains("application/ogg")) {
                    return Constants.CT_TYPE_AUDIO;
                } else if (mimeType.contains(Constants.FILE_TYPE_VIDEO)) {
                    return Constants.CT_TYPE_VEDIO;
                } else if (filePath.toLowerCase().endsWith(".vcf")) {
                    return Constants.CT_TYPE_VCARD;
                } else if (filePath.toLowerCase().endsWith(".xml")) {
                    return Constants.CT_TYPE_GEOLOCATION;
                } else {
                    Log.d(TAG, "analysisFileType() other type add here!");
                }
            }
        } else {
            Log.w(TAG, "analysisFileType(), file name is null!");
        }
        return null;
    }

    /**
     * if favorite database has new data insert, then ui will load data from
     * database again.
     * 
     * @author mtk81368
     */
    private final class FavoriteDbObserver extends ContentObserver {

        public FavoriteDbObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "FavoriteDdObserver onChange");
            Cursor cursor = mContext.getContentResolver().query(FAVOTIRE_URI, null, null, null,
                    "date DESC");
            cursor.moveToFirst();
            mListAdapter.changeCursor(cursor);
        }
    }
}
