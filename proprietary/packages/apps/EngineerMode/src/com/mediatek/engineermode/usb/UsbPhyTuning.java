/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.engineermode.usb;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
/**
 * USB PHY Tuning Activity.
 * @author mtk81238
 *
 */
public class UsbPhyTuning extends Activity
        implements android.view.View.OnClickListener {

    private static final String TAG = "EM/UsbPhyTuning";
    private static final String TYPE_USB_TERM_VREF_SEL = "RG_USB20_TERM_VREF_SEL";
    private static final String TYPE_USB_HSTX_SRCTRL = "RG_USB20_HSTX_SRCTRL";
    private static final String TYPE_USB_VRT_VREF_SEL = "RG_USB20_VRT_VREF_SEL";
    private static final String TYPE_USB_INTR_EN = "RG_USB20_INTR_EN";
    private static final String PATH_USB_PHY
            = "/sys/kernel/debug/usb20_phy";
    private static final String PATH_USB_TERM_VREF_SEL
            = PATH_USB_PHY + "/" + TYPE_USB_TERM_VREF_SEL;
    private static final String PATH_USB_HSTX_SRCTRLL
            = PATH_USB_PHY + "/" + TYPE_USB_HSTX_SRCTRL;
    private static final String PATH_USB_VRT_VREF_SEL
            = PATH_USB_PHY + "/" + TYPE_USB_VRT_VREF_SEL;
    private static final String PATH_USB_INTR_EN
            = PATH_USB_PHY + "/" + TYPE_USB_INTR_EN;
    private static final String KEY_EM_USB_VAL = "mediatek.em.usb.value";
    private static final String KEY_EM_USB_TYPE = "mediatek.em.usb.set";
    private static final int MSG_CHECK_SUBMIT_OPERATION = 10;
    private Spinner mSpTermVrefSel = null;
    private Spinner mSpHstxSrctrl = null;
    private Spinner mSpVrtVrefSel = null;
    private Spinner mSpIntrEn = null;
    private Button mBtnTermVrefSel = null;
    private Button mBtnHstxSrctrl = null;
    private Button mBtnVrtVrefSel = null;
    private Button mBtnIntrEn = null;
    private String[] mArrBValStr07 = null;
    private String[] mArrDValStr01 = null;

    /**
     * tell whether usb phy path exist.
     * @return if existed, return true, or false
     */
    public static boolean isUsbPhyExist() {
        return new File(PATH_USB_PHY).exists();
    }

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case MSG_CHECK_SUBMIT_OPERATION:
                handleCheckMsg(msg);
                break;
            default:
                Elog.d(TAG, "Unhandled msg:" + msg.what);
                break;
            }
        }
    };

    private void handleCheckMsg(Message msg) {
        Button btn = (Button) msg.obj;
        boolean ret = checkSubmitResult(btn);
        if (ret) {
            Toast.makeText(this, "Execute success", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Execute fail", Toast.LENGTH_SHORT).show();
        }
        btn.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usb_phy_tuning);
        mArrBValStr07 = getResources().getStringArray(R.array.usb_phy_tuning_val_0_7b);
        mArrDValStr01 = getResources().getStringArray(R.array.usb_phy_tuning_val_0_1d);
        mSpTermVrefSel = (Spinner) findViewById(R.id.usb_phy_term_vref_sel_sp);
        fillSelectSpinner(mSpTermVrefSel, mArrBValStr07, PATH_USB_TERM_VREF_SEL);
        mSpHstxSrctrl = (Spinner) findViewById(R.id.usb_phy_hstx_srctrl_sp);
        fillSelectSpinner(mSpHstxSrctrl, mArrBValStr07, PATH_USB_HSTX_SRCTRLL);
        mSpVrtVrefSel = (Spinner) findViewById(R.id.usb_phy_vrt_vref_sel_sp);
        fillSelectSpinner(mSpVrtVrefSel, mArrBValStr07, PATH_USB_VRT_VREF_SEL);
        mSpIntrEn = (Spinner) findViewById(R.id.usb_phy_intr_en_sp);
        fillSelectSpinner(mSpIntrEn, mArrDValStr01, PATH_USB_INTR_EN);

        mBtnTermVrefSel = (Button) findViewById(R.id.usb_phy_term_vref_sel_btn);
        mBtnTermVrefSel.setOnClickListener(this);
        mBtnHstxSrctrl = (Button) findViewById(R.id.usb_phy_hstx_srctrl_btn);
        mBtnHstxSrctrl.setOnClickListener(this);
        mBtnVrtVrefSel = (Button) findViewById(R.id.usb_phy_vrt_vref_sel_btn);
        mBtnVrtVrefSel.setOnClickListener(this);
        mBtnIntrEn = (Button) findViewById(R.id.usb_phy_intr_en_btn);
        mBtnIntrEn.setOnClickListener(this);
    }

    private void fillSpinnerItems(Spinner spinner, String[] itemArr) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                itemArr);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void fillSelectSpinner(Spinner spinner, String[] itemArr, String path) {
        fillSpinnerItems(spinner, itemArr);
        String content = readFileContent(path);
        if (content != null) {
            content = content.trim();
        }
        int index = getIdxInStrArr(itemArr, content);
        if (index >= 0) {
            spinner.setSelection(index);
        }
    }

    private String readFileContent(String path) {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        char[] buffer = new char[1024];
        try {
            reader = new BufferedReader(new FileReader(path));
            while (true) {
                int ret = reader.read(buffer, 0, buffer.length);
                if (ret <= 0) {
                    break;
                }
                builder.append(buffer, 0, ret);
            }
        } catch (IOException e) {
            Elog.e(TAG, "readFileContent IOException:" + e.getMessage());
        }
        return builder.toString();
    }

    private int getIdxInStrArr(String[] array, String str) {
        int index = -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(str)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void onClickButton(Button button) {
        String type = null;
        String value = null;
        if (button == mBtnTermVrefSel) {
            type = TYPE_USB_TERM_VREF_SEL;
            value = mSpTermVrefSel.getSelectedItem().toString();
        } else if (button == mBtnHstxSrctrl) {
            type = TYPE_USB_HSTX_SRCTRL;
            value = mSpHstxSrctrl.getSelectedItem().toString();
        } else if (button == mBtnVrtVrefSel) {
            type = TYPE_USB_VRT_VREF_SEL;
            value = mSpVrtVrefSel.getSelectedItem().toString();
        } else if (button == mBtnIntrEn) {
            type = TYPE_USB_INTR_EN;
            value = mSpIntrEn.getSelectedItem().toString();
        } else {
            Elog.d(TAG, "Unhandled button click:" + button);
        }
        if (type != null && value != null) {
            submitSetting(button, type, value);
        }
    }

    private void submitSetting(Button btn, String type, String value) {
        btn.setEnabled(false);
        SystemProperties.set(KEY_EM_USB_VAL, value);
        SystemProperties.set(KEY_EM_USB_TYPE, type);
        Message msg = Message.obtain();
        msg.what = MSG_CHECK_SUBMIT_OPERATION;
        msg.obj = btn;
        mMainHandler.sendMessageDelayed(msg, 100);
    }

    private boolean checkSubmitResult(Button button) {
        boolean result = false;
        Spinner spinner = null;
        String path = null;
        String[] array = null;
        if (button == mBtnTermVrefSel) {
            path = PATH_USB_TERM_VREF_SEL;
            spinner = mSpTermVrefSel;
            array = mArrBValStr07;
        } else if (button == mBtnHstxSrctrl) {
            path = PATH_USB_HSTX_SRCTRLL;
            spinner = mSpHstxSrctrl;
            array = mArrBValStr07;
        } else if (button == mBtnVrtVrefSel) {
            path = PATH_USB_VRT_VREF_SEL;
            spinner = mSpVrtVrefSel;
            array = mArrBValStr07;
        } else if (button == mBtnIntrEn) {
            path = PATH_USB_INTR_EN;
            spinner = mSpIntrEn;
            array = mArrDValStr01;
        } else {
            Elog.d(TAG, "checkSubmitResult Unknown button" + button);
        }
        if (spinner != null && path != null && array != null) {
            String content = readFileContent(path);
            if (content != null) {
                content = content.trim();
            }
            String selectVal = spinner.getSelectedItem().toString();
            if (selectVal.equals(content)) {
                result = true;
            } else {
                // execute fail, reset spinner consistent with system value
                int index = getIdxInStrArr(array, content);
                if (index >= 0) {
                    spinner.setSelection(index);
                }
            }
        }
        return result;
    }

    @Override
    public void onClick(View view) {
        if (view instanceof Button) {
            Button btn = (Button) view;
            onClickButton(btn);
        }
    }
}
