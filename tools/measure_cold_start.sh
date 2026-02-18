#!/bin/bash -
#
# launch app 5 times and report the metrics

ACT=net.kenstir.ui.view.launch.LaunchActivity
ITER=5
results=()

# handle cmd line
case $# in
1)
    app="$1"
    ;;
*)
    echo "usage: $0 app_name"
    exit 1
    ;;
esac

# calculate script dir
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# make sure adb wrapper is in our PATH
PATH=$PATH:$DIR

# get app pkg
set -e
PKG=$($DIR/pkg_name.sh $app)

# get app version
adb shell dumpsys package $PKG | grep -E 'versionName|versionCode'

# cold start the app $ITER times
for i in $(seq $ITER); do
    adb shell am force-stop $PKG
    sleep 0.5
    out=$(adb shell am start -W -S -n $PKG/$ACT)
    echo "$out" > start.$PKG.$i.log
    # extract ThisTime (ms)
    t=$(echo "$out" | grep TotalTime | awk -F': ' '{print $2}')
    echo "run $i: ${t}ms"
    results+=($t)
    sleep 0.5
done

# print average (bash with awk)
printf "%s\n" "${results[@]}" | awk '{sum+=$1; arr[NR]=$1} END { 
    n=NR; mean=sum/n; asort(arr);
    if (n%2==1) { median=arr[int(n/2)+1] } else { median=(arr[n/2]+arr[n/2+1]) / 2 }
    printf "runs=%d mean=%.1f median=%.1f\n", n, mean, median
}'
