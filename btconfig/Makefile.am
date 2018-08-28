# sources and intermediate files are separated

AM_CFLAGS = -Wall \
    -Wundef \
    -Wstrict-prototypes \
    -Wno-trigraphs \
    -std=c99 \
    -I ./include \
    -DLINUX_ENABLED \
    -I$(WORKSPACE)/system/core/include \
    -I$(WORKSPACE)/system/bt/hci/include

AM_CFLAGS += -DBT_SOC_TYPE_CHEROKEE -g

# HAL layer
c_sources = \
    btconfig.c

btconfig_CFLAGS = $(AM_CFLAGS) -fPIC -D_GNU_SOURCE
btconfig_LDADD = -lpthread -ldl -lcutils -lutils 
library_includedir = $(pkgincludedir)

bin_PROGRAMS = btconfig
btconfig_CC = @CC@
btconfig_SOURCES = $(c_sources)
