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
#include <linux/mtkfb.h>
#include "cust.h"
#include "common.h"
#include "miniui.h"
#include "ftm.h"
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <linux/input.h>
#include <sys/ioctl.h>

#ifdef FEATURE_FTM_FINGERPRINT

#define JMT_BASE_IO_OFFSET          0
#define JMT101_IOCTL_MAGIC_NO       0xFC
#define JMT_IOCTL_BASE(x)           JMT_BASE_IO_OFFSET+x
#define JMT_IOCTL_RV          _IOWR(JMT101_IOCTL_MAGIC_NO, JMT_IOCTL_BASE(10), unsigned int)
#define JMT_IOCTL_SET_OP           _IOWR(JMT101_IOCTL_MAGIC_NO, JMT_IOCTL_BASE(12), unsigned int)

bool fingerprint_test_exit = false;

enum {
	ITEM_PASS,
	ITEM_FAIL
};

static item_t items_pass[] = {
	item(ITEM_PASS,   uistr_pass),
	item(-1, NULL),
};
static item_t items_fail[] = {
	item(ITEM_FAIL,   uistr_fail),
	item(-1, NULL),
};

static inline int file_exist(char * filename)
{
    return (access(filename, F_OK) == 0);
}

static unsigned int update_fingerprint()
{
    FILE *in;
    char buffer[4];
    int ret = 0;

    int fd;
    unsigned int id_val = 0;
    unsigned int set_op = 0x5350;
    unsigned char value = 0;

	LOGD("%s: Start\n", __FUNCTION__);
    if(file_exist("/sys/bus/spi/devices/spi0.0/diag/selftest"))
    {
        in = fopen("/sys/bus/spi/devices/spi0.0/diag/selftest", "r");
        if(in != NULL)
        {
            if(fgets(buffer, sizeof(buffer), in) != NULL)
            {
                if(buffer[0] == '1')
                {
                    ret = 1;
                }
            }
            fclose(in);
        }
    }

    if((ret != 1)  && file_exist("/dev/jmt101"))
    {
        fd=open("/dev/jmt101", O_RDWR);
        if(fd<0){
          LOGD("Can not Open /dev/jmt101 ! \n");
        } else {
          ret = ioctl(fd, JMT_IOCTL_SET_OP,&set_op);
          if(-1 == ret){
            LOGD("ioctl JMT_IOCTL_SET_OP error!\n");
          }

          ret = ioctl(fd, JMT_IOCTL_RV,&id_val);
          if(-1 == ret){
            LOGD("ioctl JMT_IOCTL_RV error!\n");
          }
          value = id_val & 0xff;
          LOGD("get value = 0x%x !\n",value);
          if (value > 0) {
            ret = 1;
          }
          close(fd);
        }
      }

      LOGD("%s: Exit\n", __FUNCTION__);
      return ret;
}

int fingerprint_entry(struct ftm_param *param, void *priv)
{
	int chosen;
	bool exit = false;
	unsigned int ret = 0;
	struct itemview *iv;
	text_t    title;
	struct ftm_module *mod = (struct ftm_module *)priv;

	LOGD("%s\n", __FUNCTION__);

	fingerprint_test_exit = false;
	ret = update_fingerprint();
	iv = ui_new_itemview();
	if (!iv) {
		LOGD("No memory");
		return -1;
	}

  init_text(&title, param->name, COLOR_YELLOW);

	iv->set_title(iv, &title);
	if(ret == 1){
  	iv->set_items(iv, items_pass, 0);
  	mod->test_result = FTM_TEST_PASS;
  }
  if(ret == 0){
  	iv->set_items(iv, items_fail, 0);
  	mod->test_result = FTM_TEST_FAIL;
  }
  	sleep(1);
  		iv->start_menu(iv, 0);
			iv->redraw(iv);
			sleep(1);
	return 0;
}

int fingerprint_init(void)
{
	int ret = 0;
	struct ftm_module *mod;

	LOGD("%s\n", __FUNCTION__);

	mod = ftm_alloc(ITEM_FINGERPRINT, sizeof(struct ftm_module));
	if (!mod)
		return -ENOMEM;

	ret = ftm_register(mod, fingerprint_entry, (void*)mod);

	return ret;
}

#endif
