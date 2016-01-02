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
#include <fcntl.h>                                                                                           
#include <getopt.h>                                                                                          
#include <limits.h>                                                                                          
#include <linux/input.h>                                                                                     
#include <stdio.h>                                                                                           
#include <stdlib.h>                                                                                          
#include <string.h>                                                                                          
#include <sys/reboot.h>                                                                                      
#include <sys/types.h>                                                                                       
#include <time.h>                                                                                            
#include <unistd.h>                                                                                                                                                                                                     
#include <cutils/properties.h>
#include "me_connection.h"
#include <pthread.h>
#include "common.h"
#include "ftm.h"
#include "miniui.h"
#include "utils.h"
#include "item.h"
#include "hardware/ccci_intf.h"
#include "at_command.h"
#if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
#include <c2kutils.h>
#endif
//#ifdef FEATURE_FTM_SIGNALTEST

#define TAG "[Signal Test] "

#define SLEEPMODE "AT+ESLP=0"
#define EMER_CALL "ATD112;"
#define GET_SN "AT+EGMR=0,5"
// Set SN should be "AT+EGMR=1,5,'SN'"
#define SET_SN "AT+EGMR=1,5"
#define AT "AT"
#define ATE0 "ATE0"
#define ATH "ATH"
#define BUF_SIZE 128

extern int bg_arr[MAX_ROWS];
extern char result[3][16];
pthread_mutex_t M_EIND;
pthread_cond_t  COND_EIND;

pthread_mutex_t M_VPUP;
pthread_cond_t  COND_VPUP;

pthread_mutex_t M_CREG;
pthread_cond_t  COND_CREG;

pthread_mutex_t M_ESPEECH_ECPI;
pthread_cond_t  COND_ESPEECH_ECPI;

pthread_mutex_t M_CONN;
pthread_cond_t  COND_CONN;

char atebuf[1024];

Connection modem[5] ;

enum {
#if defined(MTK_ENABLE_MD1)
		ITEM_CALL_FOR_MODEM_1,
#endif

#if defined(MTK_ENABLE_MD2)
    ITEM_CALL_FOR_MODEM_2,
#endif

#if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
    ITEM_CALL_FOR_MODEM_C2K,   
#endif

#ifdef MTK_ENABLE_MD5
    ITEM_CALL_FOR_MODEM_2,
#endif  
    ITEM_PASS,
    ITEM_FAIL,
};

static item_t sigtest_items[] = {
    #if defined(MTK_ENABLE_MD1)
    item(ITEM_CALL_FOR_MODEM_1,   uistr_info_emergency_call_in_modem1),
    #endif
    
    #if defined(MTK_ENABLE_MD2)
    item(ITEM_CALL_FOR_MODEM_2,   uistr_info_emergency_call_in_modem2),
    #endif
    
    
    #if defined(MTK_ENABLE_MD5)
    item(ITEM_CALL_FOR_MODEM_2,   uistr_info_emergency_call_in_modem2),
    #endif
    
    #if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
    item(ITEM_CALL_FOR_MODEM_C2K,   uistr_info_emergency_call_in_modem2),
    #endif

    item(ITEM_PASS,   uistr_pass),
    item(ITEM_FAIL,   uistr_fail),
    item(-1, NULL),
};

static item_t sigtest_item_auto[] = 
{
    item(-1, NULL),
};
struct sigtest {  
    char info[1024];
    bool exit_thd;
    int fd_atmd;
    int fd_atmd2;
    int fd_atmd_dt;
    int fd_atmd5;
    int test_type;
    text_t title;
    text_t text;
    pthread_t update_thd;
    struct ftm_module *mod;
    struct itemview *iv;
};

#define mod_to_sigtest(p)  (struct sigtest*)((char*)(p) + sizeof(struct ftm_module))
#define FREEIF(p)   do { if(p) free(p); (p) = NULL; } while(0)
#define HALT_INTERVAL 50000
#define MAX_MODEM_INDEX 3

pthread_t g_AP_UART_USB_RX;
pthread_t g_AP_CCCI_RX;
int g_mdFlag=1;  
int g_fd_uart = -1; 
int g_hUsbComPort = -1;
//int g_fd_atcmd = -1; 
//int g_fd_atcmdmd2=-1;
//int g_fd_atcmdmd_dt = -1;
//int g_fd_atcmd5 = -1;

