lib_LTLIBRARIES = libbtvendorhal.la
libbtvendorhal_la_SOURCES = hardware.cc
$(info $$WORKSPACE val is [${WORKSPACE}])
libbtvendorhal_la_CFLAGS = -DLINUX_ENABLED -fpermissive
libbtvendorhal_la_CFLAGS += -I${WORKSPACE}/bluetooth/stack/system/bt/include
libbtvendorhal_la_CFLAGS += -I${WORKSPACE}/bluetooth/bt_audio/hal/include
libbtvendorhal_la_CPPFLAGS = -DLINUX_ENABLED -fpermissive -D__GLIBC__
libbtvendorhal_la_CPPFLAGS += -I${WORKSPACE}/bluetooth/stack/system/bt/include
libbtvendorhal_la_CPPFLAGS += -I${WORKSPACE}/bluetooth/stack/system/bt
libbtvendorhal_la_CPPFLAGS += -I${WORKSPACE}/bluetooth/btapp/bt-app/osi
libbtvendorhal_la_CPPFLAGS += -I${WORKSPACE}/bluetooth/bt_audio/hal/include
libbtvendorhal_la_LDFLAGS = -ldl -D__GLIBC__
