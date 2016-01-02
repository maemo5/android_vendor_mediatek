package com.mediatek.rcs.message.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.mediatek.mms.ipmessage.IpContact;
import com.mediatek.mms.ipmessage.IpContactCallback;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

public class RcsContact extends IpContact {
    private static String TAG = "RcseContact";
    /// M: add for ip message
//    private long mThreadId = 0;
//    private BitmapDrawable mIpMessageAvatar;
//    private boolean mIpMessageAvatarFetched = false;
//    public static final int IPMSG_AVATAR_FETCH_TIME_OUT = 200;
//    private Bitmap mIpMessageAvatarBitmap;
//
//    private BitmapDrawable mAvatar;

    private Context mContext;
    public IpContactCallback mCallback;
    
    @Override
    public void onIpInit(Context context, IpContactCallback callback) {
        mContext = context;
        mCallback = callback;
        super.onIpInit(context, callback);
    }

    public String onIpUpdateContact(String number, String name) {
        return super.onIpUpdateContact(number, name);
    }

    @Override
    public Drawable onIpGetAvatar(Drawable defaultValue, final long threadId, String number) {
        boolean isGroup = RcsMessageUtils.isGroupchat(threadId);
        if (isGroup) {
            return RcsMessageUtils.getGroupDrawable(threadId);
        } 
        return super.onIpGetAvatar(defaultValue, threadId, number);
    }

    @Override
    public String onIpGetNumber(String number) {
        return super.onIpGetNumber(number);
    }

    @Override
    public boolean onIpIsGroup(String number) {
        return super.onIpIsGroup(number);
    }
    
    @Override
    public void invalidateGroupContact(String number) {
        super.invalidateGroupContact(number);
    }

    public String getNumber() {
        return mCallback.getContactNumber();
    }
    
    public String getName() {
        return mCallback.getContactName();
    }

    public boolean isExistInDatabase() {
        return mCallback.isContactExistsInDatabase();
    }

    public BitmapDrawable getAvatar() {
        return mCallback.getAvatar();
    }
}
