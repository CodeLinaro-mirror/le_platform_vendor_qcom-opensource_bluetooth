/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <stdbool.h>
#include <stdint.h>
#ifndef USE_MUSL
#include <sys/cdefs.h>
#endif
#include <sys/types.h>
#define BT_PROFILE_OBEX_ID "obex"

#define BTSOCK_FLAG_ENCRYPT 1
#define BTSOCK_FLAG_AUTH (1 << 1)
#define BTSOCK_FLAG_NO_SDP (1 << 2)
#define BTSOCK_FLAG_AUTH_MITM (1 << 3)
#define BTSOCK_FLAG_AUTH_16_DIGIT (1 << 4)
#ifndef ANDROID_INCLUDE_BT_SOCKET_H
typedef enum {
  BTSOCK_RFCOMM = 1,
  BTSOCK_SCO = 2,
  BTSOCK_L2CAP = 3,
  BTSOCK_L2CAP_LE = 4
} btsock_type_t;

typedef enum {
    BTSOCK_OPT_GET_MODEM_BITS = 1,
    BTSOCK_OPT_SET_MODEM_BITS = 2,
    BTSOCK_OPT_CLR_MODEM_BITS = 3,
} btsock_option_type_t;
#endif
#ifndef ANDROID_INCLUDE_BLUETOOTH_H
typedef enum {
  BT_STATUS_SUCCESS,
  BT_STATUS_FAIL,
  BT_STATUS_NOT_READY,
  BT_STATUS_NOMEM,
  BT_STATUS_BUSY,
  BT_STATUS_DONE, /* request already completed */
  BT_STATUS_UNSUPPORTED,
  BT_STATUS_PARM_INVALID,
  BT_STATUS_UNHANDLED,
  BT_STATUS_AUTH_FAILURE,
  BT_STATUS_RMT_DEV_DOWN,
  BT_STATUS_AUTH_REJECTED,
  BT_STATUS_JNI_ENVIRONMENT_ERROR,
  BT_STATUS_JNI_THREAD_ATTACH_ERROR,
  BT_STATUS_WAKELOCK_ERROR
} bt_status_t;
#endif
typedef struct {
    uint8_t address[6];
} __attribute__((packed))bt_bdaddr_t_v1;

typedef struct {
    /** set to size of this struct*/
    size_t          size;

    /**
     * Listen to a RFCOMM UUID or channel. It returns the socket fd from which
     * btsock_connect_signal can be read out when a remote device connected.
     * If neither a UUID nor a channel is provided, a channel will be allocated
     * and a service record can be created providing the channel number to
     * create_sdp_record(...) in bt_sdp.
     * The callingUid is the UID of the application which is requesting the socket. This is
     * used for traffic accounting purposes.
     */
    bt_status_t (*listen)(btsock_type_t type, const char* service_name,
            const uint8_t* service_uuid, int channel, int* sock_fd, int flags, int callingUid);

    /**
     * Connect to a RFCOMM UUID channel of remote device, It returns the socket fd from which
     * the btsock_connect_signal and a new socket fd to be accepted can be read out when connected.
     * The callingUid is the UID of the application which is requesting the socket. This is
     * used for traffic accounting purposes.
     */
    bt_status_t (*connect)(const bt_bdaddr_t_v1 *bd_addr, btsock_type_t type, const uint8_t* uuid,
            int channel, int* sock_fd, int flags, int callingUid);

} btsock_interface_t_v1;