//int ccci_handle[MODEM_MAX_NUM] = {-1};




//extern int wait4_ack (const int fd, char *pACK, int timeout);
//extern int read_ack (const int fd, char *rbuff, int length);
//extern int read_ack_for_ate_factory_mode(Connection &modem , char *rbuff, int length, int *flag);
//extern int send_at (const int fd, const char *pCMD);
extern int open_usb_port(int uart_id, int baudrate, int length, char parity_c, int stopbits);
//extern int ExitFlightMode(Connection& modem);

//extern int ExitFlightMode_DualTalk(Connection& modem);
//extern int openDeviceWithDeviceName(char *deviceName);

//extern int is_support_modem(int modem);
void (*g_SIGNAL_Callback[4])(void(*pdata))={SIGNAL1_Callback,SIGNAL2_Callback,SIGNAL3_Callback,SIGNAL4_Callback};

int AT_Pre_Process (char *buf_cmd)
{
#ifdef MTK_DT_SUPPORT
    if(!strcmp(buf_cmd, "AT+SWITCH"))
    {
        LOGD(TAG "AT+SWITCH");	
	      if(g_mdFlag==1)
	         g_mdFlag=2;
	      else if(g_mdFlag==2)
	         g_mdFlag=1;
	      else
	      LOGD(TAG "Unsupported MD Flag\n");
    }
#endif

return 0;
}

/*int ACK_Pre_Process (char *buf_cmd)
{  
	if (strstr(buf_cmd, "RING"))
	{ 
		char ccci_path[MAX_MODEM_INDEX][32] = {0};
    char *temp_ccci_path = NULL;
    int ccci_handle[MODEM_MAX_NUM] = {0} ;
    int temp_result[MODEM_MAX_NUM] = {0} ;
    Connection modem[MODEM_MAX_NUM] ;
		
		LOGD(TAG "MT call: RING\n");
		temp_ccci_path = get_ccci_path(0);
    strcpy(ccci_path[0],temp_ccci_path);
    modem[0].ATEChannel_Open(ccci_path[0]);
    modem[0].ATEChannel_Write("ATA\r");
    modem[0].ATEChannel_Close();
		//send_at(g_fd_atcmd, "ATA\r");
		//wait4_ack (g_fd_atcmd, NULL, 1000);
	}
	return 0;
}*/

void * AP_UART_USB_RX (void* lpParameter)
{
    char buf_cmd [BUF_SIZE] = {0};
    int len = 0;
    int wr_len = 0;
    char result[64] = {0};
    int at_pre_ret = 0;

    LOGD(TAG "%s start \n",__FUNCTION__);
    cmd_handler_init ();

     for (;;)
     {
         LOGD(TAG "Enter for() cycle");
	       len = read_a_line(g_hUsbComPort, buf_cmd, BUF_SIZE);            
	       LOGD(TAG "len = %d\n",len);    
	       if (len>0)
	       {
		         buf_cmd[len] = '\0';
        	   LOGD(TAG "AP_UART_USB_RX Command: %s, Len: %d\n" ,buf_cmd, len);
	  	       at_pre_ret = AT_Pre_Process (buf_cmd);
	  	       LOGD(TAG "----------->at_pre_ret = %d",at_pre_ret);
             LOGD("buf_cmd:%s\n", buf_cmd);

              CMD_OWENR_ENUM owner = rmmi_cmd_processor((unsigned char *)buf_cmd, result);
              LOGD(TAG "result:%s\n", result);
              if(owner == CMD_OWENR_AP)
              {
            	     LOGD(TAG "Go here 2");
                   wr_len = write_chars (g_hUsbComPort, result, strlen(result));
			             if (wr_len != strlen(result))
		               LOGE(TAG "AP_CCCI_RX: wr_len != rd_len\n");
              }
              else
              {
			            if(g_mdFlag == 1)
			            {
                      LOGD(TAG "Send command to modem1\n");
                      LOGD(TAG "buf_cmd = %s,len = %d",buf_cmd,strlen(buf_cmd));
                      modem[0].Write_ATEChannel(buf_cmd,strlen(buf_cmd));
			            }
		              else if(g_mdFlag == 2)
		              {
                      LOGD(TAG "Send command to modem2\n");
                      LOGD(TAG "buf_cmd = %s,len = %d",buf_cmd,strlen(buf_cmd));
                      modem[1].Write_ATEChannel(buf_cmd,strlen(buf_cmd));
			            }
		   }
 	     }
 	}
 	  return 	NULL ;
}

