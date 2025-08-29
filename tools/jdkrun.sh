#!/bin/sh
#
# fix PATH to include JDK then run command


### fix PATH

PATH=$PATH:"/c/Program Files/Android/Android Studio/jbr/bin"

### run whatever it is

echo '$#' "is $#"
case $# in
0)
    echo "usage: $0 jdk-program [arg...]"
    echo "e.g.   $0 keytool --help"
    exit 1
    ;;
esac

exec "$@"
