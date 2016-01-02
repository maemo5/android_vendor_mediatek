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

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/mount.h>
#include <sys/statfs.h>
#include <dirent.h>
#include <linux/input.h>
#include <math.h>

#include "common.h"
#include "miniui.h"
#include "ftm.h"

#ifdef FEATURE_FTM_HALL

extern sp_ata_data return_data;

/******************************************************************************
 * MACRO
 *****************************************************************************/
#define TAG "[HALL] "
#define mod_to_hall_data(p) (struct hall_data*)((char*)(p) + sizeof(struct ftm_module))
#define	HALL_NAME "/sys/class/switch/hall/state"
/******************************************************************************
 * Structure
 *****************************************************************************/
enum {
    ITEM_PASS,
    ITEM_FAIL,
};
/*---------------------------------------------------------------------------*/
static item_t hall_items[] = {
    item(ITEM_PASS,   uistr_pass),
    item(ITEM_FAIL,   uistr_fail),
    item(-1, NULL),
};
/*---------------------------------------------------------------------------*/
struct hall_data
{
    struct ftm_module *mod;

    /*common for each factory mode*/
    char  info[1024];
    int   info_len;

    text_t    title;
    text_t    text;
    text_t    left_btn;
    text_t    center_btn;
    text_t    right_btn;
    
    pthread_t update_thd;
    bool  exit_thd;

    //struct textview tv;
    struct itemview *iv;
};

static bool thread_exit = false;
static pthread_t ata_thd;
static bool ata_thread_exit = false;
static bool ata_test_pass = false;

/*---------------------------------------------------------------------------*/
static int get_hall_status(void)
{
    FILE *in;
    char buffer[16];
    int ret = 0;

    in = fopen(HALL_NAME, "r");
    if(in != NULL){
        if(fread(buffer, 1, 1, in) == 1){
            if(buffer[0] == '1'){
                ret = 1;
            }else if(buffer[0] == '0'){
                ret = 2;
            }
        }
        fclose(in);
    }
    return ret;
}
/*---------------------------------------------------------------------------*/
#define HALL_TEST_SWITCH_CNT 2
static void *hall_update_ata_thread(void *priv)
{
    int hall_switch_cnt = 0;
    int last_hall_status = 1;//Far
    int last_bl_status = 0;
    int fd, size;

    LOGD(TAG "%s: Start\n", __FUNCTION__);

    while(1){
        if(get_is_ata() == 0){
            break;
        }
        if(ata_thread_exit == true){
            break;
        }
        if(hall_switch_cnt > HALL_TEST_SWITCH_CNT){
            ata_test_pass = true;
            if(last_bl_status == 1){
            /* Vanzo:maxiaojun on: Wed, 12 Mar 2014 21:29:26 +0800
             */

                if((fd = open("/sys/devices/platform/leds-mt65xx/leds/lcd-backlight/brightness", O_WRONLY ))<0)
                {
                    perror("open lighton failed:");
                    exit(1);
                }

                if((size = write( fd, "255", 3)) < 0){
                    perror("write 255 to brightness on failed");
                    exit(1);
                }
                if( close(fd) < 0 )
                {
                    perror("close:");
                    exit(1);
                }
            // End of Vanzo:maxiaojun
                last_bl_status = 0;
            }else{
            /* Vanzo:maxiaojun on: Wed, 12 Mar 2014 21:29:26 +0800
             */

                if((fd = open("/sys/devices/platform/leds-mt65xx/leds/lcd-backlight/brightness", O_WRONLY ))<0)
                {
                    perror("open lighton failed:");
                    exit(1);
                }

                if((size = write( fd, "100", 3)) < 0){
                    perror("write 255 to brightness on failed");
                    exit(1);
                }
                if( close(fd) < 0 )
                {
                    perror("close:");
                    exit(1);
                }
            // End of Vanzo:maxiaojun
                last_bl_status = 1;
            }
        }else{
            int hall_status = get_hall_status();
            if(hall_status != 0 && hall_status != last_hall_status){
                hall_switch_cnt ++;
                last_hall_status = hall_status;
            }
        }
        usleep(500000);
    }

    pthread_exit(NULL);
    return NULL;
}
/*---------------------------------------------------------------------------*/
static void *hall_update_iv_thread(void *priv)
{
    struct hall_data *dat = (struct hall_data *)priv; 
    struct itemview *iv = dat->iv;    
    int err = 0, len = 0;
    int hall_status = -1;
    int last_hall_status = 1;//Far
    int hall_switch_cnt = 0;

    LOGD(TAG "%s: Start\n", __FUNCTION__);
        
    while (1) {
        
        if (dat->exit_thd){
            LOGE("dat -> exit_thd\n");
            break;
        }    

        len = dat->info_len;
        //dat->text.color = COLOR_GREEN;
        if(hall_status == 0){
            len += snprintf(dat->info+len, sizeof(dat->info)-len, "\n%s\n", uistr_hall_nosupport);
        }else if(hall_status == 1){
            len += snprintf(dat->info+len, sizeof(dat->info)-len, "\n%s\n", uistr_hall_far);
        }else if(hall_status == 2){
            len += snprintf(dat->info+len, sizeof(dat->info)-len, "\n%s\n", uistr_hall_near);
        }

		if(len < 0)
		{
		   LOGE(TAG "%s: snprintf error \n", __FUNCTION__); 
		   len = 0;
		}

        iv->set_text(iv, &dat->text);
        iv->redraw(iv);
        if(ata_test_pass == true && get_is_ata() == 1)
        { 
            LOGD(TAG "%s:%d, passed \n", __FUNCTION__, __LINE__);
            dat->mod->test_result = FTM_TEST_PASS;
            thread_exit = true;  
            break;
        }
        usleep(50000);
        hall_status = get_hall_status();
        if(hall_status != 0 && last_hall_status != hall_status){
            last_hall_status = hall_status;
            hall_switch_cnt ++;
        }
        LOGD(TAG "%s:%d, hall_switch_cnt:%d\n", __FUNCTION__, __LINE__, hall_switch_cnt);
        if(hall_switch_cnt > HALL_TEST_SWITCH_CNT){
            ata_test_pass = true;
        }
    }
    LOGD(TAG "%s: Exit\n", __FUNCTION__);    
    pthread_exit(NULL);
    
    return NULL;
}

