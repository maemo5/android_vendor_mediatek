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

package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.common.MPlugin;
import com.mediatek.common.PluginImpl;
import com.mediatek.common.telephony.ILteDataOnlyController;
import com.mediatek.mms.ext.DefaultMmsUtilsExt;
import com.mediatek.op09.plugin.R;
import com.mediatek.xlog.Xlog;

/**
 * M: OP09 mms utils.
 */
@PluginImpl(interfaceName = "com.mediatek.mms.ext.IMmsUtilsExt")
public class Op09MmsUtilsExt extends DefaultMmsUtilsExt {
    private static final String TAG = "Mms/OP09MmsUtilsExt";

    /**
     * M: Constructor.
     * @param context the context.
     */
    public Op09MmsUtilsExt(Context context) {
        super(context);
    }

    @Override
    public String formatDateAndTimeStampString(Context context, long msgDate, long msgDateSent,
            boolean fullFormat, String formatStr) {
        if (msgDateSent > 0) {
            return MessageUtils.formatDateOrTimeStampStringWithSystemSetting(this, msgDateSent,
                fullFormat);
        } else if (msgDate > 0) {
            return MessageUtils.formatDateOrTimeStampStringWithSystemSetting(this, msgDate,
                fullFormat);
        } else {
            return formatStr;
        }
    }

    @Override
    public void showSimTypeBySubId(Context context, int subId, TextView textView) {
        Drawable simTypeDraw = null;
        Xlog.d(TAG, "showSimTypeBySimId");
        SubscriptionInfo simInfo = SubscriptionManager.from(context).getSubscriptionInfo(subId);
        if (simInfo != null) {
            /// FIXME
//            int sysResId = simInfo.simIconRes[3];
            int sysResId = 0;
            if (sysResId > 0) {
                /// FIXME
//                simTypeDraw = getResources().getDrawable(simInfo.simIconRes[3]);
            } else {
                Xlog.e(TAG, "[showSimTypeBySubId], get icon res from subInfo.simIconRes[3]"
                    + " failed. res = " + sysResId);
                simTypeDraw = getResources().getDrawable(R.drawable.sim_light_not_activated);
            }
        } else {
            simTypeDraw = getResources().getDrawable(R.drawable.sim_light_not_activated);
        }

        if (textView != null) {
            String text = textView.getText().toString().trim();
            textView.setText("  " + text + "  ");
            textView.setBackgroundDrawable(simTypeDraw);
        }
    }

    /**
     * M: get resource for send button with the sub id.
     *
     * @param context
     *            the Context.
     * @param defaultSubId
     *            the default sim's sub id.
     * @return the resource for the button.
     *            [0][0]:The button's draw; [0][1]:SlotId; [0][2]:The siminfo.color.
     */
    public Object[][] getSendButtonResourceIdBySubId(Context context, int defaultSubId) {
        /// M: [0][0]:The button's draw; [0][1]:SlotId; [0][2]:The siminfo.color
        Object[][] resourceIds = new Object[2][3];
        SubscriptionInfo defaultSimInfo = SubscriptionManager.from(context).getSubscriptionInfo(
            defaultSubId);
        int defaultSlotId = defaultSimInfo.getSimSlotIndex();
        if (defaultSlotId == 0) {
            resourceIds[0][0] = getResources().getDrawable(
                MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_C_BIG[defaultSimInfo.getIconTint()]);
            resourceIds[0][1] = 0;
            resourceIds[0][2] = defaultSimInfo.getIconTint();
            SubscriptionInfo secondSimInfo = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoForSimSlotIndex(1);
            if (secondSimInfo == null) {
                resourceIds[1][0] = getResources().getDrawable(
                    R.drawable.ct_send_2_small_blue_disable);
                resourceIds[1][2] = -1;
            } else {
                resourceIds[1][0] = getResources().getDrawable(
                    MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_G_SMALL[secondSimInfo
                            .getIconTint()]);
                resourceIds[1][2] = secondSimInfo.getIconTint();
            }
            resourceIds[1][1] = 1;
        } else if (defaultSlotId == 1) {
            resourceIds[0][0] = getResources().getDrawable(
                MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_G_BIG[defaultSimInfo.getIconTint()]);
            resourceIds[0][1] = 1;
            resourceIds[0][2] = defaultSimInfo.getIconTint();
            SubscriptionInfo secondSimInfo = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoForSimSlotIndex(0);
            if (secondSimInfo == null) {
                resourceIds[1][0] = getResources().getDrawable(
                    R.drawable.ct_send_1_small_orange_disable);
                resourceIds[1][2] = -1;
            } else {
                resourceIds[1][0] = getResources().getDrawable(
                    MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_C_SMALL[secondSimInfo
                            .getIconTint()]);
                resourceIds[1][2] = secondSimInfo.getIconTint();
            }
            resourceIds[1][1] = 0;
        }
        return resourceIds;
    }

