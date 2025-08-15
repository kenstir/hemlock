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

    echo
    set -x

    ## resize to target dimensions
    mkdir -p $d
    magick "$orig" -resize ${px}x${px} $d/$base

    ## annotate with text indicating density directory
    magick $d/$base -gravity SouthEast -pointsize 32 -font "Arial" -fill black -stroke black -strokewidth 1 -annotate +10+10 "$d" $d/new_$base
    mv $d/new_$base $d/$base

    set +x
done
