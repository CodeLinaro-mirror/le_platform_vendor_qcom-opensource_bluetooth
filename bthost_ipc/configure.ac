AC_PREREQ([2.65])
AC_INIT("bthost-ipc", "0.0.1", "")
AM_INIT_AUTOMAKE([1.10 no-define foreign])
AC_CONFIG_HEADERS([config.h])
AC_CONFIG_FILES([Makefile \
                 ])

AC_PROG_CXX
AC_PROG_CC

LT_INIT

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

# Checks for header files.
AC_CHECK_HEADERS([fcntl.h stdlib.h string.h sys/socket.h sys/time.h termios.h unistd.h])

AC_HEADER_STDBOOL
AC_C_INLINE
AC_TYPE_INT16_T
AC_TYPE_INT32_T
AC_TYPE_INT64_T
AC_TYPE_INT8_T
AC_TYPE_PID_T
AC_TYPE_SIZE_T
AC_TYPE_UINT16_T
AC_TYPE_UINT32_T
AC_TYPE_UINT64_T
AC_TYPE_UINT8_T

AC_FUNC_MALLOC
AC_FUNC_MMAP
AC_CHECK_FUNCS([memset munmap strerror])

AC_OUTPUT