void * AP_CCCI_RX (void* lpParameter)
{
	char buf_ack [BUF_SIZE] = {0};
	char buf_log [BUF_SIZE] = {0};
	int rd_len=0, wr_len=0;
	
	LOGD(TAG "Enter AP_CCCI_RX()\n");
	
	for (;;)
	{
	      memcpy (buf_log, atebuf,strlen(atebuf));
	      buf_log[strlen(atebuf)] = '\0';
	      //ACK_Pre_Process(buf_log);
	      wr_len = write_chars (g_hUsbComPort, atebuf, strlen(atebuf));
	      if (wr_len != strlen(atebuf))
            {         
	          LOGE(TAG "AP_CCCI_RX: wr_len != rd_len\n");
	      }
	}
    return 	NULL ;
}

void New_Thread ()
{
	LOGD(TAG "Enter New_Thread() \n");
	if(pthread_create (&g_AP_UART_USB_RX, NULL, AP_UART_USB_RX, NULL) != 0)
	{
		LOGD (TAG "main:Create AP_UART_USB_RX thread failed");
		return;
	}

	if(pthread_create (&g_AP_CCCI_RX, NULL, AP_CCCI_RX, NULL) != 0)
	{
		LOGD (TAG "main:Create AP_CCCI_RX thread failed");
		return;
	}
	LOGD(TAG "Exit New_Thread() \n");
}

int Wait_Thread (pthread_t arg)
{   /* exit thread by pthread_kill -> pthread_join*/
    int err;
    if ((err = pthread_kill(arg, SIGUSR1)))
        return err;

    if ((err = pthread_join(arg, NULL)))
        return err;
    return 0;
}

int Free_Thread ()
{   /* exit thread by pthread_kill -> pthread_join*/
    int err = 0;
    if ( 0 != (err = Wait_Thread(g_AP_UART_USB_RX)) )
        return err;

    if ( 0 != (err = Wait_Thread(g_AP_CCCI_RX)) )
        return err;

    return 0;
}

int com_init_for_ate_factory_mode_usb_uart()
{
     g_fd_uart = open_uart_port(UART_PORT1, 115200, 8, 'N', 1);
	   if(g_fd_uart == -1) 
     {
         LOGE(TAG "Open uart port %d fail\r\n" ,UART_PORT1);
         return g_fd_uart;
     }
     else
     {
         LOGD(TAG "Open uart port %d success------%d\r\n" ,UART_PORT1,g_fd_uart);
     }
  	
	   g_hUsbComPort = open_usb_port(UART_PORT1, 115200, 8, 'N', 1);
	   if(g_hUsbComPort == -1)
	   {
	       LOGE(TAG "Open usb fail\r\n");
		     return g_hUsbComPort;
	   }
	   else
	   {
		     LOGD(TAG "Open usb %d success------%d\r\n",UART_PORT1,g_hUsbComPort);
	   }
	   return 0 ;
}

int com_deinit_for_ate_factory_mode_usb_uart()
{
    if (g_fd_uart!= -1)
    {
        close(g_fd_uart);
		    g_fd_uart = -1;
    }
    if (g_hUsbComPort!= -1)
    {
        close(g_hUsbComPort);
		    g_hUsbComPort = -1;
    }
    return 0;
}

