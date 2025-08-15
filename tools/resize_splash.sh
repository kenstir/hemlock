#!/bin/sh
#
# given a PNG, resize it for the different densities


night=
case $# in
1)
    orig="$1"
    ;;
2)
    orig="$1"
    night="$2"
    ;;
*)
    echo "usage: $0 original.png [night.png]"
    echo
    echo "e.g. ../../../../tools/resize_splash.sh splash_title.png night_splash_title.png"
    exit 1
    ;;
esac

density=(xxxhdpi xxhdpi xhdpi hdpi mdpi)

# for logos that are square
#width=(     1024   768    512  384  256)

# for logos that are wider than tall (pines)
#width=(     1200 900 600 450 300)
#width=(1400 1050 700 525 350)
width=(1480 1110 740 555 370)
base=$(basename "$orig")

### sanity check

if [ -n "$night" ]; then
    file $orig $night || exit 1
else
    file $orig || exit 1
fi
echo ""
echo -n "continue?"
read ans

### convert

for ((i=0; i<${#density[@]}; i++)); do
    d="${density[$i]}"
    px="${width[$i]}"

    echo
    set -x

    dir="drawable-${d}"

    ## resize to target width
    mkdir -p $dir
    magick "$orig" -resize ${px}x $dir/$base

    if [ -n "$night" ]; then
        dir="drawable-night-${d}"
        mkdir -p $dir
        magick "$night" -resize ${px}x $dir/$base
    fi

    ## annotate with text indicating density directory
#    magick $d/$base -gravity SouthEast -pointsize 32 -font "Arial" -fill black -stroke black -strokewidth 1 -annotate +10+10 "$d" $d/new_$base
#    mv $d/new_$base $d/$base

    set +x
done
