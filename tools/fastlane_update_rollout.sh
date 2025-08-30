#!/bin/sh
#
# run inside *_app directory

../tools/jdkrun.sh fastlane supply --track production --rollout 0.6
