/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.incallui.ext;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultRCSeCallCardExt;
import com.mediatek.incallui.ext.IRCSeCallCardExt;
import com.mediatek.rcs.incallui.service.CallStatusServicePresenter;
import com.mediatek.xlog.Xlog;

import java.util.HashMap;
import java.util.Map;

@PluginImpl(interfaceName="com.mediatek.incallui.ext.IRCSeCallCardExt")
public class RCSCallCardExtension extends DefaultRCSeCallCardExt {

    private static final String TAG = "RCSCallCardExtension";

    private Context mHostContext;
    private String mHostPackage;
    private Resources mHostResources;

    private Context mContext;
    private HashMap<Integer, TextView> map = new HashMap<Integer, TextView>();
    private CallStatusServicePresenter mCallStatusServicePresenter;
    private RCSInCallUIPlugin mRCSInCallUIPlugin;

    public RCSCallCardExtension (Context context) {
        super();
        mContext = context;
    }

    /**
      * Interface to get name and status textview, inorder to get the string and notify to msg
      *
      * @param context the incallactivity context
      * @param view the callcard view
      */
    @Override
    public void onViewCreated(Context context, View view) {
        Xlog.d(TAG, "onViewCreated.");
        mRCSInCallUIPlugin = RCSInCallUIPlugin.getInstance();
        mHostContext = context;

        mHostPackage = getPackageName();
        mHostResources = getResources();

        TextView username = (TextView) view.findViewById(mHostResources.getIdentifier("name",
                                "id", mHostPackage));
        TextView elapsedTime = (TextView) view.findViewById(mHostResources.getIdentifier("elapsedTime",
                                "id", mHostPackage));     
        map.put(new Integer(1), username);
        map.put(new Integer(3), elapsedTime);

        mCallStatusServicePresenter = CallStatusServicePresenter.getInstance(mContext);
        mCallStatusServicePresenter.onViewSetup(map);
    }

    private String getPackageName() {
        return mRCSInCallUIPlugin.getInCallActivity().getPackageName();
    }

    private Resources getResources() {
        return mRCSInCallUIPlugin.getInCallActivity().getResources();
    }
}
