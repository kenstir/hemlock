# Push Notifications: Set up the Firebase project

This guide is for the administrator of the Firebase project associated with your app.  If I released your app, you already have a Firebase project.  Contact me for an invitation.

## Create a Firebase project

Go to https://console.firebase.google.com/.  Create an app.

## Enable Cloud Messaging

Under `Project settings`, select the `Cloud Messaging` tab.  Make sure `Firebase Cloud Messaging API (V1)` is enabled.

## Create a Service Account and key

### Create the Message Sender role

Follow the `Manage Service Accounts` link.  Now you are in the [Google Cloud Console](https://console.cloud.google.com/iam-admin/serviceaccounts) under "IAM & Admin".  Choose `Roles` from the left navigation menu.

Create the role:
* Click `+ Create Role` at the top
* Set Title to "Message Sender"
* Set Description to "Role to send push notifications"
* Set ID to "MessageSender"
* Set Role launch stage to "General Availability"
* Click Add Permissions
  * Enter property name "cloudmessaging.messages.create"
  * Select `cloudmessaging.messages.create`
  * Click `Add` to add the permission to the role.
* Click `Create` to finish creating the role.

Now you should see the "Message Sender" role at the top of the Roles list.

### Create a Service Account

In the [Google Cloud Console](https://console.cloud.google.com/iam-admin/), choose `Service Accounts` from the left navigation menu.

Create the account:
* Click `+ Create Service Account` at the top
* Leave the display name blank
* Set ID to "message-sender"
* Set Description to "service account to send messages"
* Click `Create and Continue`
* Select the role we just created, "Message Sender"
* Click `Continue`
* Click `Done`

### Add a Service Account Key

In the [Google Cloud Console](https://console.cloud.google.com/iam-admin/), choose `Service Accounts` from the left navigation menu.

Click the message-sender service account we just created, then choose the `Keys` tab at the top.

**Be prepared to store the key securely** - this procedure immediately downloads the new key to your computer.

Create a key:
* From the `Add Key` menu, select `Create new key`
* Chose JSON
* Click `Create` to create and download the key

**Store the key securely**.  The hemlock-sendmsg program assumes the key is named `service-account.json` in the same directory as the binary.

## Add apps to the project

If I setup your project, you already have these.

In the [Firebase Console](https://console.firebase.google.com/), choose `Project Overview` fromt the lef navigation menu.

* Add an Android app to the project with `+ Add app`
* Add an iOS app to the project with `+ Add app`

Under `Your apps` you can now download the file needed to build the Hemlock app.
* Android: `google-services.json`
* iOS: `GoogleService-Info.plist`
