/******************************************************************************
 *
 *  Copyright (C) 2014 Google, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#define LOG_TAG "bt_fwd_service"

#include <assert.h>
#include <string.h>
#include <dlfcn.h>
#include <cutils/log.h>
#include <cutils/properties.h>
#include "bt_vendor_lib.h"
#include <cutils/log.h>
#include <cutils/klog.h>

#define LOGD ALOGD
#define LOGI ALOGI
#define LOGE ALOGE
#define LOGV ALOGV

#define LAST_VENDOR_OPCODE_VALUE VENDOR_DO_EPILOG

static const char *VENDOR_LIBRARY_NAME = "libbt-vendor.so";
static const char *VENDOR_LIBRARY_SYMBOL_NAME = "BLUETOOTH_VENDOR_LIB_INTERFACE";

static void *lib_handle;
static bt_vendor_interface_t *lib_interface;

static const bt_vendor_callbacks_t lib_callbacks = {
    sizeof(lib_callbacks),
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
};

// Interface functions
static uint8_t vendor_open(const uint8_t *local_bdaddr) {
    assert(lib_handle == NULL);

    lib_handle = dlopen(VENDOR_LIBRARY_NAME, RTLD_NOW);
    if (!lib_handle) {
        LOGE("%s unable to open %s: %s", __func__, VENDOR_LIBRARY_NAME, dlerror());
        goto error;
    }

    lib_interface = (bt_vendor_interface_t *)dlsym(lib_handle, VENDOR_LIBRARY_SYMBOL_NAME);
    if (!lib_interface) {
        LOGE("%s unable to find symbol %s in %s: %s", __func__, VENDOR_LIBRARY_SYMBOL_NAME,
                VENDOR_LIBRARY_NAME, dlerror());
        goto error;
    }

    int status = lib_interface->init(&lib_callbacks, (unsigned char *)local_bdaddr);
    if (status) {
        LOGE("%s unable to initialize vendor library: %d", __func__, status);
        goto error;
    }

    return 1;

error:;
      lib_interface = NULL;
      if (lib_handle)
          dlclose(lib_handle);
      lib_handle = NULL;
      return 0;
}

static void vendor_close(void) {
    if (lib_interface)
        lib_interface->cleanup();

    if (lib_handle)
        dlclose(lib_handle);

    lib_interface = NULL;
    lib_handle = NULL;
}

int main() {
    int power_state = BT_VND_PWR_ON;
    char bt_state[PROPERTY_VALUE_MAX] = {'\0'};
    if (!property_get("persist.bluetooth.state", bt_state , "0") ||
            strcmp(bt_state, "1")) {
        LOGD("%s BT is not enabled, Not downloading Firmware in advance", __func__);
        return 0;
    }
    if (vendor_open (NULL)) {
        if (!lib_interface->op(BT_VND_OP_POWER_CTRL, &power_state)) {
            if (!lib_interface->op(BT_VND_OP_FW_DOWNLOAD, NULL)) {
                LOGD("%s Downloaded BT Firmware in advance", __func__);
                printMarker("BT:FW-Download done");
            }
        }
        vendor_close ();
    }
    return 0;
}
