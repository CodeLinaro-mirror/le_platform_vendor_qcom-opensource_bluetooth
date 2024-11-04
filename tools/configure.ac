#                                               -*- Autoconf -*-

# configure.ac -- Autoconf script for diag
#

# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.68])
AC_INIT([hidl-client], [1.0.0])
AM_INIT_AUTOMAKE([foreign subdir-objects])
AM_MAINTAINER_MODE
#AC_CONFIG_SRCDIR([hidl_client/src/hidl_client.cpp])
AC_CONFIG_HEADER([config.h])
AC_CONFIG_MACRO_DIR([m4])
m4_define([LT_MAJOR], [1])
m4_define([LT_MINOR], [0])
m4_define([LT_PATCH], [0])

AC_SUBST([LT_VERSION], LT_MAJOR.LT_MINOR.LT_PATCH)
AC_SUBST([LT_VERSION_NUMBER], LT_MAJOR:LT_MINOR:LT_PATCH)
AM_PROG_LIBTOOL
# Checks for programs.
AC_PROG_CC
AM_PROG_CC_C_O
AC_PROG_LIBTOOL
AC_PROG_AWK
AC_PROG_CPP
AC_PROG_CXX
AC_PROG_INSTALL
AC_PROG_LN_S
AC_PROG_MAKE_SET
PKG_PROG_PKG_CONFIG

# Library configs
AC_ARG_WITH([common_includes],
      AC_HELP_STRING([--with-common-includes=@<:@dir@:>@],
         [Specify the location of the common headers]),
      [common_incdir=$withval],
      with_common_includes=no)

if test "x$with_common_includes" != "xno"; then
   CFLAGS="${CFLAGS} -I${common_incdir}"
fi

AC_ARG_WITH([hardware_include],
                  AC_HELP_STRING([--with-hardware-include=@<:@dir@:>@],
                                 [Specify the location of the hardware headers]),
                                 [hardware_incdir=$withval],
                                 with_hardware_include=no)

if test "x$with_hardware_include" != "xno"; then
       CPPFLAGS="${CPPFLAGS} -I${hardware_incdir}"
fi

AC_ARG_WITH([hardware_libraries],
                 AC_HELP_STRING([--with-hardware-libraries=@<:@dir@:>@],
                                     [Specify the location of the hardware libraries]),
                                     [hardware_libdir=$withval],
                                          with_hardware_libraries=no)

if test "x$with_hardware_libraries" != "xno"; then
    LDFLAGS="${LDFLAGS} -L${hardware_libdir}"
fi

AC_SUBST([CFLAGS])
AC_SUBST([CPPFLAGS])
AC_SUBST([CC])

# Checks for header files.
AC_CHECK_HEADERS([fcntl.h stdlib.h string.h sys/socket.h sys/time.h termios.h unistd.h])

# Build all components by default
#
# To add new components: add AC_ARG_ENABLE, AM_CONDITIONAL and update AC_OUTPUT
#
AC_ARG_ENABLE(target,
  [AS_HELP_STRING([--enable-target=TARGET], [Specify the target product to build])],
  [TARGET=$enableval],
  [TARGET=none]
)

AC_ARG_ENABLE(rome,
  [AS_HELP_STRING([--enable-rome=ROME], [Specify the rome product to build])],
  [ROME=$enableval],
  [ROME=none]
)

AM_CONDITIONAL([MDM_ROME], [test "x$TARGET" = "xmdm9607" -o "x$TARGET" = "xmdm9635" -o "x$TARGET" = "xmdm9640" -o "x$TARGET" = "xmdm9650" -o "x$TARGET" = "xapq8096" -o "x$TARGET" = "xsdx20" -o "x$ROME" = "xqsap" -o "x$ROME" = "xrobot-rome"])
AM_CONDITIONAL([MDM_PRONTO], [test "x$TARGET" = "xapq8009" -o "x$TARGET" = "xapq8017" -o "x$TARGET" = "xapq8053" -o "x$TARGET" = "xapq8016"])
AM_CONDITIONAL([MDM_CHEROKEE], [test "x$TARGET" = "xapq8098" -o "x$TARGET" = "xsdmsteppe"])
# BTC support
# make below flag to true to enable the BTC
AM_CONDITIONAL([BTC_SUPPORT], false)

# SSR_Cleanup support
# make below flag to true to cleanup audio related things from BT app
# on detection of SSR/command timeout/Hw eror event
AM_CONDITIONAL([SSR_CLEANUP_SUPPORT], true)


AC_MSG_CHECKING(for zlib support)
AC_ARG_WITH(zlib, AC_HELP_STRING([--with-zlib],[Enable zlib support for mod_compress]),
    [WITH_ZLIB=$withval],[WITH_ZLIB=yes])
AC_MSG_RESULT([$WITH_ZLIB])

if test "$WITH_ZLIB" != "no"; then
  AC_CHECK_LIB(z, deflate, [
    AC_CHECK_HEADERS([zlib.h],[
      Z_LIB=-lz
      AC_DEFINE([HAVE_LIBZ], [1], [libz])
      AC_DEFINE([HAVE_ZLIB_H], [1])
    ])
  ])
  if test x$Z_LIB = x; then
     AC_MSG_ERROR([zlib-headers and/or libs where not found, install them or build with --without-zlib])
  fi
