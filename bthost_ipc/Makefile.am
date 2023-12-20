# sources and intermediate files are separated

AM_CFLAGS = -Wall \
        -Wundef \
        -Wstrict-prototypes \
        -Wno-trigraphs \
        -std=c++11 \
        -DHAS_NO_BDROID_BUILDCFG

AM_CPPFLAGS= \
        -I. \
        -I$(bdroid_C_INCLUDES)

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
