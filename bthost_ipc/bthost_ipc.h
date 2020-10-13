/******************************************************************************
 *  Copyright (C) 2016-2018, The Linux Foundation. All rights reserved.
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
 *  Filename:      bthost_ipc.h
 *
 *  Description:
 *
 *****************************************************************************/
#ifndef BT_HOST_IPC_H
#define BT_HOST_IPC_H
#include "audio_a2dp_hw.h"
#include <system/audio.h>
/*****************************************************************************
**  Constants & Macros
******************************************************************************/

#define BT_AUDIO_HARDWARE_INTERFACE "libbthost"

typedef enum {
  A2DP_CTRL_GET_CODEC_CONFIG = 15,
  A2DP_CTRL_GET_MULTICAST_STATUS,
  A2DP_CTRL_GET_CONNECTION_STATUS,
  A2DP_CTRL_GET_NUM_CONNECTED_DEVICE,
} tA2DP_CTRL_EXT_CMD;

/*
codec specific definitions
*/
#define CODEC_TYPE_SBC 0x00
#define CODEC_TYPE_AAC 0x02
#define NON_A2DP_CODEC_TYPE 0xFF
#define CODEC_OFFSET 3
#define VENDOR_ID_OFFSET 4
#define CODEC_ID_OFFSET (VENDOR_ID_OFFSET + 4)
#define CODEC_TYPE_PCM 0x05

#ifndef VENDOR_APTX
#define VENDOR_APTX 0x4F
#endif
#ifndef VENDOR_APTX_HD
#define VENDOR_APTX_HD 0xD7
#endif
#ifndef VENDOR_APTX_ADAPTIVE
#define VENDOR_APTX_ADAPTIVE 0xD7
#endif
#ifndef VENDOR_APTX_LL
#define VENDOR_APTX_LL 0x0A
#endif
#ifndef APTX_CODEC_ID
#define APTX_CODEC_ID 0x01
#endif
#ifndef APTX_HD_CODEC_ID
#define APTX_HD_CODEC_ID 0x24
#endif
#ifndef APTX_ADAPTIVE_CODEC_ID
#define APTX_ADAPTIVE_CODEC_ID 0xAD
#endif
#ifndef APTX_ADAPTIVE_RESERVED_BITS
#define APTX_ADAPTIVE_RESERVED_BITS 23
#endif
#ifndef APTX_TWS_CODEC_ID
#define APTX_TWS_CODEC_ID 0x25
#endif

#define A2D_SBC_FREQ_MASK 0xF0
#define A2D_SBC_CHN_MASK  0x0F
#define A2D_SBC_BLK_MASK  0xF0
#define A2D_SBC_SUBBAND_MASK 0x0C
#define A2D_SBC_ALLOC_MASK 0x03
#define A2D_SBC_SAMP_FREQ_16     0x80    /* b7:16  kHz */
#define A2D_SBC_SAMP_FREQ_32     0x40    /* b6:32  kHz */
#define A2D_SBC_SAMP_FREQ_44     0x20    /* b5:44.1kHz */
#define A2D_SBC_SAMP_FREQ_48     0x10    /* b4:48  kHz */
#define A2D_SBC_CH_MD_MONO       0x08    /* b3: mono */
#define A2D_SBC_CH_MD_DUAL       0x04    /* b2: dual */
#define A2D_SBC_CH_MD_STEREO     0x02    /* b1: stereo */
#define A2D_SBC_CH_MD_JOINT      0x01    /* b0: joint stereo */
#define A2D_SBC_BLOCKS_4         0x80    /* 4 blocks */
#define A2D_SBC_BLOCKS_8         0x40    /* 8 blocks */
#define A2D_SBC_BLOCKS_12        0x20    /* 12blocks */
#define A2D_SBC_BLOCKS_16        0x10    /* 16blocks */
#define A2D_SBC_SUBBAND_4        0x08    /* b3: 4 */
#define A2D_SBC_SUBBAND_8        0x04    /* b2: 8 */
#define A2D_SBC_ALLOC_MD_S       0x02    /* b1: SNR */
#define A2D_SBC_ALLOC_MD_L       0x01    /* b0: loundess */

