/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef ANDROID_INCLUDE_BT_AV_VENDOR_H
#define ANDROID_INCLUDE_BT_AV_VENDOR_H

#define BT_PROFILE_ADVANCED_AUDIO_VENDOR_ID "a2dp_vendor"
#define BT_PROFILE_ADVANCED_AUDIO_SINK_VENDOR_ID "a2dp_sink_vendor"

__BEGIN_DECLS

/** Vendor callback for connection priority of device for incoming connection
 * btav_connection_priority_t
 */
typedef void (* btav_connection_priority_vendor_callback)(bt_bdaddr_t *bd_addr);

/** Vendor callback for updating apps for A2dp multicast state.
 */
typedef void (* btav_is_multicast_enabled_vendor_callback)(int state);

/*
 * Vendor callback for audio focus request to be used only in
 * case of A2DP Sink. This is required because we are using
 * AudioTrack approach for audio data rendering.
 */
typedef void (* btav_audio_focus_request_vendor_callback)(bt_bdaddr_t *bd_addr);

/** BT-AV Vendor callback structure. */
typedef struct {
    /** set to sizeof(btav_vendor_callbacks_t) */
    size_t      size;
    btav_connection_priority_vendor_callback connection_priority_vendor_cb;
    btav_is_multicast_enabled_vendor_callback multicast_state_vendor_cb;
    btav_audio_focus_request_vendor_callback audio_focus_request_vendor_cb;
} btav_vendor_callbacks_t;

/** Represents the standard BT-AV interface.
 *  Used for both the A2DP source and sink interfaces.
 */
typedef struct {
    /** set to sizeof(btav_vendor_interface_t) */
    size_t          size;
    /**
     * Register the BtAvVendorcallbacks
     */
    bt_status_t (*init_vendor)( btav_vendor_callbacks_t* callbacks , int max_a2dp_connections,
                        int a2dp_multicast_state);

    /** Send priority of device to stack*/
    void (*allow_connection_vendor)( int is_valid , bt_bdaddr_t *bd_addr);

    /** Sends Audio Focus State. */
    void  (*audio_focus_state_vendor)( int focus_state );

   /** Request PCM sample. */
   uint32_t  (*get_pcm_data_vendor)( uint8_t* data, uint32_t size );

   /** Closes the av vendor interface. */
   void  (*cleanup_vendor)( void );
} btav_vendor_interface_t;

__END_DECLS

#endif /* ANDROID_INCLUDE_BT_AV_VENDOR_H */
