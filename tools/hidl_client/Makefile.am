# sources and intermediate files are separated

AM_CFLAGS = -Wall \
        -Wundef \
        -Wstrict-prototypes \
        -Wno-trigraphs \
        -std=c++11 \
        -DHAS_NO_BDROID_BUILDCFG

AM_CPPFLAGS = \
        -Iinc \
        -I$(bdroid_C_INCLUDES) \
        -I${D}${includedir}

if MDM_ROME
AM_CFLAGS += -DBT_SOC_TYPE_ROME
AM_CPPFLAGS += -DBT_SOC_TYPE_ROME
else
if MDM_PRONTO
AM_CFLAGS += -DHCI_USE_MCT
AM_CPPFLAGS += -DHCI_USE_MCT
endif
endif

if MDM_CHEROKEE
AM_CFLAGS += -DBT_SOC_TYPE_CHEROKEE
AM_CPPFLAGS += -DBT_SOC_TYPE_CHEROKEE
endif

if MDM_HASTINGS
AM_CFLAGS += -DBT_SOC_TYPE_HASTINGS
AM_CPPFLAGS += -DBT_SOC_TYPE_HASTINGS
endif

if MDM_MOSELLE
AM_CFLAGS += -DBT_SOC_TYPE_MOSELLE
AM_CPPFLAGS += -DBT_SOC_TYPE_MOSELLE
endif

if MDM_HAMILTON
AM_CFLAGS += -DBT_SOC_TYPE_HAMILTON
AM_CPPFLAGS += -DBT_SOC_TYPE_HAMILTON
endif

if BTC_SUPPORT
AM_CFLAGS += -DBTC_INCLUDED=TRUE
AM_CPPFLAGS += -DBTC_INCLUDED=TRUE
endif

if SSR_CLEANUP_SUPPORT
AM_CFLAGS += -DSSR_CLEANUP=TRUE
endif

cpp_sources = \
        src/client_reader.cpp \
        src/hidl_client.cpp

if USING_HIDL
AM_CFLAGS += -DUSING_HIDL
AM_CPPFLAGS += -DUSING_HIDL
common_LDFLAGS = -lpthread -lrt
endif

if USING_SOCKET
AM_CFLAGS += -DUSING_SOCKET
AM_CPPFLAGS += -DUSING_SOCKET
common_LDFLAGS = -lpthread -lrt
endif

if USING_STATIC_LINK
AM_CFLAGS += -DUSING_STATIC_LINK
AM_CPPFLAGS += -DUSING_STATIC_LINK
common_LDFLAGS = -lpthread -lrt -lhidl -ldl -lresolv -lcutils -lutils
endif


common_CFLAGS =  $(AM_CFLAGS) -fPIC -D_GNU_SOURCE -DBTSNOOP_DEFAULT=TRUE -DBT_NET_DEBUG=TRUE
common_CPPFLAGS = $(common_CFLAGS) -DHAS_NO_BDROID_BUILDCFG -fPIC
library_includedir = $(pkgincludedir)\
library_include_HEADERS = $(h_sources)
lib_LTLIBRARIES = libhidlclient.la
hidlClient_includedir = $(includedir)/
hidlClient_include_HEADERS = $(srcdir)/inc/hidl_client.h
libhidlclient_la_CC = @CC@
libhidlclient_la_SOURCES = $(cpp_sources)
libhidlclient_la_CPPFLAGS = $(AM_CPPFLAGS) -std=c++11 $(common_CPPFLAGS) -g -O0 -DFTM
libhidlclient_la_CFLAGS = $(common_CFLAGS) -DFTM
libhidlclient_la_LDFLAGS := $(common_LDFLAGS) -shared
