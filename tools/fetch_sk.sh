#!/bin/sh

case $# in
1)
    app="$1"
    what=sk
    ;;
2)
    app="$1"
    what="$2"
    ;;
*)
    echo "usage: $0 app_name [sk|require_part]"
    exit 1
    ;;
esac

topdir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

case "$app" in
*_app) app_dir="$app";;
*)     app_dir="${app}_app";;
esac

settings="${topdir}/${app_dir}/src/main/res/values/ou.xml"
if [ ! -f "$settings" ]; then
    echo "No such file: $settings"
    exit 1
fi

# scrape library_url from settings xml
url=$(fgrep -m1 '"ou_library_url"' $settings)
url=${url%<*}
url=${url#*>}
test -n "$url" || {
    echo >&2 "library_url is empty in ${app_dir}"
    exit 1
}
#echo "url: $url"

# construct url
url="${url}/osrf-gateway-v1"
case "$what" in
require_part)
    url="${url}?service=open-ils.actor&method=open-ils.actor.ou_setting.ancestor_default.batch&param=1&param=%5B%22circ.holds.ui_require_monographic_part_when_present%22%5D&param=%22ANONYMOUS%22"
    ;;
sk)
    url="${url}?service=open-ils.actor&method=open-ils.actor.ou_setting.ancestor_default.batch&param=1&param=%5B%22hemlock.cache_key%22%5D&param=%22ANONYMOUS%22"
    ;;
org*)
    url="${url}?service=open-ils.actor&method=open-ils.actor.org_unit.retrieve&param=\"ANONYMOUS\"&param=1"
    ;;
*version)
    url="${url}?service=open-ils.actor&method=opensrf.open-ils.system.ils_version"
    ;;
use_authoritative)
    url="${url}?service=open-ils.actor&method=opensrf.open-ils.system.use_authoritative"
    ;;
*)
    echo >&2 "don't know about \"$what\""
    exit 1
    ;;
esac

# fetch
curl -sS "${url}" && echo
