
lib_LTLIBRARIES = libbtvendorhal.la
libbtvendorhal_la_SOURCES = hardware.c

#Export headers
h_sources = $(srcdir)/include/hardware/bluetooth.h
h_sources += $(srcdir)/include/hardware/bt_av.h
h_sources += $(srcdir)/include/hardware/bt_common_types.h
h_sources += $(srcdir)/include/hardware/bt_gatt_client.h
h_sources += $(srcdir)/include/hardware/bt_gatt_server.h
h_sources += $(srcdir)/include/hardware/bt_gatt_types.h
h_sources += $(srcdir)/include/hardware/bt_gatt.h
h_sources += $(srcdir)/include/hardware/bt_hf_client.h
h_sources += $(srcdir)/include/hardware/bt_hf.h
h_sources += $(srcdir)/include/hardware/bt_hh.h
h_sources += $(srcdir)/include/hardware/bt_hl.h
h_sources += $(srcdir)/include/hardware/bt_mce.h
h_sources += $(srcdir)/include/hardware/bt_pan.h
h_sources += $(srcdir)/include/hardware/bt_rc.h
h_sources += $(srcdir)/include/hardware/bt_sdp.h
h_sources += $(srcdir)/include/hardware/bt_sock.h
h_sources += $(top_srcdir)/vhal/include/hardware/bt_av_vendor.h
h_sources += $(top_srcdir)/vhal/include/hardware/bt_hf_client_vendor.h
h_sources += $(top_srcdir)/vhal/include/hardware/bt_hf_vendor.h
h_sources += $(top_srcdir)/vhal/include/hardware/bt_hh_vendor.h
h_sources += $(top_srcdir)/vhal/include/hardware/bt_rc_vendor.h
h_sources += $(top_srcdir)/vhal/include/hardware/vendor.h

libbtvendorhal_la_includedir = $(includedir)/hardware/
libbtvendorhal_la_include_HEADERS = $(h_sources)

libbtvendorhal_la_CFLAGS = -Iinclude -DLINUX_ENABLED
libbtvendorhal_la_LDFLAGS = -lcutils -llog -ldl
