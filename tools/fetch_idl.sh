#!/bin/sh

base=https://bark.cwmars.org
#base=http://kenstir.ddns.net
if [ -n "$1" ]; then
    base="$1"
fi

version=$(curl -sS "$base/osrf-gateway-v1?service=open-ils.actor&method=opensrf.open-ils.system.ils_version" | q -r '.payload[0]')

#classes="ac,acn,acp,ahr,ahrn,ahtc,aoa,aou,aouhoo,aout,au,aua,auact,auch,aum,aus,bmp,bre,cbreb,cbrebi,cbrebin,cbrebn,ccs,ccvm,cfg,circ,csc,cuat,ex,mbt,mbts,mous,mra,mraf,mus,mvr,perm_ex"
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
