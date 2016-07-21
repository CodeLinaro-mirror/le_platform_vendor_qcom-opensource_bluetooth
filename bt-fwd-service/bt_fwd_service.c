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

#define LOG_TAG "bt_vendor"

#include <assert.h>
#include <dlfcn.h>

#include "buffer_allocator.h"
#include "bt_vendor_lib.h"
#include "osi/include/osi.h"
#include "osi/include/log.h"
#include "vendor.h"

#define LAST_VENDOR_OPCODE_VALUE VENDOR_DO_EPILOG

static const char *VENDOR_LIBRARY_NAME = "libbt-vendor.so";
static const char *VENDOR_LIBRARY_SYMBOL_NAME = "BLUETOOTH_VENDOR_LIB_INTERFACE";

static void *lib_handle;
static bt_vendor_interface_t *lib_interface;

static const bt_vendor_callbacks_t lib_callbacks = {
  sizeof(lib_callbacks),
  firmware_config_cb,
  sco_config_cb,
  low_power_mode_cb,
  sco_audiostate_cb,
  buffer_alloc_cb,
  buffer_free_cb,
  transmit_cb,
  epilog_cb
};

// Interface functions

static bool vendor_open(
    const uint8_t *local_bdaddr,
    const hci_t *hci_interface) {
  assert(lib_handle == NULL);
  hci = hci_interface;

  lib_handle = dlopen(VENDOR_LIBRARY_NAME, RTLD_NOW);
  if (!lib_handle) {
    LOG_ERROR("%s unable to open %s: %s", __func__, VENDOR_LIBRARY_NAME, dlerror());
    goto error;
  }

  lib_interface = (bt_vendor_interface_t *)dlsym(lib_handle, VENDOR_LIBRARY_SYMBOL_NAME);
  if (!lib_interface) {
    LOG_ERROR("%s unable to find symbol %s in %s: %s", __func__, VENDOR_LIBRARY_SYMBOL_NAME, VENDOR_LIBRARY_NAME, dlerror());
    goto error;
  }

  LOG_INFO("alloc value %p", lib_callbacks.alloc);

  int status = lib_interface->init(&lib_callbacks, (unsigned char *)local_bdaddr);
  if (status) {
    LOG_ERROR("%s unable to initialize vendor library: %d", __func__, status);
    goto error;
  }

  return true;

error:;
  lib_interface = NULL;
  if (lib_handle)
    dlclose(lib_handle);
  lib_handle = NULL;
  return false;
}

static void vendor_close(void) {
  if (lib_interface)
    lib_interface->cleanup();

  if (lib_handle)
    dlclose(lib_handle);

  lib_interface = NULL;
  lib_handle = NULL;
}
