#!/bin/sh

if [ $# -ne 1 ]; then
    echo "usage: $0 app_name"
    exit 1
fi
app="$1"
project=${app}_app

### check env

if [ ! -r secret/keystore.properties ]; then
    echo "No such file: secret/keystore.properties"
    exit 1
fi

### find gradle_file

gradle_file="${project}/build.gradle"
if [ ! -f "$gradle_file" ]; then
    echo "No such file: $gradle_file"
    exit 1
fi

### abandon this madness for fastlane

cd $project
exec ../tools/fl beta
