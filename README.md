What is Hemlock?
----------------
Hemlock is an Android app for your library, provided your library is powered by [Evergreen](http://evergreen-ils.org/).

With the Hemlock app, you can:
* search the catalog
* place a hold
* review the items you have checked out
* renew items

Quick start for end users
-------------------------
1. Make sure your library catalog is powered by Evergreen.  Open the catalog in your browser,
    search for "harry potter goblet of fire", and scroll to the bottom.
    If you see "powered by Evergreen", you are in luck!
    If you see "powered by Aspen Discovery", you may or may not be in luck, ask your library staff.
2. Install the app from the [Google Play Store](https://play.google.com/store/apps/details?id=net.kenstir.apps.hemlock)

Quick start for developers
--------------------------
1. Get the correct version of [Android Studio](https://developer.android.com/studio/index.html). 
   This branch is built using
   ```
   Android Studio Narwhal | 2025.1.1 Patch 1
   ```
2. Directory `core/` contains the shared code and resources, `cwmars_app/` the code and resources needed to customize it for the C/W MARS system.
3. Profit. C'mon, you know better. This is Open Source.

History of this app
-------------------
Large portions of this app were originally implemented by [Daniel Rizea](https://github.com/danielrizea), under the mentorship of Daniel Wells, as a Google Summer of Code project.  It was never released through the Play Store.  For a long while the code sat in a [forgotten corner of the Evergreen Git repo](http://git.evergreen-ils.org/?p=working/Evergreen.git;a=shortlog;h=refs/heads/collab/drizea/android), where it remained powerful but inactive, until one day it infected a hapless bystander, who worked on it [in the shadows](http://git.evergreen-ils.org/?p=working/Evergreen.git;a=shortlog;h=refs/heads/user/kenstir/android-master) before releasing it to the world on this very site.
