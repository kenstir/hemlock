# Push Notifications: Install the hemlock-sendmsg daemon

This guide is for the Evergreen System Administrator.

## Overview

`hemlock-sendmsg` is a small daemon for sending push notifications to the Hemlock mobile apps.
By default, it listens only on `localhost:8842`, and so it should be installed on every host
that executes your action triggers.

For detailed installation and setup instructions, refer to the
[hemlock-sendmsg README](https://github.com/kenstir/hemlock-sendmsg/blob/main/README.md).