    /**
     * M: get resource for send button with sub id.
     *
     * @param context
     *            the Context.
     * @param defaultSubId
     *            the default sim's subId.
     * @param enable
     *            true: the button can click; false: the button cannot be clicked.
     * @return the resource for the button.
     *            [0][0]:The button's draw; [0][1]:SlotId; [0][2]:The siminfo.color.
     */
    public Object[][] getSendButtonResourceIdBySubId(Context context, int defaultSubId,
            boolean enable) {
        Xlog.d(TAG, "getSendButtonResourceIdBySlotId: defaultSlotId:" + defaultSubId + " enable:"
            + enable);
        Object[][] resourceIds = new Object[2][3];
        int resBigId = 0;
        int resSmallId = 0;
        SubscriptionInfo defaultSimInfo = SubscriptionManager.from(context).getSubscriptionInfo(
            defaultSubId);
        int defaultSlotId = defaultSimInfo.getSimSlotIndex();
        if (defaultSlotId == 0) {
            if (enable) {
                resBigId = MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_C_BIG[defaultSimInfo
                        .getIconTint()];
            } else {
                resBigId = MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_C_BIG[defaultSimInfo
                        .getIconTint()];
            }
            resourceIds[0][0] = getResources().getDrawable(resBigId);
            resourceIds[0][1] = 0;
            resourceIds[0][2] = defaultSimInfo.getIconTint();
            SubscriptionInfo secondSimInfo = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoForSimSlotIndex(1);
            if (secondSimInfo == null) {
                resourceIds[1][0] = getResources().getDrawable(
                    R.drawable.ct_send_2_small_blue_disable);
                resourceIds[1][2] = -1;
            } else {
                if (enable) {
                    resSmallId = MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_G_SMALL[
                            secondSimInfo.getIconTint()];
                } else {
                    resSmallId = MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_G_SMALL[
                            secondSimInfo.getIconTint()];
                }
                resourceIds[1][0] = getResources().getDrawable(resSmallId);
                resourceIds[1][2] = secondSimInfo.getIconTint();
            }
            resourceIds[1][1] = 1;
        } else if (defaultSlotId == 1) {
            if (enable) {
                resBigId = MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_G_BIG[defaultSimInfo
                        .getIconTint()];
            } else {
                resBigId = MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_G_BIG[defaultSimInfo
                        .getIconTint()];
            }
            resourceIds[0][0] = getResources().getDrawable(resBigId);
            resourceIds[0][1] = 1;
            resourceIds[0][2] = defaultSimInfo.getIconTint();
            SubscriptionInfo secondSimInfo = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoForSimSlotIndex(0);
            if (secondSimInfo == null) {
                resourceIds[1][0] = getResources().getDrawable(
                    R.drawable.ct_send_1_small_orange_disable);
                resourceIds[1][2] = -1;
            } else {
                if (enable) {
                    resSmallId = MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_C_SMALL[
                            secondSimInfo.getIconTint()];
                } else {
                    resSmallId = MessageUtils.SEND_BUTTON_DRAWABLE_RESOURCE_ID_C_SMALL[
                            secondSimInfo.getIconTint()];
                }
                resourceIds[1][0] = getResources().getDrawable(resSmallId);
                resourceIds[1][2] = secondSimInfo.getIconTint();
            }
            resourceIds[1][1] = 0;
        }
        return resourceIds;
    }

