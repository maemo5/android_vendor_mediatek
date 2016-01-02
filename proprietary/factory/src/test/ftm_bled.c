
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

#include "cust.h"
#include "common.h"
#include "miniui.h"
#include "ftm.h"

#ifdef FEATURE_FTM_BLED
#define BLED_FILE		"/sys/class/breathlightdrv/breathlight/open"
#define RED_LED_FILE		"/sys/class/leds/red/brightness"
#define GREEN_LED_FILE		"/sys/class/leds/green/brightness"
#define BLUE_LED_FILE		"/sys/class/leds/blue/brightness"


enum {
	ITEM_BLED_TEST,
	ITEM_PASS,
	ITEM_FAIL,
};

static item_t items[] = {
	item(ITEM_BLED_TEST,uistr_bled),
	item(ITEM_PASS,   uistr_pass),
	item(ITEM_FAIL,   uistr_fail),
	item(-1, NULL),
};

extern int status;
static int
write_int(char const* path, int value)
{
	int fd;

	if (path == NULL)
		return -1;

	fd = open(path, O_RDWR);
	if (fd >= 0) {
		char buffer[20];
		int bytes = sprintf(buffer, "%d\n", value);
		int amt = write(fd, buffer, bytes);
		close(fd);
		if(amt == -1)
		{
			LOGE("write_int failed to write %s\n", path);
			return -errno;
		}
		else
		{
			return 0;
		}
	}

	LOGE("write_int failed to open %s\n", path);
	return -errno;
}


int bled_entry(struct ftm_param *param, void *priv)
{
	int chosen;
	bool exit = false;
	struct itemview *iv;
	text_t    title;
	struct ftm_module *mod = (struct ftm_module *)priv;

	LOGD("%s\n", __FUNCTION__);

	iv = ui_new_itemview();
	if (!iv) {
		LOGD("No memory");
		return -1;
	}

	init_text(&title, param->name, COLOR_YELLOW);
	iv->set_title(iv, &title);
	iv->set_items(iv, items, 0);
    if(status == 1){
      write_int(BLED_FILE, 17);
      write_int(RED_LED_FILE, 250);
      write_int(GREEN_LED_FILE, 250);
      write_int(BLUE_LED_FILE, 250);
      mod->test_result = FTM_TEST_PASS;
      return 0;
    }
    if(status == 0){
        do {
            chosen = iv->run(iv, &exit);
            switch (chosen) {
                case ITEM_BLED_TEST:
                    write_int(BLED_FILE, 17);
                    write_int(RED_LED_FILE, 250);
                    write_int(GREEN_LED_FILE, 250);
                    write_int(BLUE_LED_FILE, 250);
                    break;
                    
                case ITEM_PASS:
                    mod->test_result = FTM_TEST_PASS;
                    exit = true;
                    break;
                case ITEM_FAIL:
                    mod->test_result = FTM_TEST_FAIL;
                    exit = true;
                    break;
                default:
                    break;
            }
            
            if (exit) {
                write_int(BLED_FILE, 0);
                break;
            }		
        } 
        while (1);
    }
	return 0;
}

int bled_init(void)
{
	int index;
	int ret = 0;
	struct ftm_module *mod;

	LOGD("%s\n", __FUNCTION__);
	
	mod = ftm_alloc(ITEM_BLED, sizeof(struct ftm_module));
	if (!mod)
		return -ENOMEM;


	ret = ftm_register(mod, bled_entry, (void*)mod);

	return ret;
}

#endif
