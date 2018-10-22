/******************************************************************************
 *  Copyright (C) 2018, The Linux Foundation. All rights reserved.
 *
 *  Not a Contribution
 *****************************************************************************/
/*****************************************************************************
 *  Copyright (C) 2009-2012 Broadcom Corporation
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
/*****************************************************************************
 *
 *  Filename:      bthost_avk_ipc.h
 *
 *  Description:
 *
 *****************************************************************************/
#ifndef BT_HOST_AVK_IPC_H
#define BT_HOST_AVK_IPC_H
/*****************************************************************************
**  Constants & Macros
******************************************************************************/

#define CODEC_AVK_OFFSET 2

typedef enum {
  A2DP_AVK_CTRL_CMD_NONE,
  A2DP_AVK_CTRL_CMD_CHECK_SOCKET,
  A2DP_AVK_CTRL_CMD_CHECK_READY,
  A2DP_AVK_CTRL_CMD_START_CAPTURE,
  A2DP_AVK_CTRL_CMD_STOP_CAPTURE,
  A2DP_AVK_CTRL_SESSION_SETUP_COMPLETE,
  A2DP_AVK_CTRL_GET_CODEC_CONFIG,
} tA2DP_AVK_CTRL_CMD;

typedef enum {
  AUDIO_A2DP_AVK_STATE_STARTING_CAPTURE,
  AUDIO_A2DP_AVK_STATE_STARTED_CAPTURE,
  AUDIO_A2DP_AVK_STATE_STOPPING,
  AUDIO_A2DP_AVK_STATE_STOPPED,
  AUDIO_A2DP_AVK_STATE_SESSION_COMPLETE
} a2dp_avk_state_t;

typedef struct {
    /** Set to sizeof(bt_host_avk_ipc_interface_t) */
    size_t          size;
    int (*audio_sink_start_capture)(void);
    int (*audio_sink_stop_capture)(void);
    void* (*audio_get_decoder_config)(audio_format_t *codec_type);
    int (*audio_sink_session_setup_complete)(uint64_t latency);
    int (*audio_check_a2dp_ready)(void);
} bt_host_avk_ipc_interface_t;

extern "C" int audio_sink_start_capture(void);
extern "C" int audio_sink_check_a2dp_ready(void);
extern "C" int audio_sink_stop_capture(void);
extern "C" int audio_sink_session_setup_complete(uint64_t latency);
extern "C" void* audio_get_decoder_config(audio_format_t *codec_type);

#endif
