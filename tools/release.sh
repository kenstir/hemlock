#!/bin/bash -
git describe --abbrev=0 --match='dev_*' | sed -e 's/dev_//'
