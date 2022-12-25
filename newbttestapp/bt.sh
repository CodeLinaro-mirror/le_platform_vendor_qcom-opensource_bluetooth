###############################################################################
#
# Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
# SPDX-License-Identifier: BSD-3-Clause-Clear
#
###############################################################################

###############################################################################
#
# File: bt.sh
#
# Description : Utility script to test Bluetooth
#
# Version : 1.0
#
###############################################################################

#!/bin/bash
set -e

readonly usage="\
Usage:

Run the script:
    $0 <-d>     dump test app process id

    $0 <-k>     kill test app

    $0 <-r>     restart test app

    $0 <-s>     start test app

    $0 <-t> [adapter1|hfp|a2dp|pbap|hidh|spp1]
                adapter1 enable|disable|search|cancel_search|pair|
                         accept_pair|reject_pair|get_supported_profiles
                            : test new Bluetooth adapter

                hfp connect|disconnect|connect_audio|disconnect_audio|set_active_device device
                            : test hfp(ag) in new Bluetooth adapter
                            : device is remote Bluetooth device's address
                            : E.g. 11:22:33:44:AA:BB

                a2dp connect|disconnect device
                            : test a2dp(source) in new Bluetooth adapter

                pbap disconnect|allow|reject device
                            : test pbap(server) in new Bluetooth adapter

                hidh connect|disconnect device
                            : test hid(host) in new Bluetooth adapter

                spp1 connect|disconnect device
                            : test spp in new Bluetooth adapter
                voip start|stop|start_bt_sco|stop_bt_sco
                            : test voip simulation

Examples:
    $0 -r : restart test app
    $0 -t \"adapter1 enable\" : enable new Bluetooth adapter
    $0 -t \"hfp connect 11:22:33:44:AA:BB\" : connect hfp with remote device
"

#############################################################

# software product
sp=SA8295P.HQX.4.2.4.1

# bluetooth test app
app=org.codeaurora.bluetooth.newbttestapp

show_help()
{
    echo -e "$usage"
}

exit_info()
{
    echo "$1"
    show_help
    exit 1
}

echo_info()
{
    echo -e "\e[32m$1 \e[m"
}

start_app()
{
    echo_info "start $app"
    am start -n $app/.MainActivity
}

kill_app()
{
    echo_info "kill $app"
    am force-stop $app
}

restart_app()
{
    echo_info "restart $app"
    kill_app
    sleep 2
    start_app
}

dump_app()
{
    echo_info "dump $app"
    ps -A | grep $app
}

if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    exit_info "show help"
fi

while getopts "dkrst:" arg
do
    case $arg in
        d)
            dump_app
            ;;
        k)
            kill_app
            ;;
        r)
            restart_app
            ;;
        s)
            start_app
            ;;
        t)
            echo "test command: $OPTARG"
            dumpsys activity $app $OPTARG
            ;;
        ?)
            exit_info "unknown argument $arg"
            ;;
    esac
done
