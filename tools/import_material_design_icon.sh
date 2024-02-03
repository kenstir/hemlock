#!/bin/bash

set -e

topdir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
tar cvf /tmp/foo.tar $(find drawable-{hdpi,mdpi,xhdpi} -name "$1")

echo topdir=$topdir
cd $topdir/core/src/main/res
tar xvf /tmp/foo.tar
