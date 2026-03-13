#!/bin/sh
#
# fix PATH to include JDK then run command


### fix PATH

case "$OSTYPE" in
darwin*)
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home";;
*)
    export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr";;
esac
PATH="$JAVA_HOME/bin:$PATH"

### run whatever it is

case $# in
0)
    echo "usage: $0 jdk-program [arg...]"
    echo "e.g.   $0 keytool --help"
    exit 1
    ;;
esac

exec "$@"
