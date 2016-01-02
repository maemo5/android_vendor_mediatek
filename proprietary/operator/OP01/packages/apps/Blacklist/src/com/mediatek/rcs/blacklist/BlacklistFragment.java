/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcs.blacklist;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
//import android.widget.Toast;

import com.mediatek.rcs.blacklist.BlacklistData.BlacklistTable;

/**
 * BlacklistFragment.
 */
public class BlacklistFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<Cursor>, BlacklistUtils.SyncWithContactsCallback {

    private static final String TAG = "Blacklist";

    public static final String FRAGMENT_TAG = "BlacklistFragment";
    private static final String INPUT_DIALOG_TAG = "blacklist_input";

    private static final Uri mUri = BlacklistData.AUTHORITY_URI;
    private static final String[] BLACKLIST_PROJECTION = {BaseColumns._ID,
                                                        BlacklistTable.DISPLAY_NAME,
                                                        BlacklistTable.PHONE_NUMBER};
    private static final String CONTACTS_ADD_ACTION =
                                    "android.intent.action.contacts.list.PICKMULTIPHONES";
    private static final String CONTACTS_ADD_ACTION_DATA_RESULT =
                                    "com.mediatek.contacts.list.pickdataresult";

    private static final int REQUEST_CODE = 100;

    private static final int MENU_ID_ADD = Menu.FIRST;
    private static final int MENU_ID_DELETE = Menu.FIRST + 1;

    private static final int MSG_ID_IMPORT_CONTACTS = 100;

    private static final int TOTAL_MEMBER_MAX = BlacklistTable.RECORDS_NUMBER_MAX;

    private BlacklistAdapter mAdapter;
    private Cursor mCursor;
    private boolean mSkipRequery = true;

    private View mInputDlgView;
    private MenuItem mAddMenu;
    private MenuItem mDeleteMenu;
    private ImageButton mImportBtn;

    private AddMembersTask mAddTask;
    private Intent mResultIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate");

        setHasOptionsMenu(true);

        getLoaderManager().initLoader(0, null, this);
        mSkipRequery = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        log("onCreateView");