/* APTX bitmask helper */
#define A2D_APTX_SAMP_FREQ_MASK  0xF0
#define A2D_APTX_SAMP_FREQ_48    0x10
#define A2D_APTX_SAMP_FREQ_44    0x20
#define A2D_APTX_CHAN_MASK       0x0F
#define A2D_APTX_CHAN_STEREO     0x02
#define A2D_APTX_CHAN_MONO       0x01
#define A2D_APTX_TWS_CHAN_MODE   0x08

/* APTX Adaptive bitmask helper */
#define A2D_APTX_ADAPTIVE_SAMP_FREQ_MASK           (0xF8)
#define A2D_APTX_ADAPTIVE_CHAN_MASK                (0x1F)
#define A2DP_APTX_ADAPTIVE_SAMPLERATE_44100        (0x08)
#define A2DP_APTX_ADAPTIVE_SAMPLERATE_48000        (0x10)
#define A2DP_APTX_ADAPTIVE_SAMPLERATE_88000        (0x20)
#define A2DP_APTX_ADAPTIVE_SAMPLERATE_192000       (0x40)
#define A2DP_APTX_ADAPTIVE_CHANNELS_MONO          (0x01)
#define A2DP_APTX_ADAPTIVE_CHANNELS_STEREO        (0x02)
#define A2DP_APTX_ADAPTIVE_CHANNELS_TWS_STEREO    (0x04)
#define A2DP_APTX_ADAPTIVE_CHANNELS_JOINT_STEREO  (0x08)
#define A2DP_APTX_ADAPTIVE_CHANNELS_TWS_MONO      (0x10)


#define A2D_AAC_IE_OBJ_TYPE_MSK                0xF0    /* b7-b4 Object Type */
#define A2D_AAC_IE_OBJ_TYPE_MPEG_2_AAC_LC      0x80    /* b7:MPEG-2 AAC LC */
#define A2D_AAC_IE_OBJ_TYPE_MPEG_4_AAC_LC      0x40    /* b7:MPEG-4 AAC LC */
#define A2D_AAC_IE_OBJ_TYPE_MPEG_4_AAC_LTP     0x20    /* b7:MPEG-4 AAC LTP */
#define A2D_AAC_IE_OBJ_TYPE_MPEG_4_AAC_SCA     0x10    /* b7:MPEG-4 AAC SCALABLE */

#define A2D_AAC_IE_CHANNELS_MSK                0x0C
#define A2D_AAC_IE_CHANNELS_1                  0x08    /* Channel 1 */
#define A2D_AAC_IE_CHANNELS_2                  0x04    /* Channel 2 */

#define A2D_AAC_IE_VBR_MSK                     0x80
#define A2D_AAC_IE_VBR                         0x80    /* supported */

typedef struct {
    uint8_t  codec_type;
    uint8_t  dev_idx;
    uint16_t sampling_rate; /*44.1khz,48khz*/
    uint8_t  chn;           /*0(Mono),1(Dual),2(Stereo),3(JS)*/
    uint8_t  blk_len;       /*4,8,12,16 */
    uint8_t  subband;       /*4,8*/
    uint8_t  alloc;         /*0(Loudness),1(SNR)*/
    uint8_t  min_bitpool;   /* 2 */
    uint8_t  max_bitpool;   /*53(44.1khz),51 (48khz) */
    uint16_t mtu;
    uint32_t bitrate;
}tA2DP_SBC_CODEC;

typedef struct {
    uint8_t  codec_type;
    uint8_t  dev_idx;
    uint32_t vendor_id;
    uint16_t codec_id;
    uint16_t sampling_rate;
    uint8_t  chnl;
    uint8_t  cp;
    uint16_t mtu;
    uint32_t bitrate;
}tA2DP_APTX_CODEC;

#if 0
#ifdef USE_RB5_AUDIO_HAL
typedef struct {
    uint16_t sampling_rate;
    uint8_t  channels;
    uint32_t bitrate;
    uint32_t bits_per_sample;
} audio_aptx_default_config;

typedef struct {
    uint32_t subband;        /* 4, 8 */
    uint32_t blk_len;        /* 4, 8, 12, 16 */
    uint16_t sampling_rate;  /*44.1khz,48khz*/
    uint8_t  channels;       /*0(Mono),1(Dual_mono),2(Stereo),3(JS)*/
    uint8_t  alloc;          /*0(Loudness),1(SNR)*/
    uint8_t  min_bitpool;    /* 2 */
    uint8_t  max_bitpool;    /*53(44.1khz),51 (48khz) */
    uint32_t bitrate;        /* 320kbps to 512kbps */
    uint32_t bits_per_sample;
} audio_sbc_encoder_config;

