bin_PROGRAMS = btconfig

btconfig_SOURCES = btconfig.c

direct_hidl_shell_CPPFLAGS = \
        -I$(top_srcdir)/btconfig

btconfig_CFLAGS = \
        -I$(top_srcdir)/include

btconfig_LDADD = \
        $(top_builddir)/hidl_client/libhidlclient.la \
        -lpthread \
        -lcutils \
        -lutils \
        -ldl
