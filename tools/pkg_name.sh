#!/bin/bash -
#
# given an app name, return the package name (applicationId)

# calculate script dir
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# handle cmd line
case $# in
1)
    app="$1"
    ;;
*)
    echo "usage: $0 app_name"
    exit 1
    ;;
esac

# scrape namespace from build.gradle
# we do not use applicationId as it defaults to namespace
set -e
ns=$(grep -E namespace $DIR/../${app}_app/build.gradle)
ns="${ns#*\'}"
ns="${ns%\'*}"
echo "$ns"
