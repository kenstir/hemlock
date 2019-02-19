#!/bin/sh

base=http://bark.cwmars.org
#base=http://kenstir.ddns.net
if [ -n "$1" ]; then
    base="$1"
fi

classes="ac,acn,acp,ahr,ahtc,aou,aout,au,aua,auact,aum,aus,bmp,cbreb,cbrebi,cbrebin,cbrebn,ccs,circ,csc,cuat,ex,mbt,mbts,mous,mra,mraf,mus,mvr,perm_ex"

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
curl -o fm_IDL.$(basename $base).full.xml "$base/reports/fm_IDL.xml"
curl -o fm_IDL.$(basename $base).partial.xml "$base/reports/fm_IDL.xml?$args"
