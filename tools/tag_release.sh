#!/bin/sh

if [ $# -ne 1 ]; then
    echo "usage: $0 app_name"
    exit 1
fi
app="$1"
project=${app}_app

### find manifest

manifest="${project}/src/main/AndroidManifest.xml"
if [ ! -f "$manifest" ]; then
    echo "No such file: $manifest"
    exit 1
fi

### scrape versionCode / versionName from the manifest

set -e

versionCode=$(egrep android:versionCode $manifest)
versionCode=${versionCode#*\"}
versionCode=${versionCode%\"*}
echo versionCode=$versionCode
test -n "$versionCode"

versionName=$(egrep android:versionName $manifest)
versionName=${versionName#*\"}
versionName=${versionName%\"*}
echo versionName=$versionName
test -n "$versionName"

### tag it

set -ex

tag=${app}_v${versionCode}
msg="${tag} (${versionName})"

git commit core/build.gradle "$manifest" -m "$msg" || true
git tag -a -m "$msg" $tag
git push
git push origin $tag
