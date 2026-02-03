#!/bin/sh
#
# foreach_app.sh -
#
#       print the names of all apps
#
# usage:
#       # for each app, print the server version
#       for i in $(tools/foreach_app.sh); do echo $i:; tools/fetch.sh $i vers; done
#

for app_dir in *_app; do
    app=${app_dir%_app}
    [ "$app" == "hemlock" ] && continue
    echo  $app
done
