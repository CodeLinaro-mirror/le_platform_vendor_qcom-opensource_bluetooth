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
/*      bthost_avk_ipc.cc
 *
 *  Description:   Implements IPC interface between HAL and BT host
 *
 *****************************************************************************/

#include <errno.h>
#include <inttypes.h>
#include <pthread.h>
#include <stdint.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/poll.h>
#include <sys/errno.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <system/audio.h>
//#include <audio.h>

#include <hardware/hardware.h>
#include "bthost_ipc.h"
#include "bthost_avk_ipc.h"
#include "bt_utils.h"
#include "osi/include/hash_map_utils.h"
#include "osi/include/log.h"
#include "osi/include/osi.h"
#include "osi/include/socket_utils/sockets.h"

#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG "bthost_avk_ipc"
#include "osi/include/log.h"

#ifdef USE_GLIB
#include <glib.h>
#define strlcpy g_strlcpy
#endif

/*****************************************************************************
**  Constants & Macros
******************************************************************************/
#define STREAM_START_MAX_RETRY_COUNT 80 /* Retry for 8sec to address IOT issue*/
#define CTRL_CHAN_RETRY_COUNT 3
#define USEC_PER_SEC 1000000L
#define SOCK_SEND_TIMEOUT_MS 2000  /* Timeout for sending */
#define SOCK_RECV_TIMEOUT_MS 5000  /* Timeout for receiving */
#define EXPORT_SYMBOL   __attribute__((visibility("default")))
// set WRITE_POLL_MS to 0 for blocking sockets, nonzero for polled non-blocking sockets
#define WRITE_POLL_MS 20

#define CASE_RETURN_STR(const) case const: return #const;