    @Override
    public boolean allowSafeDraft(final Activity activity, boolean deviceStorageIsFull,
            boolean isNofityUser, int toastType) {
        Xlog.d(TAG, "allowSafeDraft: deviceStorageIsFull:" + deviceStorageIsFull + " isNotifyUser:"
            + isNofityUser);
        if (activity == null || !deviceStorageIsFull) {
            return true;
        }
        if (deviceStorageIsFull && !isNofityUser) {
            return false;
        }
        if (deviceStorageIsFull && isNofityUser) {
            final String str;
            switch (toastType) {
                case TOAST_TYPE_FOR_SAVE_DRAFT:
                    str = getResources().getString(R.string.memory_full_cannot_save);
                    break;
                case TOAST_TYPE_FOR_SEND_MSG:
                    str = getResources().getString(R.string.memory_full_cannot_send);
                    break;
                case TOAST_TYPE_FOR_ATTACH:
                    str = getResources().getString(R.string.memory_full_cannot_attach);
                    break;
                case TOAST_TYPE_FOR_DOWNLOAD_MMS:
                    str = getResources().getString(R.string.memory_full_cannot_download_mms);
                    break;
                default:
                    str = "";
                    break;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(), str, Toast.LENGTH_LONG).show();
                }
            });
        }
        return false;
    }

    @Override
    public String formatDateTime(Context context, long time, int formatFlags) {
        return MessageUtils.formatDateTime(context, time, formatFlags);
    }

    private static final String TEXT_SIZE = "message_font_size";
    private static final float DEFAULT_TEXT_SIZE = 18;
    private static final float MIN_TEXT_SIZE = 10;
    private static final float MAX_TEXT_SIZE = 32;

    /**
     * M: Get text size.
     * @param context the Context.
     * @return the text size.
     */
    public static float getTextSize(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        float size = sp.getFloat(TEXT_SIZE, DEFAULT_TEXT_SIZE);
        Xlog.v(TAG, "getTextSize = " + size);
        if (size < MIN_TEXT_SIZE) {
            size = MIN_TEXT_SIZE;
        } else if (size > MAX_TEXT_SIZE) {
            size = MAX_TEXT_SIZE;
        }
        return size;
    }

    /**
     * M: set text size.
     * @param context the context.
     * @param size the text size.
     */
    public static void setTextSize(Context context, float size) {
        float textSize;
        Xlog.v(TAG, "setTextSize = " + size);

        if (size < MIN_TEXT_SIZE) {
            textSize = MIN_TEXT_SIZE;
        } else if (size > MAX_TEXT_SIZE) {
            textSize = MAX_TEXT_SIZE;
        } else {
            textSize = size;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(TEXT_SIZE, textSize);
        editor.commit();
    }

    /**
     * M: Juduge the addres is dialable for ct.
     * @param address the phone number.
     * @return true: is phone number; false: is not phone number.
     */
    private boolean isDialableForCT(String address) {
        if (address == null || address.length() <= 0) {
            return false;
        }
        /// M: Judge the first character is dialable.
        char firstC = address.charAt(0);
        if (!(firstC == '+' || (firstC >= '0' && firstC <= '9'))) {
            return false;
        }
        for (int i = 1, count = address.length(); i < count; i++) {
            char c = address.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isWellFormedSmsAddress(String address) {
        if (!isDialableForCT(address)) {
            return false;
        }
        String networkPortion =
                PhoneNumberUtils.extractNetworkPortion(address);
        return (!(networkPortion.equals("+")
                  || TextUtils.isEmpty(networkPortion)))
               && isDialableForCT(networkPortion);
    }

    /**
     * M: get icon for button which can be clicked.
     * @param slotId the sim's slotId.
     * @param smallIcon true: get small icon. false: get big small icon.
     * @param color the sim's color.
     * @return the icon drawable.
     */
    public Drawable getActivatedButtonIconBySlotId(int slotId, boolean smallIcon, int color) {
        Drawable drawable = null;

        switch (slotId) {
            case 0:
                if (smallIcon) {
                    if (color > -1) {
                        drawable = getResources().getDrawable(
                            MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_C_SMALL[color]);
                    } else {
                        drawable = getResources().getDrawable(
                            R.drawable.ct_send_1_small_orange_disable);
                    }
                } else {
                    if (color > -1) {
                        drawable = getResources().getDrawable(
                            MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_C_BIG[color]);
                    } else {
                        drawable = getResources().getDrawable(
                            R.drawable.ct_send_1_big_orange_disable);
                    }
                }
                break;
            case 1:
                if (smallIcon) {
                    if (color > -1) {
                        drawable = getResources().getDrawable(
                            MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_G_SMALL[color]);
                    } else {
                        drawable = getResources().getDrawable(
                            R.drawable.ct_send_2_small_blue_disable);
                    }
                } else {
                    if (color > -1) {
                        drawable = getResources().getDrawable(
                            MessageUtils.SEND_BUTTON_ACTIVIATE_RESOURCE_ID_G_BIG[color]);
                    } else {
                        drawable = getResources()
                                .getDrawable(R.drawable.ct_send_2_big_blue_disable);
                    }
                }
                break;
            default:
                break;
        }
        return drawable;
    }

    @Override
    public void setIntentDateForMassTextMessage(Intent intent, long groupId) {
        if (intent == null) {
            return;
        }
        intent.putExtra(MASS_TEXT_MESSAGE_GROUP_ID, groupId < 0 ? groupId : -1L);
    }

    @Override
    public long getGroupIdFromIntent(Intent intent) {
        if (intent == null) {
            return -1L;
        }
        return intent.getLongExtra(MASS_TEXT_MESSAGE_GROUP_ID, -1L);
    }

    @Override
    public Cursor getReportItemsForMassSMS(Context context, String[] projection, long groupId) {
        if (context == null || (groupId >= 0) || projection == null || projection.length < 1) {
            return null;
        }
        Xlog.d(TAG, "getReportItemsForMassSMS, groupId:" + groupId);
        return context.getContentResolver().query(Sms.CONTENT_URI, projection, "ipmsg_id = ?",
            new String[] {groupId + ""}, null);
    }

    @Override
    public boolean isCDMAType(Context context, int subId) {
        return MessageUtils.isCDMAType(context, subId);
    }

    @Override
    public SubscriptionInfo getSubInfoBySubId(Context ctx, int subId) {
        return MessageUtils.getSimInfoBySubId(ctx, subId);
    }

    @Override
    public SubscriptionInfo getFirstSimInfoBySlotId(Context ctx, int slotId) {
        return MessageUtils.getFirstSimInfoBySlotId(ctx, slotId);
    }

    @Override
    public boolean is4GDataOnly(Context context, int subId) {
        Xlog.d(TAG, "[is4GDataOnly]");
        if (context == null) {
            return false;
        }
        boolean result = false;
        ILteDataOnlyController ldoc = (ILteDataOnlyController) MPlugin.createInstance(
            ILteDataOnlyController.class.getName(), context);
        if (ldoc == null) {
            result = false;
        }
        if (ldoc.checkPermission(subId)) {
            result = false;
        } else {
            result = true;
        }
        Xlog.d(TAG, "[is4GDataOnly],result:" + result);
        return result;
    }
}
