#!/bin/sh -ex

case $# in
1) 
    base="$1"
    ;;
*) 
    echo >&2 "usage: $0 URL"
    echo >&2 "e.g.:  $0 https://bark.cwmars.org"
    exit 1
esac

version=$(curl -sS "$base/osrf-gateway-v1?service=open-ils.actor&method=opensrf.open-ils.system.ils_version" | jq -r '.payload[0]')

classes=$(grep IDL_CLASSES core/src/main/java/org/evergreen_ils/Api.kt | awk '{print $NF}' | sed -e 's/"//g')

# create array params
IFS=, read -r -a class_array <<< "$classes"
params=()
for class in "${class_array[@]}"; do
    #echo "class=$class"
    params+=("class=$class")
done
#echo "params=${params[@]}"

# join params with &
function join_with { local IFS="$1"; shift; echo "$*"; }
args="$(join_with '&' "${params[@]}")"
echo args="$args"

# fetch full IDL and IDL with only select classes
set -x
curl -o fm_IDL.$(basename $base).$version.full.xml "$base/reports/fm_IDL.xml"
curl -o fm_IDL.$(basename $base).$version.partial.xml "$base/reports/fm_IDL.xml?$args"
