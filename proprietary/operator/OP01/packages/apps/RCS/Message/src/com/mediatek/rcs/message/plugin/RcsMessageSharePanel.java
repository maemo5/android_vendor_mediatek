/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcs.message.plugin;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.mms.ipmessage.IpSharePanel;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

public class RcsMessageSharePanel extends IpSharePanel {
    private Context mContext;
    private boolean mDisplayBurned;
    private String[] mLabelArray;
    private int[]    mIconArray;
    private RcsComposeActivity mComposer;
    
    public static final int ACTION_SHARE = 0;
    public static final String SHARE_ACTION = "shareAction";
    public static final int IPMSG_TAKE_PHOTO        = 100;
    public static final int IPMSG_RECORD_VIDEO      = 101;
    public static final int IPMSG_RECORD_AUDIO      = 102;
    public static final int IPMSG_CHOOSE_PHOTO      = 104;
    public static final int IPMSG_CHOOSE_VIDEO      = 105;
    public static final int IPMSG_CHOOSE_AUDIO      = 106;
    public static final int IPMSG_SHARE_CONTACT     = 108;
    public static final int IPMSG_SHARE_CALENDAR    = 109;
    public static final int IPMSG_SHARE_POSITION    = 110;


    private final int[] IP_MESSAGE_ACTIONS = {
        IPMSG_TAKE_PHOTO, IPMSG_RECORD_VIDEO, IPMSG_RECORD_AUDIO, IPMSG_SHARE_CONTACT,
        IPMSG_CHOOSE_PHOTO, IPMSG_CHOOSE_VIDEO, IPMSG_CHOOSE_AUDIO, IPMSG_SHARE_POSITION
        };
    
    private final int[] IP_BURNED_MESSAGE_ACTIONS = {
        IPMSG_TAKE_PHOTO, IPMSG_RECORD_VIDEO, IPMSG_RECORD_AUDIO,
        IPMSG_CHOOSE_PHOTO, IPMSG_CHOOSE_VIDEO, IPMSG_CHOOSE_AUDIO, 
    };
    
    private final int[] IP_MSG_SHARE_DRAWABLE_IDS = {
            R.drawable.ipmsg_take_a_photo,
            R.drawable.ipmsg_record_a_video,
            R.drawable.ipmsg_record_an_audio,
            R.drawable.ipmsg_share_contact,
            R.drawable.ipmsg_choose_a_photo,
            R.drawable.ipmsg_choose_a_video,
            R.drawable.ipmsg_choose_an_audio,
            R.drawable.ipmsg_share_location
    };
    
    private final int[] IP_MSG_BURNED_SHARE_DRAWABLE_IDS = {
            R.drawable.ipmsg_take_a_photo,
            R.drawable.ipmsg_record_a_video,
            R.drawable.ipmsg_record_an_audio,
            R.drawable.ipmsg_choose_a_photo,
            R.drawable.ipmsg_choose_a_video,
            R.drawable.ipmsg_choose_an_audio,
    };
    
    /**
     * M: 
     * @param context
     * @return boolean
     */
    
    public RcsMessageSharePanel(Context context) {
        mContext = context;
    }
    
    public RcsMessageSharePanel(Context context, RcsComposeActivity composer) {
        mContext = context;
        mComposer = composer;
    }
    
    public void setComposer(RcsComposeActivity composer) {
        mComposer = composer;
    }

    public String[]  getIpLableArray(Context context) {
        if (!isShowRcsPannel()) {
            return null;
        }
        if (RcsMessageConfig.isEditingDisplayBurnedMsg()) {
            mLabelArray =  mContext.getResources().getStringArray(R.array.burned_share_string_array);
        } else {
            mLabelArray =  mContext.getResources().getStringArray(R.array.share_string_array);
        }
        return mLabelArray;
    }
    
    public int[] getIpIconArray(Context context) {
        if (!isShowRcsPannel()) {
            return null;
        }
        if (RcsMessageConfig.isEditingDisplayBurnedMsg()) {
            mIconArray = IP_MSG_BURNED_SHARE_DRAWABLE_IDS;
        } else {
            mIconArray = IP_MSG_SHARE_DRAWABLE_IDS;
        }
        return mIconArray;
    }

    public Drawable[] getIconDrableArray() {
        if (!isShowRcsPannel()) {
            return null;
        }
        Resources resource = mContext.getResources();
        int[] resourceIds;
        if (RcsMessageConfig.isEditingDisplayBurnedMsg()) {
            resourceIds = IP_MSG_BURNED_SHARE_DRAWABLE_IDS;
        } else {
            resourceIds = IP_MSG_SHARE_DRAWABLE_IDS;
        }
        int length = resourceIds.length;
        Drawable[] drawables = new Drawable[length];
        for (int index = 0; index < length; index ++) {
            drawables[index] = resource.getDrawable(resourceIds[index]);
        }
        return drawables;
    }
    
    public boolean getIpView(int position, TextView text, ImageView img) {
        if (!isShowRcsPannel()) {
            return false;
        }
        Resources resource = mContext.getResources();
        int number = 0;
        if (mIconArray != null) {
            number = mIconArray.length;
        }
        if (position < number) {
            text.setText(mLabelArray[position]);
            img.setBackgroundDrawable(resource.getDrawable(mIconArray[position]));
            return true;
        }
        return false;
    }

    private int[] getActionArray() {
        if (!isShowRcsPannel()) {
            return null;
        }
        if (RcsMessageConfig.isEditingDisplayBurnedMsg()) {
            return IP_BURNED_MESSAGE_ACTIONS;
        } else {
            return IP_MESSAGE_ACTIONS;
        }
    }
    
    public boolean onIpGridViewItemClick(int actionPosition, Bundle bundle) {
        if (!isShowRcsPannel()) {
            return false;
        }
        int[] shareid = getActionArray();
        if (actionPosition < shareid.length && bundle != null) {
            bundle.putInt(SHARE_ACTION, shareid[actionPosition]);
            return true;
        }
        return false;
    }
    
    private boolean isShowRcsPannel() {
        /*
        //if have mms draft, need go on edit.
        if (mComposer != null && mComposer.isMms()) {
            return false;
        }
        //group chat always show rcs panel if show
        if (mComposer != null && mComposer.isGroupChat()) {
            return true;
        }
        */
        if (mComposer != null && mComposer.isRcsMode()) {
            return true;
        } else {
            return false;
        }
        /*
        //not provisioning successfull, back to sms/mms mode
        if (!RCSServiceManager.getInstance().isActivated()) {
            return false;
        }
        
        //mainsubid show rcs panel while the other subid show normal panel
        int subId =  1;//RcsMessageUtils.getSendSubId();
        int mainSubId = 1; //(int)RcsMessageUtils.get34GCapabilitySubId();
        if (subId == mainSubId) {
            // main sub id
            return true;
        } else {
            return false;
        }
        */
    }
}

