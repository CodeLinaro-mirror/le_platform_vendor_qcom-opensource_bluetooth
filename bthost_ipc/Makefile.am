# sources and intermediate files are separated

AM_CFLAGS = -Wall \
        -Wundef \
        -Wstrict-prototypes \
        -Wno-trigraphs \
        -std=c++11 \
        -DHAS_NO_BDROID_BUILDCFG

AM_CPPFLAGS= \
        -I. \
        #-I${WORKSPACE}/vendor/qcom/opensource/system/bt \
        #-I${WORKSPACE}/vendor/qcom/opensource/system/bt/include \
        #-I${WORKSPACE}/vendor/qcom/opensource/system/bt/types \
        #-I${WORKSPACE}/vendor/qcom/opensource/system/bt/utils/include \
        #-I${WORKSPACE}/vendor/qcom/opensource/system/bt/audio_a2dp_hw/include \
        -I$(bdroid_C_INCLUDES)
#if !NEW_AUDIO_HAL
#AM_CPPFLAGS += \
#        -I${WORKSPACE}/frameworks/system/media/audio/include \
#        -I${WORKSPACE}/frameworks/include \
#        -I${WORKSPACE}/frameworks/include/media \
#        -I${WORKSPACE}/hardware/libhardware/include \
#       -I${WORKSPACE}/system/core/include
#endif

AM_CPPFLAGS += @LIBHARDWARE_CFLAGS@ @SYSTEM_BT_CFLAGS@

#exporting Headers
#hostIpc_includedir = $(includedir)/
#hostIpc_include_HEADERS  = $(top_srcdir)/bthost_ipc/bthost_avk_ipc.h

#pkgconfigdir   = $(libdir)/pkgconfig
#pkgconfig_DATA = bthost_ipc.pc
#EXTRA_DIST     = $(pkgconfig_DATA)

common_LDFLAGS = -lpthread  -lrt -llog @GLIB_LIBS@
common_CFLAGS =  $(AM_CFLAGS) -fPIC -D_GNU_SOURCE -DUSE_GLIB @GLIB_CFLAGS@

########BT_HOST_SOURCE_IPC########
ipc_sources = \
           bthost_ipc.cc

library_includedir = $(pkgincludedir)
lib_LTLIBRARIES =  libbthost_if.la
libbthost_if_la_SOURCES = $(ipc_sources)
libbthost_if_la_CPPFLAGS =$(AM_CPPFLAGS)    $(common_CFLAGS)
libbthost_if_la_CFLAGS = $(common_CFLAGS)
libbthost_if_la_LDFLAGS = $(common_LDFLAGS) -avoid-version

########BT_HOST_SINK_IPC########
ipc_avk_sources = \
           bthost_avk_ipc.cc

library_includedir = $(pkgincludedir)
lib_LTLIBRARIES +=  libbthost_if_sink.la
libbthost_if_sink_la_SOURCES = $(ipc_avk_sources)
libbthost_if_sink_la_CPPFLAGS =$(AM_CPPFLAGS)    $(common_CFLAGS)
libbthost_if_sink_la_CFLAGS = $(common_CFLAGS)
libbthost_if_sink_la_LDFLAGS = $(common_LDFLAGS) -avoid-version