        //View view = inflater.inflate(R.layout.blacklist_fragment, container, false);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new BlacklistAdapter(getActivity(), mCursor);
        mAdapter.setListViewMode(BlacklistAdapter.LIST_VIEW_NORMAL);
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume");

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                                        ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.blacklist);
        }

        setEmptyText(getString(R.string.list_empty));

        if (!mSkipRequery) {
            //getLoaderManager().restartLoader(0, null, this);
            restartQuery();
        }
        mSkipRequery = false;

        BlacklistUtils.startSyncWithContacts(getActivity().getContentResolver(), this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mAddMenu = menu.add(Menu.NONE, MENU_ID_ADD, MENU_ID_ADD, R.string.add);
        mAddMenu.setIcon(R.drawable.ic_menu_add_holo_light);
        mAddMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        mDeleteMenu = menu.add(Menu.NONE, MENU_ID_DELETE, MENU_ID_DELETE, R.string.delete);
        mDeleteMenu.setIcon(R.drawable.ic_menu_trash_holo_light);
        mDeleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mAdapter.getCount() > 0) {
            mDeleteMenu.setVisible(true);
        } else {
            mDeleteMenu.setVisible(false);
        }

        if (mAdapter.getCount() < TOTAL_MEMBER_MAX) {
            mAddMenu.setVisible(true);
        } else {
            mAddMenu.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = false;
        switch (item.getItemId()) {
        case MENU_ID_ADD:
            showInputDialog();
            break;

        case MENU_ID_DELETE:
            showDeleteFragment();
            break;

        case android.R.id.home:
            getActivity().finish();
            ret = true;
            break;

        default:
            ret = false;
        }

        return ret;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAddTask != null) {
            mAddTask.cancel(true);
        }

        BlacklistUtils.cancelSyncWithContacts(this);
    }

    private void showInputDialog() {
        final InputDialog inputDlg = new InputDialog();
        inputDlg.show(getFragmentManager(), INPUT_DIALOG_TAG);
    }

    private void showDeleteFragment() {
        BlacklistDeleteFragment deleteFragment = new BlacklistDeleteFragment();
        deleteFragment.setCursor(mCursor);

        getFragmentManager().beginTransaction()
                .replace(R.id.blacklistActivity, deleteFragment,
                    BlacklistDeleteFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != getActivity().RESULT_OK) {
            return;
    }

        mResultIntent = data;

            mAddTask = new AddMembersTask();
        log("import from contacts start");

        if (mAddTask.getStatus() != AsyncTask.Status.RUNNING) {
            mAddTask.execute(requestCode, resultCode);
        }
    }

    private void restartQuery() {
        log("restart query");
        mAdapter.notifyDataSetInvalidated();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity().getApplicationContext(),
                                mUri, BLACKLIST_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
        mCursor = arg1;
        mAdapter.swapCursor(arg1);
        mAdapter.notifyDataSetChanged();
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
    }

    @Override
    public void onUpdatedWithContacts(boolean result) {
        log("Sync with contacts done: " + result);
        if (result) {
            restartQuery();
        }
    }

    /**
     * AddMembersTask.
     */
    private class AddMembersTask extends AsyncTask<Integer, String, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            final long[] contactsId = mResultIntent
                    .getLongArrayExtra(CONTACTS_ADD_ACTION_DATA_RESULT);
            BlacklistUtils.importFromContacts(getActivity().getContentResolver(), contactsId);
            return null;
        }

        @Override
        protected void onPreExecute() {
            getActivity().showDialog(BlacklistManagerActivity.PLEASEWAIT_DIALOG);
        }

        @Override
        protected void onProgressUpdate(String... id) {
        }

        @Override
        protected void onPostExecute(Integer size) {
            log("import from contacts end");
            if (!this.isCancelled()) {
                restartQuery();
            }
            ((BlacklistManagerActivity) getActivity())
                    .dismissDialogSafely(BlacklistManagerActivity.PLEASEWAIT_DIALOG);
            getActivity().invalidateOptionsMenu();
        }
    }

    private Handler mMsgHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            log("handleMessage " + msg.what);
            switch(msg.what) {
                case MSG_ID_IMPORT_CONTACTS:
                    Intent intent = new Intent(CONTACTS_ADD_ACTION);
                    intent.setType(Phone.CONTENT_TYPE);
                    try {
                        startActivityForResult(intent, REQUEST_CODE);
                    } catch (ActivityNotFoundException e) {
                        log("start activity exception");
                        e.printStackTrace();
                    }
                    break;
                default:
                    log("handleMessage default");
                    break;
            }
        }
    };

    /**
     * Input dialog.
     */
    public class InputDialog extends DialogFragment {
        private AlertDialog mDialog;

        @Override
        public void onResume() {
            super.onResume();
            log("[InputDialog]onResume");
            mDialog.setTitle(getResources().getString(R.string.add_number));

            Button posBtn = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            posBtn.setText(android.R.string.ok);
            Button negBtn = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            negBtn.setText(android.R.string.cancel);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            log("[InputDialog]onCreateDialog");
            View v = View.inflate(getActivity(), R.layout.blacklist_input_dialog, null);

            final ImageButton importBtn = (ImageButton) v.findViewById(R.id.contact_selector);
            if (importBtn != null) {
                importBtn.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            mMsgHandler.sendEmptyMessage(MSG_ID_IMPORT_CONTACTS);
                            mDialog.dismiss();
                        }
                });
            }

            final EditText numberEdit = (EditText) v.findViewById(R.id.editor);
            if (numberEdit != null) {
                numberEdit.setText("");
                numberEdit.requestFocus();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add_number)
                    .setView(v)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final String number = numberEdit.getText().toString();
                                    if (number.isEmpty()) {
                                        log("insert number return");
                                        return;
                                    }
                                    String phoneNumber = BlacklistUtils.removeSpeicalChars(number);
                                    log("insert number:" + phoneNumber);

                                    dialog.dismiss();
                                    BlacklistUtils.insertNumber(getActivity().getContentResolver(),
                                                                null, phoneNumber);
                                    restartQuery();
                                }
                            });


            mDialog = builder.create();

            mDialog.getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            return mDialog;
        }

        @Override
        public void onDestroyView() {
            log("[InputDialog]onDestroyView");
            super.onDestroyView();
            setTargetFragment(null, 0);
        }
    }

    private void log(String message) {
        Log.d(TAG, "[" + getClass().getSimpleName() + "] " + message);
    }
}
