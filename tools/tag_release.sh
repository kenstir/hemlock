#!/bin/sh

if [ $# -ne 1 ]; then
    echo "usage: $0 app_name"
    exit 1
fi
app="$1"

manifest="${app}_app/src/main/AndroidManifest.xml"
if [ ! -f "$manifest" ]; then
    echo "No such file: $manifest"
    exit 1
fi

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

set -x

tag=${app}_v${versionCode}
msg="${tag} (${versionName})"

git commit "$manifest" -m "$msg" || true
git tag -a -m "$msg" $tag
git push
git push --tags