fi
AC_SUBST(Z_LIB)

if (test "x${with_glib}" = "xyes"); then
        PKG_CHECK_MODULES(GTHREAD, gthread-2.0 >= 2.16, dummy=yes,
                                AC_MSG_ERROR(GThread >= 2.16 is required))
        PKG_CHECK_MODULES(GLIB, glib-2.0 >= 2.16, dummy=yes,
                                AC_MSG_ERROR(GLib >= 2.16 is required))
        GLIB_CFLAGS="$GLIB_CFLAGS $GTHREAD_CFLAGS"
        GLIB_LIBS="$GLIB_LIBS $GTHREAD_LIBS"

        AC_SUBST(GLIB_CFLAGS)
        AC_SUBST(GLIB_LIBS)
fi

AC_ARG_WITH([lib_path],
      AC_HELP_STRING([--with-lib-path=@<:@dir@:>@],
         [Specify the location of the libraries]),
      [std_libdir=$withval],
      with_lib_path=no)

if test "x$with_lib_path" != "xno"; then
   SYS_LIB=${std_libdir}
fi

AC_ARG_ENABLE([static],
        [  --enable-static Statically link vendor image],
        [case "${enableval}" in
            yes) static=true ;;
            no)  static=false ;;
            *) AC_MSG_ERROR([bad value ${enableval} for --enable-static]) ;;
        esac],[wlan=false])

AM_CONDITIONAL([USING_STATIC_LINK], [test x$static = xtrue])

AC_ARG_ENABLE([ipc],
        [  --enable-ipc Socket IPC used],
        [case "${enableval}" in
            yes) ipc=true ;;
            no)  ipc=false ;;
            *) AC_MSG_ERROR([bad value ${enableval} for --enable-ipc]) ;;
        esac],[ipc=false])

AM_CONDITIONAL([USING_SOCKET], [test x$ipc = xtrue])

AC_ARG_ENABLE([hidl],
        [  --enable-hidl HIDL mechanism],
        [case "${enableval}" in
            yes) hidl=true ;;
            no)  hidl=false ;;
            *) AC_MSG_ERROR([bad value ${enableval} for --enable-hidl]) ;;
        esac],[hidl=false])

AM_CONDITIONAL([USING_HIDL], [test x$hidl = xtrue])

AC_ARG_ENABLE([debug],
        [  --enable-debug    Turn on debugging],
        [case "${enableval}" in
            yes) debug=true ;;
            no)  debug=false ;;
            *) AC_MSG_ERROR([bad value ${enableval} for --enable-debug]) ;;
        esac],[debug=false])

AM_CONDITIONAL([DEBUG], [test x$debug = xtrue])

AC_ARG_ENABLE([all],
        [  --enable-all Enable all FTM functionality],
        [case "${enableval}" in
            yes) all=true ;;
            no)  all=false ;;
            *) AC_MSG_ERROR([bad value ${enableval} for --enable-all]) ;;
        esac],[all=false])

AM_CONDITIONAL([CONFIG_FTM_BT], [test x$all = xtrue])
AM_CONDITIONAL([CONFIG_FTM_FM], [test x$all = xtrue])
AM_CONDITIONAL([CONFIG_FTM_ANT], [test x$all = xtrue])
AM_CONDITIONAL([CONFIG_FTM_NFC], [test x$all = xtrue])

AC_ARG_ENABLE([wlan],
        [  --enable-wlan Enable WLAN FTM functionality],
        [case "${enableval}" in
            yes) wlan=true ;;
            no)  wlan=false ;;
            *) AC_MSG_ERROR([bad value ${enableval} for --enable-wlan]) ;;
        esac],[wlan=false])

AM_CONDITIONAL([CONFIG_FTM_WLAN], [test x$wlan = xtrue -o x$all = xtrue])

AC_ARG_ENABLE([bt],
        [  --enable-bt Enable BT FTM functionality],
        [case "${enableval}" in
           yes) bt=true ;;
           no)  bt=false ;;
        *) AC_MSG_ERROR([bad value ${enableval} for --enable-bt]) ;;
         esac],[bt=false])

AC_SUBST(SYS_LIB)

AC_CHECK_FUNC(signalfd, dummy=yes,
      AC_MSG_ERROR(signalfd support is required))

AC_CHECK_LIB(rt, clock_gettime, dummy=yes,
      AC_MSG_ERROR(realtime clock support is required))

AC_CHECK_LIB(pthread, pthread_create, dummy=yes,
      AC_MSG_ERROR(posix thread support is required))

AC_CHECK_LIB(dl, dlopen, dummy=yes,
      AC_MSG_ERROR(dynamic linking loader is required))

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_TYPE_OFF_T
AC_TYPE_SIZE_T
AC_TYPE_UINT32_T
AC_TYPE_UINT8_T

# Checks for library functions.
AC_FUNC_MALLOC
AC_CHECK_FUNCS([gettimeofday memset select socket strerror])

AC_CONFIG_FILES([ \
         Makefile \
         hidl_client/Makefile \
         ])

AC_OUTPUT
