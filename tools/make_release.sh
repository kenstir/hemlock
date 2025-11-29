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

### scrape versionCode / versionName

set -e

versionCode=$(grep -E '^\s*versionCode' "$gradle_file" | awk '{print $NF}')
echo "Found versionCode=$versionCode"
test -n "$versionCode"

versionName=$(grep -E '^\s*versionName' "$gradle_file" | awk '{print $NF}')
versionName=${versionName//\"/}
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

bash gradlew :${project}:bundleRelease
bundle=${project}/build/outputs/bundle/release/${project}-release.aab

### copy the bundle somewhere convenient

echo "Built signed bundle at $bundle"
cp "$bundle" ~/Downloads/
echo "Copied to ~/Downloads/"
case "$OSTYPE" in
darwin*) open ~/Downloads;;
*) explorer "$HOMEDRIVE$HOMEPATH\\Downloads";
esac

### make the tag

#git commit core/build.gradle "$gradle_file" -m "$msg" || true
#git tag -a -m "$msg" $tag
#git push
#git push origin $tag
