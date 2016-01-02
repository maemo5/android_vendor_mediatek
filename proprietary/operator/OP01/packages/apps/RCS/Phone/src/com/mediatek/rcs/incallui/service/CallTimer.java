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

package com.mediatek.rcs.incallui.service;

import com.google.common.base.Preconditions;

import android.os.Handler;
import android.os.SystemClock;


public class CallTimer extends Handler {
    private Runnable mInternalCallback;
    private Runnable mCallback;
    private long mLastReportedTime;
    private long mInterval;
    private boolean mRunning;

    public CallTimer(Runnable callback) {
        Preconditions.checkNotNull(callback);

        mInterval = 0;
        mLastReportedTime = 0;
        mRunning = false;
        mCallback = callback;
        mInternalCallback = new CallTimerCallback();
    }

    public boolean startTimeTick(long interval) {
        if (interval <= 0) {
            return false;
        }

        // cancel any previous timer
        stopTimeTick();

        mInterval = interval;
        mLastReportedTime = SystemClock.uptimeMillis();

        mRunning = true;
        periodicUpdateTimer();

        return true;
    }

    public void stopTimeTick() {
        removeCallbacks(mInternalCallback);
        mRunning = false;
    }

    private void periodicUpdateTimer() {
        if (!mRunning) {
            return;
        }

        final long now = SystemClock.uptimeMillis();
        long nextReport = mLastReportedTime + mInterval;
        while (now >= nextReport) {
            nextReport += mInterval;
        }

        postAtTime(mInternalCallback, nextReport);
        mLastReportedTime = nextReport;

        // Run the callback
        mCallback.run();
    }

    private class CallTimerCallback implements Runnable {
        @Override
        public void run() {
            periodicUpdateTimer();
        }
    }
}