typedef struct {
    uint32_t enc_mode; /* LC, SBR, PS */
    uint16_t format_flag; /* RAW, ADTS */
    uint16_t channels; /* 1-Mono, 2-Stereo */
    uint32_t sampling_rate;
    uint32_t bitrate;
    uint32_t bits_per_sample;
} audio_aac_encoder_config;

/* Information about BT APTX ADAPTIVE encoder configuration
 *  * This data is used between audio HAL module and
 *   * BT IPC library to configure DSP encoder
 *    * for HQ and LL mode
 *     */
typedef struct {
    uint8_t sampling_rate;
    uint8_t  channel_mode;
    uint16_t mtu;
    uint8_t min_sink_buffering_LL;
    uint8_t max_sink_buffering_LL;
    uint8_t min_sink_buffering_HQ;
    uint8_t max_sink_buffering_HQ;
    uint8_t min_sink_buffering_TWS;
    uint8_t max_sink_buffering_TWS;
    uint8_t TTP_LL_low;
    uint8_t TTP_LL_high;
    uint8_t TTP_HQ_low;
    uint8_t TTP_HQ_high;
    uint8_t TTP_TWS_low;
    uint8_t TTP_TWS_high;
    uint32_t bits_per_sample;
    uint16_t aptx_mode;
} audio_aptx_ad_config;

/* Information about BT APTX encoder configuration
 *  * This data is used between audio HAL module and
 *   * BT IPC library to configure DSP encoder
 *    * for TWS in dual mono scenario
 *     */
typedef struct {
    uint16_t sampling_rate;
    uint8_t  channels;
    uint32_t bitrate;
    uint8_t sync_mode;
    uint32_t bits_per_sample;
} audio_aptx_dual_mono_config;
#endif
#endif

typedef struct {
    /** Set to sizeof(bt_host_ipc_interface_t) */
    size_t          size;
    void (*a2dp_open_ctrl_path)(struct a2dp_stream_common *common);
    void (*a2dp_stream_common_init)(struct a2dp_stream_common *common);
    int (*start_audio_datapath)(struct a2dp_stream_common *common);
    int (*suspend_audio_datapath)(struct a2dp_stream_common *common, bool standby);
    int (*stop_audio_datapath)(struct a2dp_stream_common *common);
    int (*check_a2dp_stream_started)(struct a2dp_stream_common *common);
    int (*check_a2dp_ready)(struct a2dp_stream_common *common);
    int (*a2dp_read_audio_config)(struct a2dp_stream_common *common);
    int (*skt_read)(int fd,void *buf, size_t bytes);
    int (*skt_write)(int fd,const void *buf, size_t bytes);
    int (*skt_disconnect)(int fd);
    int (*a2dp_command)(struct a2dp_stream_common *common,char cmd);
    int (*audio_stream_open)(void);
    int (*audio_stream_close)(void);
    int (*audio_start_stream)(void);
    int (*audio_stop_stream)(void);
    int (*audio_suspend_stream)(void);
    void* (*audio_get_codec_config)(uint8_t *mcast, uint8_t *num_dev, audio_format_t *codec_type);
    void (*audio_handoff_triggered)(void);
    void (*clear_a2dpsuspend_flag)(void);
    void*(*audio_get_next_codec_config)(uint8_t idx, audio_format_t *codec_type);
    int (*audio_check_a2dp_ready)(void);
    uint16_t (*audio_get_a2dp_sink_latency)(void);
} bt_host_ipc_interface_t;

extern "C" int audio_stream_open(void);
extern "C" int audio_stream_close(void);
extern "C" int audio_start_stream(void);
extern "C" int audio_stop_stream(void);
extern "C" int audio_suspend_stream(void);
extern "C" void* audio_get_codec_config(uint8_t *mcast, uint8_t *num_dev, audio_format_t *codec_type);
extern "C" void audio_handoff_triggered(void);
extern "C" void clear_a2dpsuspend_flag(void);
extern "C" int audio_check_a2dp_ready(void);
extern "C" uint16_t audio_get_a2dp_sink_latency(void);
#endif