int sigtest_entry(struct ftm_param *param, void *priv)
{   
	  static int signalFlag = 0 ;
	  bool exit = (bool)FALSE;
	  int chosen = 0 ;
    struct sigtest *st = (struct sigtest *)priv ;
    struct itemview *iv = NULL;
    int pret = 0 ;
    char ccci_path[MAX_MODEM_INDEX][32] = {0};
    char temp_ccci_path[MAX_MODEM_INDEX][32] = {0};
    int test_result_temp = TRUE;
    int modem_number = 0;
    int ccci_status = 0;
    int i = 0;
    int j = 0; 
    int modem_count = 0;
    int temp_result[MODEM_MAX_NUM] = {0} ;
    char *asciDevice = NULL ;
    LOGD(TAG "%s\n", __FUNCTION__); 
    init_COND();
    modem_number = get_md_count();
    
    LOGD(TAG "modem_number is %d\n",modem_number);
      
    if(modem_number == 0)
    {
        LOGD(TAG "There is no modem\n");
    }
    
    for(i = 0; i < MAX_MODEM_INDEX; i++)
    {
        if(1 == get_ccci_path(i,temp_ccci_path[i]))
        {
            strcpy(ccci_path[j],temp_ccci_path[i]);
            j++ ;
        }
    }
    
    if (0 == signalFlag)
    {
    	  if(1 == modem_number)
    	  {
    	      g_Flag_CREG = 0 ;
            g_Flag_EIND = 0 ;
            if(0 == modem[0].Conn_Init(ccci_path[0],1,g_SIGNAL_Callback[0]))
	          {
	          	  LOGD(TAG "modem 1 open fail!\n");
	          }
	          else
	          {
	              ExitFlightMode(modem[0]); 
	          }
            
        }
        if(modem_number != 1)
        {
        	  for(i=0 ;i<modem_number; i++)
        	  {
        	  	  g_Flag_CREG = 0 ;
                g_Flag_EIND = 0 ;
                if(0 == modem[i].Conn_Init(ccci_path[i],i+1,g_SIGNAL_Callback[i]))
	              {
	              	   LOGD(TAG "modem %d open fail!\n",i+1);
	              }
	              else
	              {
	              	  ExitFlightMode_DualTalk(modem[i]);
        	      }
        	  }
        }
    }
    else
    {
    	 LOGD(TAG "The second time signal test\n");
    	 for(i=0 ;i<modem_number; i++)
    	 {
    	     if(0 == modem[i].Conn_Init(ccci_path[i],i+1,g_SIGNAL_Callback[i]))
	         {
	             LOGD(TAG "modem %d open fail",(i+1));
	         }
	         else
	         {
	             LOGD(TAG "modem %d open OK",(i+1));
	         }
    	 }
    }
    signalFlag++;
    
    #if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
    g_Flag_CREG = 0 ;
    g_Flag_VPUP = 0 ;
    asciDevice = viatelAdjustDevicePathFromProperty(VIATEL_CHANNEL_AT);
    if(0 == modem[modem_number].Conn_Init(asciDevice,4,g_SIGNAL_Callback[3]))
    {
        LOGD("modem c2k open failed");
    }
    else
    {
    	  LOGD("modem c2k open successfully");
    	  C2Kmodemsignaltest(modem[modem_number]);
    }
    
    #endif
    
    init_text(&st->title, param->name, COLOR_YELLOW);
    init_text(&st->text, &st->info[0], COLOR_YELLOW);
    memset(&st->info[0], 0, sizeof(st->info));
    sprintf(st->info, "%s\n", uistr_info_emergency_call_testing);
    st->exit_thd = FALSE ;
    
    if (!st->iv) 
    {
        iv = ui_new_itemview();
        if (!iv) 
        {
            LOGD(TAG "No memory");
            return -1;
        }
        st->iv = iv;
    }
    st->test_type = param->test_type;

    if(FTM_MANUAL_ITEM == param->test_type)
    {
    	  iv = st->iv;
        iv->set_title(iv, &st->title);
    	  iv->set_text(iv, &st->text);
        sprintf(st->info, "%s\n", uistr_info_emergency_call_not_start);
        iv->set_items(iv, sigtest_items, 0);
        do 
        {
        	  modem_count = 0 ;
            chosen = iv->run(iv, &exit);
            switch (chosen)
            {
                #if defined(MTK_ENABLE_MD1)
                case ITEM_CALL_FOR_MODEM_1 :
                 g_Flag_ESPEECH_ECPI = 0 ;
                 modem_count++ ;
                 pret = 0 ;
                 sprintf(st->info, "%s\n", uistr_info_emergency_call_testing);
		             iv->redraw(iv);
		             LOGD(TAG "modem_count-1 = %d",modem_count-1);
		             pret = dial112(modem[modem_count-1]);
		    	       if(1 == pret)
                 {
		                 LOGD(TAG "Dial 112 Success in modem 1\n");
		                 sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem1);
		             }
		    	       else 
                 {
		    		         LOGD(TAG "Dial 112 Fail in modem 1\n");
		    		         sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem1);
		    	       }
		    	       iv->redraw(iv);
		    	       break; 
                #endif
                 
                 #if defined(MTK_ENABLE_MD2)
                 case ITEM_CALL_FOR_MODEM_2 :
                 	g_Flag_ESPEECH_ECPI = 0 ;
                  modem_count++ ;
                 	pret = 0 ;
                 	sprintf(st->info, "%s\n", uistr_info_emergency_call_testing);
		              iv->redraw(iv);
		              pret = dial112(modem[modem_count-1]);
		    	        if(1 == pret) 
                  {
		                   LOGD(TAG "Dial 112 Success in modem 2\n");
		                   sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
		              }
		    	        else 
                  {
		    		           LOGD(TAG "Dial 112 Fail in modem 2\n");
		    		           sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
		    	        }
		    	        iv->redraw(iv);
		    	        break; 
                 #endif 
                 
                 #if defined(MTK_ENABLE_MD5)
                 case ITEM_CALL_FOR_MODEM_2 :
                 	g_Flag_ESPEECH_ECPI = 0 ;
                  modem_count++ ;
                  pret = 0;
                 	sprintf(st->info, "%s\n", uistr_info_emergency_call_testing);
		              iv->redraw(iv);
		              pret = dial112(modem[modem_count-1]);
		    	        if(1 == pret) 
                  {
		                   LOGD(TAG "Dial 112 Success in modem 5\n");
		                   sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
		              }
		    	        else 
                  {
		    		           LOGD(TAG "Dial 112 Fail in modem 5\n");
		    		           sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
		    	        }
		    	        iv->redraw(iv);
		    	        break; 
                 #endif 
                 
                 #if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
                 case ITEM_CALL_FOR_MODEM_C2K :
                  g_Flag_CONN = 0 ;
                 	pret = 0 ;
                 	sprintf(st->info, "%s\n", uistr_info_emergency_call_testing);
		              iv->redraw(iv);
		              pret = dial112C2K(modem[modem_number]);
		    	        if(1 == pret) 
                  {
		                   LOGD(TAG "Dial 112 Success in c2k modem \n");
		                   sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
		              }
		    	        else 
                  {
		    		           LOGD(TAG "Dial 112 Fail in c2k modem \n");
		    		           sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
		    	        }
		    	        iv->redraw(iv);
		    	        break; 
                 #endif 
                 
                 case ITEM_PASS:
                 case ITEM_FAIL:
                      if (chosen == ITEM_PASS) 
                      {
                          st->mod->test_result = FTM_TEST_PASS;
                      } 
                      else if (chosen == ITEM_FAIL) 
                      {
                          st->mod->test_result = FTM_TEST_FAIL;
                      }
                      exit = TRUE;
                    break;
            }
            if(exit)
            {
                st->exit_thd = TRUE;
                break;	
            }
        } while (1);
    }
    else if((FTM_AUTO_ITEM == param->test_type) || (FTM_ASYN_ITEM == param->test_type))
    {
    	  modem_count = 0 ;
    	  i = 0 ;
    	  iv = st->iv;
        iv->set_title(iv, &st->title);
    	  iv->set_items(iv, sigtest_item_auto, 0);
        iv->set_text(iv, &st->text); 
        iv->start_menu(iv,0);
        iv->redraw(iv);
 
        #ifdef MTK_ENABLE_MD1
        g_Flag_ESPEECH_ECPI = 0 ;
        pret = 0 ;
        modem_count++;
        pret = dial112(modem[modem_count-1]);
		    if(1 == pret)
        {
		         LOGD(TAG "Dial 112 Success in modem 1\n");
		         sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem1);
		         temp_result[i] = TRUE ;
		    }
		    else 
        {
		         LOGD(TAG "Dial 112 Fail in modem 1\n");
		         sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem1);
		         temp_result[i] = FALSE ;
		    }
		    i++ ;
		    iv->redraw(iv);
        #endif
        
        #if defined(MTK_ENABLE_MD2)
        g_Flag_ESPEECH_ECPI = 0 ;
        pret = 0 ;
        modem_count++;
		    pret = dial112(modem[modem_count-1]);
		    if(1 == pret) 
        {
		         LOGD(TAG "Dial 112 Success in modem 2\n");
		         sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
		         temp_result[i] = TRUE ;
		    }
		    else 
        {
		         LOGD(TAG "Dial 112 Fail in modem 2\n");
		         sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
		         temp_result[i] = FALSE ;
		    }
		    i++ ;
		    iv->redraw(iv);
        #endif 
		    
		    #if defined(MTK_ENABLE_MD5)
		    g_Flag_ESPEECH_ECPI = 0 ;
        pret = 0 ;
		    modem_count++ ;
		    pret = dial112(modem[modem_count-1]);
		    if(1 == pret) 
        {
		        LOGD(TAG "Dial 112 Success in  modem 5 \n");
		        sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
		        temp_result[i] = TRUE ;
		    }
		    else 
        {
		        LOGD(TAG "Dial 112 Fail in modem 5 \n");
		        sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
		        temp_result[i] = FALSE ;
		    }
		    i++ ;
		    iv->redraw(iv);
		    #endif
             
        #if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
        g_Flag_VPUP  = 0;
		    pret = 0 ;
		    pret = dial112C2K(modem[modem_number]);
		    if(1 == pret) 
        {
		        LOGD(TAG "Dial 112 Success in c2k modem \n");
		        sprintf(st->info, "%s\n", uistr_info_emergency_call_success_in_modem2);
		        temp_result[i] = TRUE ;
		    }
		    else 
        {
		        LOGD(TAG "Dial 112 Fail in c2k modem \n");
		        sprintf(st->info, "%s\n", uistr_info_emergency_call_fail_in_modem2);
		        temp_result[i] = FALSE ;
		    }
		    i++ ;
		    iv->redraw(iv);
		    #endif
		    
        for(j=0 ;j<i ;j++ )
        {
            test_result_temp = test_result_temp&&temp_result[j] ;
        }
        if(1 == test_result_temp)
        {
            st->mod->test_result = FTM_TEST_PASS;
        }
        else
        {
            st->mod->test_result = FTM_TEST_FAIL;	
        }
        iv->redraw(iv);
    }
    
    LOGD(TAG "[AT]CCCI port close\n");
    for(i=0;i<modem_number;i++)
    {
        LOGD(TAG "modem %d deinit start \n",i);
        if(1 == modem[i].Conn_DeInit())
        {
        	 LOGD(TAG "Deinit the port successfully\n");
        }
        else
        {
           LOGD(TAG "Deinit the port failed \n");
        }
    
        LOGD(TAG "modem %d deinit end \n",i);
    }
    #if defined(EVDO_DT_SUPPORT) || defined (MTK_ENABLE_MD3)
      LOGD(TAG "[AT]modem_number = %d,SDIO port close\n",modem_number);
      modem[modem_number].Conn_DeInit();
      LOGD(TAG "[AT]SDIO port close done \n");
    #endif
    deinit_COND();
    return 0;
}

