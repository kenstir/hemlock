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
echo "Found versionCode=$versionCode"
test -n "$versionCode"

versionName=$(egrep android:versionName $manifest)
versionName=${versionName#*\"}
versionName=${versionName%\"*}
echo "Found versionName=$versionName"
test -n "$versionName"

### see if the tag exists

tag=${app}_v${versionCode}
msg="${tag} (${versionName})"

echo "Checking for tag $tag ..."
if git rev-parse $tag &>/dev/null; then
    echo $tag already exists at $(git rev-parse $tag)
    exit 1
fi

### set up PATH to include JDK

PATH=$PATH:"/c/Program Files/Android/Android Studio/jbr/bin"

### build and sign the bundle

./gradlew :${project}:bundleRelease
bundle=${project}/build/outputs/bundle/release/${project}-release.aab

### copy the bundle somewhere convenient

echo "Built signed bundle at $bundle"
cp "$bundle" ~/Downloads/
echo "Copied to ~/Downloads/"
explorer "$HOMEDRIVE$HOMEPATH\\Downloads"

### make the tag

#git commit core/build.gradle "$manifest" -m "$msg" || true
#git tag -a -m "$msg" $tag
#git push
#git push origin $tag