int hall_entry(struct ftm_param *param, void *priv)
{
    char *ptr;
    int chosen;
    struct hall_data *dat = (struct hall_data *)priv;
    struct textview *tv;
    struct itemview *iv;
    struct statfs stat;
    int err;

    LOGD(TAG "%s\n", __FUNCTION__);

    init_text(&dat->title, param->name, COLOR_YELLOW);
    init_text(&dat->text, &dat->info[0], COLOR_YELLOW);
    init_text(&dat->left_btn, uistr_info_sensor_fail, COLOR_YELLOW);
    init_text(&dat->center_btn, uistr_info_sensor_pass, COLOR_YELLOW);
    init_text(&dat->right_btn, uistr_info_sensor_back, COLOR_YELLOW);
       
    snprintf(dat->info, sizeof(dat->info), uistr_hall_text);
    dat->info_len = strlen(dat->info);
    dat->exit_thd = false;  


    if (!dat->iv) {
        iv = ui_new_itemview();
        if (!iv) {
            LOGD(TAG "No memory");
            return -1;
        }
        dat->iv = iv;
    }
    iv = dat->iv;
    iv->set_title(iv, &dat->title);
    iv->set_items(iv, hall_items, 0);
    iv->set_text(iv, &dat->text);
    
    ata_thread_exit = true;
    pthread_create(&dat->update_thd, NULL, hall_update_iv_thread, priv);
    do {
        if(get_is_ata() != 1){
          chosen = iv->run(iv, &thread_exit);
          switch (chosen) {
            case ITEM_PASS:
            case ITEM_FAIL:
                if (chosen == ITEM_PASS) {
                    dat->mod->test_result = FTM_TEST_PASS;
                } else if (chosen == ITEM_FAIL) {
                    dat->mod->test_result = FTM_TEST_FAIL;
                }           
                thread_exit = true;            
                break;
            }
        }
        iv->redraw(iv);
        if (thread_exit) {
            dat->exit_thd = true;
            break;
        }        
    } while (1);
    pthread_join(dat->update_thd, NULL);

    return 0;
}
/*---------------------------------------------------------------------------*/
int hall_init(void)
{
    int ret = 0;
    struct ftm_module *mod;
    struct hall_data *dat;

    LOGD(TAG "%s\n", __FUNCTION__);
    
    mod = ftm_alloc(ITEM_HALL, sizeof(struct hall_data));
    dat  = mod_to_hall_data(mod);

    memset(dat, 0x00, sizeof(*dat));
    /*NOTE: the assignment MUST be done, or exception happens when tester press Test Pass/Test Fail*/    
    dat->mod = mod; 
    
    if (!mod)
        return -ENOMEM;

    ret = ftm_register(mod, hall_entry, (void*)dat);

    return ret;
}

int hall_ata_init(void)
{
    LOGD(TAG "%s\n", __FUNCTION__);
    pthread_create(&ata_thd, NULL, hall_update_ata_thread, NULL);
    return 0;
}

bool get_hall_test_rst(void)
{
    return ata_test_pass;
}
#endif 

