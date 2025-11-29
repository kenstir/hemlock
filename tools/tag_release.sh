#!/bin/sh

if [ $# -ne 1 ]; then
    echo "usage: $0 app_name"
    exit 1
fi
app="$1"
project=${app}_app

### find gradle_file

gradle_file="${project}/build.gradle"
if [ ! -f "$gradle_file" ]; then
    echo "No such file: $gradle_file"
    exit 1
fi

### scrape versionCode / versionName

set -e

versionCode=$(grep -E '^\s*versionCode' $gradle_file | awk '{print $NF}')
echo "Found versionCode=$versionCode"
test -n "$versionCode"

versionName=$(grep -E '^\s*versionName' $gradle_file | awk '{print $NF}')
versionName=${versionName//\"/}
echo "Found versionName=$versionName"
test -n "$versionName"

### tag it

set -ex

tag=${app}_v${versionCode}
msg="${tag} (${versionName})"

git commit core/build.gradle "$manifest" -m "$msg" || true
git tag -a -m "$msg" $tag
git push
git push origin $tag