#define FNLOG() LOG_VERBOSE(" %s", __FUNCTION__);
#define DEBUG(fmt, ...) \
  LOG_VERBOSE(" %s: " fmt, __FUNCTION__, ##__VA_ARGS__)
#define INFO(fmt, ...) LOG_INFO(" %s: " fmt, __FUNCTION__, ##__VA_ARGS__)
#define WARN(fmt, ...) LOG_WARN(" %s: " fmt, __FUNCTION__, ##__VA_ARGS__)
#define ERROR(fmt, ...) LOG_ERROR(" %s: " fmt, __FUNCTION__, ##__VA_ARGS__)

#define ASSERTC(cond, msg, val) if (!(cond)) {ERROR("### ASSERT : %s line %d %s (%d) ###", __FILE__, __LINE__, msg, val);}

struct a2dp_avk_stream_common {
  pthread_mutex_t lock;  // See note below on mutex acquisition order.
  int ctrl_fd;
  struct a2dp_config cfg;
  a2dp_avk_state_t state;
  size_t buffer_sz;
  uint8_t codec_cfg[MAX_CODEC_CFG_SIZE];
};

typedef struct {
    uint32_t bitrate;
    uint32_t bitrate_mode; // 0 - unknown, 1 - avg, 2 - max
    uint32_t mtu;
} audio_sink_buffer_config_t;

typedef struct {
    audio_sink_buffer_config_t snk_buffer;
    uint32_t      audio_object_type; /* LC */
    uint16_t      aac_fmt_flag; /* LATM*/
    uint16_t      channels; /* Stereo */
    uint32_t      sampling_rate; /* 8k, 11.025k, 12k, 16k, 22.05k, 24k, 32k,
                                  44.1k, 48k, 64k, 88.2k, 96k */
    uint32_t      bits_per_sample;
} audio_aac_decoder_config;

typedef struct {
    audio_sink_buffer_config_t snk_buffer;
    uint16_t      sampling_rate;
    uint8_t      channels;
    uint32_t      bits_per_sample;
} audio_sbc_decoder_config;

typedef struct {
    uint32_t sampling_rate;
    uint8_t  channel_mode;
} audio_aptx_ad_decoder_config;

typedef struct {
    uint32_t sampling_rate;/*48k and 44.1k*/
    uint8_t channel_mode;/*Stereo*/
} audio_aptx_decoder_config;

typedef struct {
    uint32_t sampling_rate;/*48k and 44.1k*/
    uint8_t channel_mode;/*Stereo*/
} audio_aptx_hd_decoder_config;

/*****************************************************************************
**  Local type definitions
******************************************************************************/

struct a2dp_avk_stream_common audio_stream;

/*****************************************************************************
**  Static functions
******************************************************************************/

audio_aptx_decoder_config aptx_codec;
audio_aac_decoder_config aac_codec;
audio_sbc_decoder_config sbc_codec;
audio_aptx_ad_decoder_config aptxad_codec;
audio_aptx_hd_decoder_config aptx_hd_codec;

/*****************************************************************************
**  Externs
******************************************************************************/

/*****************************************************************************
**  Functions
******************************************************************************/
void a2dp_avk_open_ctrl_path(struct a2dp_avk_stream_common *common);
/*****************************************************************************
**   Miscellaneous helper functions
******************************************************************************/
static const char* dump_a2dp_avk_ctrl_event(char event)
{
    switch(event)
    {
        CASE_RETURN_STR(A2DP_AVK_CTRL_CMD_NONE)
        CASE_RETURN_STR(A2DP_AVK_CTRL_CMD_CHECK_SOCKET)
        CASE_RETURN_STR(A2DP_AVK_CTRL_CMD_CHECK_READY)
        CASE_RETURN_STR(A2DP_AVK_CTRL_CMD_START_CAPTURE)
        CASE_RETURN_STR(A2DP_AVK_CTRL_CMD_STOP_CAPTURE)
        CASE_RETURN_STR(A2DP_AVK_CTRL_SESSION_SETUP_COMPLETE)
        CASE_RETURN_STR(A2DP_AVK_CTRL_GET_CODEC_CONFIG)
        default:
            return "UNKNOWN MSG ID";
    }
}

/* logs timestamp with microsec precision
   pprev is optional in case a dedicated diff is required */
static void ts_log(char *tag, int val, struct timespec *pprev_opt)
{
    struct timespec now;
    static struct timespec prev = {0,0};
    unsigned long long now_us;
    unsigned long long diff_us;
    //UNUSED(tag);
    //UNUSED(val);

    clock_gettime(CLOCK_MONOTONIC, &now);

    now_us = now.tv_sec*USEC_PER_SEC + now.tv_nsec/1000;

    if (pprev_opt)
    {
        diff_us = (now.tv_sec - prev.tv_sec) * USEC_PER_SEC + (now.tv_nsec - prev.tv_nsec)/1000;
        *pprev_opt = now;
        DEBUG("[%s] ts %08lld, *diff %08lld, val %d", tag, now_us, diff_us, val);
    }
    else
    {
        diff_us = (now.tv_sec - prev.tv_sec) * USEC_PER_SEC + (now.tv_nsec - prev.tv_nsec)/1000;
        prev = now;
        DEBUG("[%s] ts %08lld, diff %08lld, val %d", tag, now_us, diff_us, val);
    }
}


static const char* dump_a2dp_hal_state(int event)
{
    switch(event)
    {
        CASE_RETURN_STR(AUDIO_A2DP_AVK_STATE_STARTING_CAPTURE)
        CASE_RETURN_STR(AUDIO_A2DP_AVK_STATE_STARTED_CAPTURE)
        CASE_RETURN_STR(AUDIO_A2DP_AVK_STATE_STOPPING)
        CASE_RETURN_STR(AUDIO_A2DP_AVK_STATE_STOPPED)
        CASE_RETURN_STR(AUDIO_A2DP_AVK_STATE_SESSION_COMPLETE)
        default:
            return "UNKNOWN STATE ID";
    }
}
static void* a2dp_codec_parser(uint8_t *codec_cfg, audio_format_t *codec_type)
{
    char byte,len;
    uint8_t *p_cfg = codec_cfg;
    INFO("%s",__func__);
    if (codec_cfg[CODEC_AVK_OFFSET] == CODEC_TYPE_PCM)
    {
        *codec_type = AUDIO_FORMAT_PCM_16_BIT;
        //For the time being Audio does not require any param to be passed for PCM so returning null
        return NULL;
    }
    else if (codec_cfg[CODEC_AVK_OFFSET] == CODEC_TYPE_SBC)
    {
        uint16_t sbc_samp_freq;
        memset(&sbc_codec,0,sizeof(audio_sbc_decoder_config));
        p_cfg++;//skip length
        p_cfg++;//skip media type
        p_cfg++;//skip codec type
        sbc_samp_freq = *p_cfg++;
        sbc_samp_freq |= (*p_cfg++ << 8);
        sbc_codec.channels = *p_cfg++;
        sbc_codec.snk_buffer.bitrate = *p_cfg++;
        sbc_codec.snk_buffer.bitrate |= (*p_cfg++ << 8);
        sbc_codec.snk_buffer.bitrate |= (*p_cfg++ << 16);
        sbc_codec.snk_buffer.bitrate |= (*p_cfg++ << 24);

        sbc_codec.snk_buffer.bitrate_mode = *p_cfg++;

        sbc_codec.snk_buffer.mtu = *p_cfg++;
        sbc_codec.snk_buffer.mtu |= (*p_cfg++ << 8);

        sbc_codec.bits_per_sample = *p_cfg;

        sbc_codec.sampling_rate = sbc_samp_freq;
        *codec_type = AUDIO_FORMAT_SBC;
        INFO("Codectype: SBC, samp freq: %d, channels: %d ",sbc_samp_freq,sbc_codec.channels);
        return ((void *)(&sbc_codec));
    } else if (codec_cfg[CODEC_AVK_OFFSET] == CODEC_TYPE_AAC)
    {
        uint16_t aac_samp_freq = 0;
        uint32_t aac_bit_rate = 0;
        memset(&aac_codec,0,sizeof(audio_aac_decoder_config));
        len = *p_cfg++;
        p_cfg++;//skip media type
        p_cfg++;//skip codec type
        byte = *p_cfg++;
        /*switch (byte & A2D_AAC_IE_OBJ_TYPE_MSK)
        {
            case A2D_AAC_IE_OBJ_TYPE_MPEG_2_AAC_LC:
                aac_codec.audio_object_type = AUDIO_FORMAT_AAC_SUB_LC;
                break;
            case A2D_AAC_IE_OBJ_TYPE_MPEG_4_AAC_LC:
                aac_codec.audio_object_type = AUDIO_FORMAT_AAC_SUB_LC;
                break;
            case A2D_AAC_IE_OBJ_TYPE_MPEG_4_AAC_LTP:
                aac_codec.audio_object_type = AUDIO_FORMAT_AAC_SUB_LTP;
                break;
            case A2D_AAC_IE_OBJ_TYPE_MPEG_4_AAC_SCA:
                aac_codec.audio_object_type = AUDIO_FORMAT_AAC_SUB_SCALABLE;
                break;
            default:
                ERROR("Unknown encoder mode");
        }*/
        //USE 0 (AAC_LC) as hardcoded value till Audio
        //define constants
        aac_codec.audio_object_type = 0;
        //USE LOAS(1) or LATM(4) hardcoded values till
        //Audio define proper constants
        aac_codec.aac_fmt_flag = 4;
        byte = *p_cfg++;
        aac_samp_freq = byte << 8; //1st byte of sample_freq
        byte = *p_cfg++;
        aac_samp_freq |= byte & 0x00F0; //1st nibble of second byte of samp_freq
        switch (aac_samp_freq) {
            case 0x8000: aac_codec.sampling_rate = 8000; break;
            case 0x4000: aac_codec.sampling_rate = 11025; break;
            case 0x2000: aac_codec.sampling_rate = 12000; break;
            case 0x1000: aac_codec.sampling_rate = 16000; break;
            case 0x0800: aac_codec.sampling_rate = 22050; break;
            case 0x0400: aac_codec.sampling_rate = 24000; break;
            case 0x0200: aac_codec.sampling_rate = 32000; break;
            case 0x0100: aac_codec.sampling_rate = 44100; break;
            case 0x0080: aac_codec.sampling_rate = 48000; break;
            case 0x0040: aac_codec.sampling_rate = 64000; break;
            case 0x0020: aac_codec.sampling_rate = 88200; break;
            case 0x0010: aac_codec.sampling_rate = 96000; break;
            default:
                ERROR("Invalid sample_freq: %x", aac_samp_freq);
        }
        INFO(" aac samp freq: %d",aac_codec.sampling_rate);
        switch (byte & A2D_AAC_IE_CHANNELS_MSK)
        {
            case A2D_AAC_IE_CHANNELS_1:
                 aac_codec.channels = 1;
                 break;
            case A2D_AAC_IE_CHANNELS_2:
                 aac_codec.channels = 2;
                 break;
            default:
                 ERROR("Unknow channel mode");
        }
        INFO("channels :%d",aac_codec.channels);
        byte = *p_cfg++;
        aac_bit_rate = (byte << 16) & (0x7F << 16);
        byte = *p_cfg++;
        aac_bit_rate |= (byte << 8) & (0xFF << 8);
        byte = *p_cfg++;
        aac_bit_rate |= byte & 0xFF;
        aac_codec.snk_buffer.bitrate = aac_bit_rate;
        aac_codec.snk_buffer.bitrate_mode = *p_cfg++;

        aac_codec.snk_buffer.mtu = *p_cfg++;
        aac_codec.snk_buffer.mtu |= (*p_cfg++ << 8);

        aac_codec.bits_per_sample = *p_cfg;

        *codec_type = AUDIO_FORMAT_AAC;
        INFO("AAC: Done copying full codec config");
        return ((void *)(&aac_codec));
    }
    else if (codec_cfg[CODEC_AVK_OFFSET] == NON_A2DP_CODEC_TYPE)
    {
        if (codec_cfg[VENDOR_ID_OFFSET - 1] == VENDOR_APTX &&
            codec_cfg[CODEC_ID_OFFSET - 1] == APTX_CODEC_ID)
        {
            INFO("AptX-classic codec");
            *codec_type = AUDIO_FORMAT_APTX;
            memset(&aptx_codec,0,sizeof(audio_aptx_decoder_config));
            len = *p_cfg++;//LOSC
            p_cfg++; // Skip media type
            len--;
            p_cfg++; //codec_type
            len--;
            p_cfg+=4;//skip vendor id
            len -= 4;
            p_cfg += 2; //skip codec id
            len -= 2;
            byte = *p_cfg++;
            len--;
            switch (byte & A2D_APTX_SAMP_FREQ_MASK)
            {
                case A2D_APTX_SAMP_FREQ_48:
                     aptx_codec.sampling_rate = 48000;
                     INFO(" aptx samp freq: %d",aptx_codec.sampling_rate);
                     break;
                case A2D_APTX_SAMP_FREQ_44:
                     aptx_codec.sampling_rate = 44100;
                     INFO(" aptx samp freq: %d",aptx_codec.sampling_rate);
                     break;
                default:
                     ERROR("Unknown sampling rate");
            }
            switch (byte & A2D_APTX_CHAN_MASK)
            {
                case A2D_APTX_CHAN_STEREO:
                     aptx_codec.channel_mode = 2;
                     break;
                case A2D_APTX_CHAN_MONO:
                     ERROR("Mono is not supported");
                     aptx_codec.channel_mode = 1;
                     break;
                default:
                     ERROR("Unknown channel mode");
            }
            INFO(" aptx channels : %d",aptx_codec.channel_mode);

            INFO("APTx: Done copying full codec config");
            return ((void *)&aptx_codec);
        }
        if (codec_cfg[VENDOR_ID_OFFSET - 1] == VENDOR_APTX_HD &&
            codec_cfg[CODEC_ID_OFFSET - 1] == APTX_HD_CODEC_ID)
        {
            INFO("AptX-HD codec");
            *codec_type = AUDIO_FORMAT_APTX_HD;
            memset(&aptx_hd_codec,0,sizeof(audio_aptx_hd_decoder_config));
            len = *p_cfg++;//LOSC
            p_cfg++; // Skip media type
            len--;
            p_cfg++; //codec_type
            len--;
            p_cfg+=4;//skip vendor id
            len -= 4;
            p_cfg += 2; //skip codec id
            len -= 2;
            byte = *p_cfg++;
            len--;
            switch (byte & A2D_APTX_HD_SAMP_FREQ_MASK)
            {
                case A2D_APTX_HD_SAMP_FREQ_48:
                     aptx_hd_codec.sampling_rate = 48000;
                     INFO(" aptx_hd samp freq: %d",aptx_hd_codec.sampling_rate);
                     break;
                case A2D_APTX_HD_SAMP_FREQ_44:
                     aptx_hd_codec.sampling_rate = 44100;
                     INFO(" aptx_hd samp freq: %d",aptx_hd_codec.sampling_rate);
                     break;
                default:
                     ERROR("Unknown sampling rate");
            }
            switch (byte & A2D_APTX_HD_CHAN_MASK)
            {
                case A2D_APTX_HD_CHAN_STEREO:
                     aptx_hd_codec.channel_mode = 2;
                     break;
                case A2D_APTX_HD_CHAN_MONO:
                     ERROR("Mono is not supported");
                     aptx_hd_codec.channel_mode = 1;
                     break;
                default:
                     ERROR("Unknown channel mode");
            }
            INFO("aptx_hd channels : %d",aptx_hd_codec.channel_mode);

            INFO("APTX_HD: Done copying full codec config");
            return ((void *)&aptx_hd_codec);
        }
        if (codec_cfg[VENDOR_ID_OFFSET - 1] == VENDOR_APTX_ADAPTIVE &&
            codec_cfg[CODEC_ID_OFFSET - 1] == APTX_ADAPTIVE_CODEC_ID)
        {
            INFO("AptX-Adaptive codec");
            *codec_type = AUDIO_FORMAT_APTX_ADAPTIVE;

            memset(&aptxad_codec, 0, sizeof(audio_aptx_ad_decoder_config));
            len = *p_cfg++;//LOSC
            p_cfg++; // Skip media type
            len--;
            p_cfg++; //codec_type
            len--;
            p_cfg += 4;//skip vendor id
            len -= 4;
            p_cfg += 2; //skip codec id
            len -= 2;

            switch(*p_cfg++ & A2D_APTX_ADAPTIVE_SAMP_FREQ_MASK)
            {
                case A2DP_APTX_ADAPTIVE_SAMPLERATE_44100:
                     aptxad_codec.sampling_rate = 1;
                     break;
                case A2DP_APTX_ADAPTIVE_SAMPLERATE_48000:
                     aptxad_codec.sampling_rate = 0;
                     break;
                default:
                     ERROR("Unknown sampling rate");
            }
            len--;

            switch(*p_cfg++ & A2D_APTX_ADAPTIVE_CHAN_MASK)
            {
                case A2DP_APTX_ADAPTIVE_CHANNELS_MONO:
                     aptxad_codec.channel_mode = 1;
                     break;
                case A2DP_APTX_ADAPTIVE_CHANNELS_TWS_MONO:
                     aptxad_codec.channel_mode = 2;
                     break;
                case A2DP_APTX_ADAPTIVE_CHANNELS_JOINT_STEREO:
                     aptxad_codec.channel_mode = 0;
                     break;
                case A2DP_APTX_ADAPTIVE_CHANNELS_TWS_STEREO:
                     aptxad_codec.channel_mode = 4;
                     break;
                default:
                     ERROR("Unknown channel id");
            }
            len--;
            len -= 6; //skip latency info

            p_cfg += 3; // ignoring eoc bits
            len -= 3;
            p_cfg += APTX_ADAPTIVE_RESERVED_BITS;
            len -= APTX_ADAPTIVE_RESERVED_BITS;
            INFO("%s: ## aptXAdaptive ## sampleRate 0x%x", __func__, aptxad_codec.sampling_rate);
            INFO("%s: ## aptXAdaptive ## channelMode 0x%x", __func__, aptxad_codec.channel_mode);

            if(len == 0)
                INFO("%s: Aptx AD: codec config copied", __func__);
            else
                INFO("%s: Aptx AD: codec config length error: %d", __func__, len);

            return ((void *)&aptxad_codec);
        }
    }
    return NULL;
}
/*****************************************************************************
**
**   bluedroid stack adaptation
**
*****************************************************************************/

static int skt_connect(char *path, size_t buffer_sz)
{
    int ret;
    int skt_fd;
    struct sockaddr_un remote;
    int len;

    INFO("connect to %s (sz %zu)", path, buffer_sz);

    skt_fd = socket(AF_LOCAL, SOCK_STREAM, 0);
    memset(&remote, 0, sizeof(remote));
    remote.sun_family = AF_LOCAL;
    strlcpy(remote.sun_path, path, sizeof(remote.sun_path));

    if(connect(skt_fd, (struct sockaddr*)&remote, sizeof(remote)) < 0)
    {
        ERROR("failed to connect (%s)", strerror(errno));
        close(skt_fd);
        return -1;
    }

    len = buffer_sz;
    ret = setsockopt(skt_fd, SOL_SOCKET, SO_SNDBUF, (char*)&len, (int)sizeof(len));
    if (ret < 0)
        ERROR("setsockopt failed (%s)", strerror(errno));

    ret = setsockopt(skt_fd, SOL_SOCKET, SO_RCVBUF, (char*)&len, (int)sizeof(len));
    if (ret < 0)
        ERROR("setsockopt failed (%s)", strerror(errno));

    INFO("connected to stack fd = %d", skt_fd);

    return skt_fd;
}

static int skt_disconnect(int fd)
{
    INFO("fd %d", fd);

    if (fd != AUDIO_SKT_DISCONNECTED)
    {
        shutdown(fd, SHUT_RDWR);
        close(fd);
    }
    return 0;
}



/*****************************************************************************
**
**  AUDIO CONTROL PATH
**
*****************************************************************************/

int a2dp_avk_ctrl_receive(struct a2dp_avk_stream_common *common, void* buffer, int length)
{
    ssize_t ret;
    int i;

    INFO("%s num_bytes_read (%d)", __func__, length);
    for (i = 0;; i++) {
        TEMP_FAILURE_RETRY(ret = recv(common->ctrl_fd, buffer, length, MSG_NOSIGNAL));
        INFO("%s bytes_read ret = (%d)", __func__, ret);
        if (ret > 0) {
            break;
        }
        if (ret == 0) {
            ERROR("ack failed: peer closed");
            break;
        }
        if (errno != EWOULDBLOCK && errno != EAGAIN) {
            ERROR("ack failed: error(%s)", strerror(errno));
            break;
        }
        if (i == (CTRL_CHAN_RETRY_COUNT - 1)) {
            ERROR("ack failed: max retry count");
            break;
        }
        INFO("ack failed (%s), retrying", strerror(errno));
    }
    if (ret <= 0) {
        skt_disconnect(common->ctrl_fd);
        common->ctrl_fd = AUDIO_SKT_DISCONNECTED;
    }
    return ret;
}

int a2dp_avk_command(struct a2dp_avk_stream_common *common, char cmd)
{
    char ack;

    INFO("A2DP COMMAND %s", dump_a2dp_avk_ctrl_event(cmd));

    if (common->ctrl_fd == AUDIO_SKT_DISCONNECTED) {
        INFO("recovering from previous error");
        a2dp_avk_open_ctrl_path(common);
        if (common->ctrl_fd == AUDIO_SKT_DISCONNECTED) {
            ERROR("failure to open ctrl path");
            return -1;
        }
    }

    /* send command */
    ssize_t sent;
    TEMP_FAILURE_RETRY(sent = send(common->ctrl_fd, &cmd, 1, MSG_NOSIGNAL));
    if (sent == -1)
    {
        ERROR("cmd failed (%s)", strerror(errno));
        skt_disconnect(common->ctrl_fd);
        common->ctrl_fd = AUDIO_SKT_DISCONNECTED;
        return -1;
    }

    /* wait for ack byte */
    if (a2dp_avk_ctrl_receive(common, &ack, 1) < 0) {
        ERROR("A2DP COMMAND %s: no ACK", dump_a2dp_avk_ctrl_event(cmd));
        return -1;
    }

    INFO("A2DP COMMAND %s DONE STATUS %d", dump_a2dp_avk_ctrl_event(cmd), ack);

    if (ack == A2DP_CTRL_ACK_INCALL_FAILURE)
        return ack;
    if (ack != A2DP_CTRL_ACK_SUCCESS) {
        ERROR("A2DP COMMAND %s error %d", dump_a2dp_avk_ctrl_event(cmd), ack);
        return -1;
    }
    return 0;
}

int check_a2dp_avk_socket_ready(struct a2dp_avk_stream_common *common)
{
    INFO("state %s", dump_a2dp_hal_state(common->state));
    if (a2dp_avk_command(common, A2DP_AVK_CTRL_CMD_CHECK_SOCKET) < 0)
    {
        ERROR("check a2dp ready failed");
        return -1;
    }
    return 0;
}

int a2dp_avk_read_codec_config(struct a2dp_avk_stream_common *common)
{
    char cmd,ack;
    int i,len = 0;
    uint8_t *p_codec_cfg = common->codec_cfg;
    INFO("%s",__func__);
    memset(p_codec_cfg,0,MAX_CODEC_CFG_SIZE);

    if (a2dp_avk_command(&audio_stream, A2DP_AVK_CTRL_GET_CODEC_CONFIG) != 0)
    {
        INFO("%s: FAIL",__func__);
        return -1;
    }

    if ((a2dp_avk_ctrl_receive(common, &len, 1) < 0) ||
        (len <= 0) || (len > MAX_CODEC_CFG_SIZE))
        return -1;
    if (a2dp_avk_ctrl_receive(common, p_codec_cfg, len) < 0)
        return -1;

    INFO("got codec config");

    for (i=0;i<len;i++)
         INFO("code_config[%d] = %d ", i,*p_codec_cfg++);

    return 0;
}

void a2dp_avk_open_ctrl_path(struct a2dp_avk_stream_common *common)
{
    int i;

    /* retry logic to catch any timing variations on control channel */
    for (i = 0; i < CTRL_CHAN_RETRY_COUNT; i++)
    {
        /* connect control channel if not already connected */
        if ((common->ctrl_fd = skt_connect(A2DP_AVK_CTRL_PATH, common->buffer_sz)) > 0)
        {
            /* success, now check if stack is ready */
            if (check_a2dp_avk_socket_ready(common) == 0)
                break;

            ERROR("error : a2dp not ready, wait 250 ms and retry");
            usleep(250000);
            skt_disconnect(common->ctrl_fd);
            common->ctrl_fd = AUDIO_SKT_DISCONNECTED;
        }

        /* ctrl channel not ready, wait a bit */
        usleep(250000);
    }
}

/*****************************************************************************
**
** AUDIO DATA PATH
**
*****************************************************************************/

void a2dp_avk_stream_common_init(struct a2dp_avk_stream_common *common)
{
    pthread_mutexattr_t lock_attr;

    FNLOG();

    pthread_mutexattr_init(&lock_attr);
    pthread_mutexattr_settype(&lock_attr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&common->lock, &lock_attr);

    common->ctrl_fd = AUDIO_SKT_DISCONNECTED;
    common->state = AUDIO_A2DP_AVK_STATE_STOPPED;

    common->buffer_sz = AUDIO_STREAM_OUTPUT_BUFFER_SZ;
    /* manages max capacity of socket pipe */
}

int start_audio_avk_datapath(struct a2dp_avk_stream_common *common)
{
    INFO("state %d", common->state);

    #ifdef BT_AUDIO_SYSTRACE_LOG
    char trace_buf[512];
    #endif

    INFO("state %s", dump_a2dp_hal_state(common->state));

    a2dp_avk_state_t oldstate = common->state;
    common->state = AUDIO_A2DP_AVK_STATE_STARTING_CAPTURE;

    int a2dp_status = a2dp_avk_command(common, A2DP_AVK_CTRL_CMD_START_CAPTURE);
#ifdef BT_AUDIO_SYSTRACE_LOG
    snprintf(trace_buf, 32, "start_audio_data_path:");
    if (PERF_SYSTRACE)
    {
        ATRACE_BEGIN(trace_buf);
    }
#endif

    #ifdef BT_AUDIO_SYSTRACE_LOG
    if (PERF_SYSTRACE)
    {
        ATRACE_END();
    }
    #endif
    if (a2dp_status < 0)
    {
        ERROR("%s Audiopath start failed (status %d)", __func__, a2dp_status);
        goto error;
    }
    else if (a2dp_status == A2DP_CTRL_ACK_INCALL_FAILURE)
    {
        ERROR("%s Audiopath start failed - in call, move to suspended", __func__);
        goto error;
    }
    common->state = AUDIO_A2DP_AVK_STATE_STARTED_CAPTURE;

    return 0;
error:
    common->state = oldstate;
    return -1;
}

int suspend_audio_avk_datapath(struct a2dp_avk_stream_common *common)
{
    a2dp_avk_state_t oldstate = common->state;
    INFO("state %s", dump_a2dp_hal_state(common->state));

    if (common->ctrl_fd == AUDIO_SKT_DISCONNECTED)
        return -1;

    common->state = AUDIO_A2DP_AVK_STATE_STOPPING;

    if (a2dp_avk_command(common, A2DP_AVK_CTRL_CMD_STOP_CAPTURE) < 0)
        return -1;

    common->state = AUDIO_A2DP_AVK_STATE_STOPPED;

    return 0;
}

int audio_sink_start_capture()
{
    int i;
    a2dp_avk_stream_common_init(&audio_stream);
    pthread_mutex_lock(&audio_stream.lock);
    a2dp_avk_open_ctrl_path(&audio_stream);
    INFO("%s: state = %s",__func__,dump_a2dp_hal_state(audio_stream.state));

    for (i = 0; i < STREAM_START_MAX_RETRY_COUNT; i++)
    {
        if (start_audio_avk_datapath(&audio_stream) == 0)
        {
            INFO("a2dp stream started successfully");
            break;
        }
        if (audio_stream.ctrl_fd == AUDIO_SKT_DISCONNECTED)
        {
            INFO("control path is disconnected");
            break;
        }
        INFO("%s: a2dp stream not started,wait 100mse & retry", __func__);
        usleep(100000);
    }
    if (audio_stream.state != AUDIO_A2DP_AVK_STATE_STARTED_CAPTURE)
    {
        ERROR("Failed to start a2dp stream");
        pthread_mutex_unlock(&audio_stream.lock);
        return -1;
    }
    pthread_mutex_unlock(&audio_stream.lock);
    return 0;
}

int audio_sink_stop_capture()
{
    int ret = -1;
    INFO("%s",__func__);
    pthread_mutex_lock(&audio_stream.lock);
    if (suspend_audio_avk_datapath(&audio_stream) == 0)
    {
        INFO("audio stop stream successful");
        ret = 0;
    }
    skt_disconnect(audio_stream.ctrl_fd);
    audio_stream.ctrl_fd = AUDIO_SKT_DISCONNECTED;
    audio_stream.state = AUDIO_A2DP_AVK_STATE_STOPPED;
    pthread_mutex_unlock(&audio_stream.lock);
    return ret;
}

void* audio_get_decoder_config(audio_format_t *codec_type)
{
    INFO("%s: state = %s",__func__,dump_a2dp_hal_state(audio_stream.state));

    pthread_mutex_lock(&audio_stream.lock);
    if (a2dp_avk_read_codec_config(&audio_stream) == 0)
    {
        pthread_mutex_unlock(&audio_stream.lock);
        return (a2dp_codec_parser(&audio_stream.codec_cfg[0], codec_type));
    }
    pthread_mutex_unlock(&audio_stream.lock);
    return NULL;
}

int audio_sink_check_a2dp_ready()
{
    INFO("%s: state %s", __func__, dump_a2dp_hal_state(audio_stream.state));
    pthread_mutex_lock(&audio_stream.lock);
    if (a2dp_avk_command(&audio_stream, A2DP_AVK_CTRL_CMD_CHECK_READY) != 0)
    {
        INFO("%s: FAIL",__func__);
        pthread_mutex_unlock(&audio_stream.lock);
        return 0;
    }
    pthread_mutex_unlock(&audio_stream.lock);
    return 1;
}

int audio_sink_session_setup_complete(uint64_t latency)
{
    char cmd[9],ack;
    int i = 0;
    cmd[i++] = A2DP_AVK_CTRL_SESSION_SETUP_COMPLETE;
    cmd[i++] = (uint8_t)(latency & 0x00FF);
    cmd[i++] = (uint8_t)(((latency & 0xFF00) >> 8) & 0x00FF);
    cmd[i++] = (uint8_t)(((latency & 0xFF0000) >> 16) & 0x00FF);
    cmd[i++] = (uint8_t)(((latency & 0xFF000000) >> 24) & 0x00FF);
    cmd[i++] = (uint8_t)(((latency & 0xFF00000000) >> 32) & 0x00FF);
    cmd[i++] = (uint8_t)(((latency & 0xFF0000000000) >> 40) & 0x00FF);
    cmd[i++] = (uint8_t)(((latency & 0xFF000000000000) >> 48) & 0x00FF);
    cmd[i++] = (uint8_t)(((latency & 0xFF00000000000000) >> 56) & 0x00FF);

    INFO("%s state = %d ",__func__, audio_stream.state);
    if(audio_stream.state == AUDIO_A2DP_AVK_STATE_STARTED_CAPTURE)
    {
        pthread_mutex_lock(&audio_stream.lock);

        if(send(audio_stream.ctrl_fd,cmd, 9, MSG_NOSIGNAL) == -1)
        {
            ERROR("%s, cmd failed (%s)",__func__ ,strerror(errno));
            skt_disconnect(audio_stream.ctrl_fd);
            audio_stream.ctrl_fd = AUDIO_SKT_DISCONNECTED;
            pthread_mutex_unlock(&audio_stream.lock);
            return -1;
        }

        if (a2dp_avk_ctrl_receive(&audio_stream, &ack, 1) < 0)
        {
            INFO("%s: FAIL",__func__);
            pthread_mutex_unlock(&audio_stream.lock);
            return -1;
        }
        INFO("%s: recv_success ack = %d",__func__,ack);
        if (ack != A2DP_CTRL_ACK_SUCCESS)
        {
            ERROR("%s: Failed to get ack",__func__);
            pthread_mutex_unlock(&audio_stream.lock);
            return -1;
        }
        pthread_mutex_unlock(&audio_stream.lock);
        audio_stream.state = AUDIO_A2DP_AVK_STATE_SESSION_COMPLETE;
        INFO(" %s Done, State = %d ",__func__,audio_stream.state);
        return 1;
    }
    return -1;
}

//Entry point for dynamic lib
EXPORT_SYMBOL bt_host_avk_ipc_interface_t BTHOST_AVK_IPC_INTERFACE = {
    sizeof(bt_host_avk_ipc_interface_t),
    audio_sink_start_capture,
    audio_sink_stop_capture,
    audio_get_decoder_config,
    audio_sink_session_setup_complete,
    audio_sink_check_a2dp_ready
};
