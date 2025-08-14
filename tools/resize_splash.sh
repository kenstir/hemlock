#!/bin/sh
#
# given a 1024x1024px PNG, resize it for the different densities


case $# in
1)
    orig="$1"
    ;;
*)
    echo "usage: $0 original_1024_px_png"
    exit 1
    ;;
esac

dir=(drawable drawable-xxxhdpi drawable-xxhdpi drawable-xhdpi drawable-hdpi drawable-mdpi)
pixels=( 1024  1024  768  512  384  256)
base=$(basename "$orig")

### sanity check

file $orig || exit 1
echo ""
echo -n "continue?"
read ans

### 

for ((i=0; i<${#dir[@]}; i++)); do
    d="${dir[$i]}"
    px="${pixels[$i]}"

    mkdir -p $d
    echo magick "$orig" -resize ${px}x${px} $d/$base
done
