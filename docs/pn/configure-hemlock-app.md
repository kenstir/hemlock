# Push Notifications: Configure the Hemlock app

This guide is for the mobile app developer.

**TODO**

## Configure the iOS app

* Download the GoogleService-Info.plist file from the [Firebase Console](https://console.firebase.google.com/) (click the gear and choose "Project settings") and store it under the app (e.g. Source/pines_app/)
* Add the GoogleService-Info.plist file to the project
* Add the FCM entitlements to the project
  * In the Project navigator, click Hemlock
  * Choose the project, e.g. PINES
  * Click "+ Capability" and add the "Push Notifications" capability
  * Click "+ Capability" and add the "Background Modes" capability, then select "Remote notifications"
  * For cleanliness, move the newly created .entitlements file (drag it in Xcode to Source/pines_app/)
