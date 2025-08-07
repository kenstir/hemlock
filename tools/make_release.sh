#!/bin/sh

if [ $# -ne 1 ]; then
    echo "usage: $0 app_name"
    exit 1
fi
app_name="$1"
app=${app_name}_app

### find manifest

manifest="${app}/src/main/AndroidManifest.xml"
if [ ! -f "$manifest" ]; then
    echo "No such file: $manifest"
    exit 1
fi

set -e

### scrape versionCode / versionName from the manifest

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

### see if the tag exists

tag=${app}_v${versionCode}
msg="${tag} (${versionName})"

if git rev-parse $tag &>/dev/null; then
    echo $tag already exists at $(git rev-parse $tag)
    exit 1
fi

### set up PATH to include JDK

PATH=$PATH:"/c/Program Files/Android/Android Studio/jbr/bin"

### make the bundle

./gradlew :${app}:bundleRelease
bundle=${app}/build/outputs/bundle/release/${app}-release.aab

### sign the bundle

. secret/env.signing
case "$app_name" in
cwmars|pines|hemlock)
    keyalias=$ORIGINAL_UPLOAD_KEY_ALIAS;;
*)
    keyalias=$NEW_UPLOAD_KEY_ALIAS;;
esac
jarsigner -keystore $KEY_STORE $bundle $keyalias

echo "signed $bundle"

### make the tag

#git commit core/build.gradle "$manifest" -m "$msg" || true
#git tag -a -m "$msg" $tag
#git push
#git push origin $tag
