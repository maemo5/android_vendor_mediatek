package com.mediatek.rcs.pam.activities;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.model.MessageContent;

import java.util.List;

public class AccountHistoryAdapter extends BaseAdapter {

    private static final String TAG = Constants.TAG_PREFIX + "AccountHistoryAdapter";

    private Context mContext;
    private List<MessageContent> mMessageList;
    private AccountHistoryMsgUtils mAccountHistoryMsgUtils;

    public AccountHistoryAdapter(Context context, List<MessageContent> messageList,
            ClientQueryActivity.Downloader downloader) {
        mContext = context;
        mMessageList = messageList;
        mAccountHistoryMsgUtils = new AccountHistoryMsgUtils(mContext, downloader);
    }

    @Override
    public int getCount() {
        return mMessageList.size();
    }

    @Override
    public MessageContent getItem(int position) {
        return mMessageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView " + position + ", type " + getItem(position).mediaType
                + ", count " + getCount());

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.account_history_item,
                    parent, false);
        }

        AccountHistoryMsgItemView messageView = (AccountHistoryMsgItemView) convertView;
        messageView.bind(getItem(position), position, mAccountHistoryMsgUtils);
        return convertView;
    }

    public void setMessageList(List<MessageContent> messageList) {
        mMessageList = messageList;
        notifyDataSetChanged();
    }

    public List<MessageContent> getMessageList() {
        return mMessageList;
    }
}