struct ftm_module* sigtest_init(void)
{
    int ret;
    struct ftm_module *mod;
    struct sigtest *st;

    LOGD(TAG "%s\n", __FUNCTION__);
    
    mod = ftm_alloc(ITEM_SIGNALTEST, sizeof(struct sigtest));
    st  = mod_to_sigtest(mod);

    st->mod = mod;
    
    if (!mod)
        return NULL;

    ret = ftm_register(mod, sigtest_entry, (void*)st);

    if(ret == 0) {
        LOGD(TAG "ftm_register success!\n");
        return mod;
    }
    else {
        LOGD(TAG "ftm_register fail!\n");
        return NULL;
    }
}

// The below is for ATE signal test.
void ate_signal(void)
{
    struct itemview ate;
    text_t ate_title, info;
    int modem_number = 0;
    char buf[100] = {0};
    int i = 0;
    int j = 0;
    char dev_node[32] = {0};
	  int flag=0;
	  char ccci_path[MAX_MODEM_INDEX][32] = {0};
	  char temp_ccci_path[MAX_MODEM_INDEX][32] = {0};
    int test_result_temp = TRUE;
    
    ui_init_itemview(&ate);
    init_text(&info, buf, COLOR_YELLOW);
    ate.set_text(&ate, &info);
    sprintf(buf, "%s", "ATE Signaling Test\nEmergency call is not started\n");
    ate.redraw(&ate);

    LOGD(TAG "Entry ate_signal\n");
    
    com_init_for_ate_factory_mode_usb_uart();	
    
    modem_number = get_md_count();

    for(i = 0; i < MAX_MODEM_INDEX; i++)
    {
        if(1 == get_ccci_path(i,temp_ccci_path[i]))
        {
            strcpy(ccci_path[j],temp_ccci_path[i]);
            j++ ;
        }
    }

    if(1 == modem_number)
    {
        if(0 == modem[0].Conn_Init(ccci_path[0],1,g_SIGNAL_Callback[0]))
	      {
	          LOGD("modem 1 open fail");
	      }
	      else
	      {
	      	  LOGD("modem 1 open ok");
	      	  ExitFlightMode(modem[0]);
	         
	      }
    }
    if(2 == modem_number)
    {
    	  for(i=0 ;i<modem_number; i++)
    	  {
    	      if(0 == modem[i].Conn_Init(ccci_path[i],i+1,g_SIGNAL_Callback[i]))
	          {
	              LOGD(TAG "modem %d open fail",(i+1));
	          }
	          else
	          {
	              LOGD(TAG "modem %d open OK",(i+1));
                ExitFlightMode_DualTalk(modem[i]);
            }
    	  }

    }
    
    LOGD(TAG "[AT]CCCI port close\n");
    for(i=0;i<modem_number;i++)
    {
        LOGD(TAG "modem %d deinit start \n",i);
        if(1 == modem[i].Conn_DeInit())
        {
        	 LOGD(TAG "Deinit the port successfully\n");
        }
        else
        {
           LOGD(TAG "Deinit the port failed \n");
        }
    
        LOGD(TAG "modem %d deinit end \n",i);
    }
    
    sleep(1);
    
    for(i = 0; i<modem_number; i++)
    {
    	  LOGD(TAG "Open ATE channel start: \n");
        if(1 == modem[i].Open_ATEChannel(ccci_path[i],i,g_ATE_Callback))
        {
            LOGD(TAG "Open ATE channel successfully: \n"); 
        }
        else
        {
        	  LOGD(TAG "Open ATE channel falied: \n");
        }  
            	
    }
    
	  New_Thread ();
	
    while(1) 
	  {
        usleep(HALT_INTERVAL*20);
    }
	
	  Free_Thread ();

    for(i = 0; i<modem_number; i++)
    {
    	  LOGD(TAG "close ATE channel start: \n");
    	  if(1==modem[i].Close_ATEChannel())
    	  {
    	      LOGD(TAG "Close ATE channel successfully: \n");
    	  }
    	  else
    	  {
    	      LOGD(TAG "Close ATE channel failed: \n"); 	
    	  }
       	
    }
    com_deinit_for_ate_factory_mode_usb_uart();

}
//#endif

void init_COND()
{
    pthread_mutex_init(&M_EIND,0);
    pthread_cond_init(&COND_EIND,0);
    pthread_mutex_init(&M_CREG,0);
    pthread_cond_init(&COND_CREG,0);
    pthread_mutex_init(&M_ESPEECH_ECPI,0);
    pthread_cond_init(&COND_ESPEECH_ECPI,0);
    pthread_mutex_init(&M_CONN,0);
    pthread_cond_init(&COND_CONN,0);
    pthread_mutex_init(&M_VPUP,0);
    pthread_cond_init(&COND_VPUP,0);
}

void deinit_COND()
{
    pthread_cond_destroy(&COND_EIND);
    pthread_mutex_destroy(&M_EIND);     
		pthread_cond_destroy(&COND_CREG);
    pthread_mutex_destroy(&M_CREG);
    pthread_cond_destroy(&COND_ESPEECH_ECPI);
    pthread_mutex_destroy(&M_ESPEECH_ECPI);
    pthread_cond_destroy(&COND_CONN);
    pthread_mutex_destroy(&M_CONN);
    pthread_cond_destroy(&COND_VPUP);
    pthread_mutex_destroy(&M_VPUP);
}