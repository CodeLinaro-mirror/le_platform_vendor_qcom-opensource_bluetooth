SUBDIRS = hal

#exporting Headers
hal_includedir = $(includedir)/hardware/
hal_include_HEADERS  = $(top_srcdir)/hal/include/hardware/bt_obex_sock.h

#exporting hostIpc Headers
hostIpc_includedir = $(includedir)/
hostIpc_include_HEADERS  = $(top_srcdir)/bthost_ipc/bthost_avk_ipc.h

pkgconfigdir   = $(libdir)/pkgconfig
pkgconfig_DATA = bluetoothHost.pc
EXTRA_DIST     = $(pkgconfig_DATA)
