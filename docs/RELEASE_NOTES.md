## 4.6.0 - or maybe 5.0.0 - UNRELEASED

### New
* If the public catalog has an alert banner showing, display the same message in a dialog on app startup

### Fixed
* Fixed accessibility issues where author and copy info text links were too small
* Fixed ANR (App Not Responding) errors by moving account operations off main thread
* Fixed "Add account" to automatically choose the new account when restarting
* Fixed bug where the app exited when pressing Back after starting from a notification
* Fixed bug where address in Library Info could be double-spaced

### Internal
* internal: Fixed abstraction leaks of data layer into UI layer
* internal: Upgrade to Android Studio Otter 2 Feature Drop | 2025.2.2 Patch 1
* internal: Migrate to ViewPager2 to mitigate TransactionTooLargeException
* internal: Limit search to 100 results to mitigate TransactionTooLargeException
* internal: Fixed NPE seen once in updateButtonViews
* internal: Convert more Java to Kotlin and remove local copies of library code
* internal: Replace deprecated ProgressDialog with Fragment + ViewModel

## 4.5.0

### New
* noble: Enable part holds
* cwmars: Use "Complete set" instead of "Any part"

### Fixed
* Fixed ANR (App Not Responding) error when adding account
* internal: Add fallback drawable/splash_title.png to try to fix Resources$NotFoundException
* internal: Remove unused library_url from SharedPrefs
* internal: Fastlane improvements
* internal: Upgrade to fastlane 2.230.0
* internal: Add fallback splash_title.png to hemlock, indiana, mo, noble, sagecat

## 4.4.1

### Fixes
* Fixed issue where Upcoming Closure was not shown if Reason for Closure
  was missing.  Now it displays with "No reason provided".
* internal: Factor out version.gradle and maintain with fastlane
* internal: Upgrade to AGP 8.13.2

## 4.4.0

### New
* Improve accessibility throughout the app (#57)

  Review and fix items reported by Accessibility Scanner, including - touch target minimum heights
  - content descriptions for TalkBack

### Fixes
* noble: Pay Charges link now goes to new NOBLE catalog

## 4.3.1

### Fixes
* Fixed rare crash in Item Details
* internal: Fixed data abstraction leaks (#56)
* internal: Logging improvements to help track app launch errors
* internal: Include exceptions in logBuffer used by Send Report to Developer
* internal: Include public IP in Send Report to Developer
* internal: Enable nonFinalResIds to speed up build
* internal: Enable gradle config cache to speed up build
* internal: Enable nonTransitiveRClass to speed up build
* cwmars: Tweak Show Card icon

## 4.3.0

### New
* Add physical description to Item Details
* Add Hours of Operation notes to Library Info

### Fixes
* Fixed issue where Retry button was obscured when 3-button navigation is enabled
* Fixed issue where checked out precat item had "Unknown Title"
* internal: Upgrade to Android Studio Narwhal 4 Feature Drop | 2025.1.4

## 4.2.0

### New
* Remember search options, hold options, and list sort options across app launches

  Add just once/always dialog when changing pickup org

### Fixes
* noble: Changed server URL
* internal: Upgrade to Android Studio Narwhal 3 Feature Drop | 2025.1.3
* internal: Upgrade to AGP 8.13.0

## 4.1.0

### New
* pines: Show hold shelf expiration date

### Fixes
* Fix regression: part hold fails with "The system could not find any items to match this hold request"
* Fix rare ANR on first startup after upgrade (Delete volley cache dir on coroutine)
* internal: Upgrade to Android Studio Narwhal Feature Drop | 2025.1.2
* internal: Upgrade to AGP 8.12.1
* internal: Upgrade firebase-bom to 33.16.0

## 4.0.5

### New
* Add ability to clear all accounts

## 4.0.4

### New
* Make lists display edge-to-edge
* Add ability to create list with description
### Fixes
* Fix issue where keyboard covered password field on login
* Fix bug when rotating Search Results
* Target Android API 35 and update dependencies
* internal: Fix issue creating new list with space in name (regression in 4.0.0)
* internal: Fix ripple effect when clicking search results (regression using RecyclerView)
* internal: Show error if username or password are empty

## 4.0.3

### New
* Add edge-to-edge display for most of the app screens

## 4.0.2

### Fixes
* Target Android API 35 and update dependencies

## 4.0.1

### Fixes
* internal: Reorganize code packages structure

## 4.0.0

### Fixes
* internal: Rewrite data layer in Ktor/OkHttp

## 3.2.1

* internal: Upgrade to Android Studio Meerkat Feature Drop | 2024.3.2 Patch 1
* acorn: Update URL and developer email

## 3.2.0

* internal: Upgrade to AS Meerkat 2024.3.2 Patch 1

## 3.1.7

* fix: Always load home library settings on startup so optional buttons appear

## 3.1.6

* Show hold shelf expiration date (mo)

## 3.1.5

* fix: Display current email/phone in Library Info, ignoring cache
* internal: Upgrade to AS Ladybug 2024.2.1 Patch 3
* internal: Rename analytics event to "login_v2" to hopefully make my custom dimensions work
* fix: Improve link contrast in dark mode (MO)

## 3.1.4

* New feature: Push notifications for holds available and overdue notices (acorn)
* internal: Push Notifications Change 1: Enable Opt-In Setting Type to prevent creating database events for push notifications to patrons without the app
* internal: Fix bug: no email app installed if login fails and user tries to send report to developer

## 3.1.3

* Fix bug where Library Info could display a library that was not the patron's home library but which had the same label
* Limit display of Upcoming Closures to 5
* New main screen with grid of buttons (noble)

## 3.1.2

* Restore warning color for text when item approaching due date
* Increase contrast for text colors to WCAG 2.0 level AA
* internal: Upgrade to AS Ladybug

## 3.1.1 (never released to GA)

* internal: Upgrade dependencies

## 3.1.0

* Add support for Push Notifications
* Update dependencies

## 3.0.0

* Replace main screen with grid of buttons (acorn)

## 2.9.1

* Require part selection for item with parts (pines)
* Update dependencies and target Android SDK 34
* internal: Track num_accounts on login
* internal: Update analytics for search event 

## 2.9.0 (Version bumped to match iOS)

* Fixed ugly error when hold fails due to alert block

## 2.8.1

* Add edit button to items in Holds list, tap to see item details
* Add Upcoming Closures to Library Info
* Show Place Hold button if any copies exist, and show Electronic Resources button if any URLs exist (#30)
* Highlight due date only when overdue

## 2.8.0

* New feature: Checkout History
* Update Android tooling to meet Play Store requirements

## 2.7.2

* Fix bug where Place Hold was greyed out if there was a ", " in the record attributes

## 2.7.1

* Update catalog URL (cool, cwmars)
* internal: Use catalog URL from resources, not AccountManager

## 2.7.0

* Tap on author name to search by author
* Update dependencies and target Android 13

## 2.6.3

* Update to Android Studio Giraffe 2022.3.1
* Update dependencies and target Android 12

## 2.6.2

* Restore color icons (CW MARS)

## 2.6.1.2

* Fix CalledFromWrongThreadException

## 2.6.1

* Upgrade dependencies and target Android 13

## 2.6.0 (cwmars 5.6.0)

* Fix error: cancelled
  // Make asynchronous operations more reliable with LifecycleScope
* Display account expiration date on Show Card screen
* Provide a means for the Evergreen admin to invalidate the app cache
  // by changing hemlock.cache_key setting on orgID 1)

## 2.5.0.9

* Install scanner module on first run to fix "waiting to be downloaded" error
* Display account expiration date on Show Card screen (Acorn)
* Provide a means for the Evergreen admin to invalidate the app cache
  // by changing hemlock.cache_key setting on orgID 1

## 2.5.0.3

* Read and delete system messages in the app, instead of launching the browser and having to login again
* Add sort direction to List Details and remember sort preferences across app launches
* Add link to GALILEO Virtual Library (PINES)

## 2.4.1.0

* Add ability to sort lists by title/author/pubdate
* Fix bug where items added to a list would not show up right away

## 2.4.0.4

* Filter duplicate items from bookbags

## 2.4.0

* Scan a barcode to search by ISBN or UPC
* Increase screenBrightness on Show Card screen to make scanning more reliable

## 2.3.4.3

* Increase brightness on Show Card screen to improve barcode scanning. (OWWL)

## 2.3.4.2

* Add Events button to main screen, if the home branch has an events calendar URL.
* Do not clear search results on screen rotation.

## 2.3.4.1 (cwmars 5.3.4.1)

* Remove expiration date from Place Hold screen.  You can still add an expiration date to a hold by editing it.
  // Convert MBRecord to kotlin

## cwmars 5.3.4.0

* Update dependencies
  // Convert HoldRecord to kotlin


## 2.3.3.2 (cwmars 5.3.3.1)

* Sort lists by publication date, like web site
* Update dependencies

## 2.3.3.0 (cwmars 5.3.3.0)

* Do not count staff-only messages toward unread badge
* Update dependencies and target Android 12

## 2.3.2

* Prevent deleted items from appearing in bookbags
* Fix crash in Show Card
* Add Additional Content link to Item Details screen (COOL)
* Display alert on Item Details if record is deleted in the database
* Update dependencies and target Android 11 (CW MARS)
* Fix rare issue viewing administrative holds of type F or R

## 2.3.1

* Display first + last name when logging in with a barcode
* Fix rare issue viewing holds with note: "unregistered class"
* Add Place Hold button to Copy Info screen

## 2.3.0.4

* Fix rare crash when viewing Item Details

## 2.3.0.3

* Add Recommended Reads link to Item Details screen (PINES)


## 2.3.0

* Fix bug preventing SMS hold notification in some conditions
* Fix rare crash when phone does not have a browser configured to handle links
* Do not cache search results or internal server errors

## 2.0.1

* Fix issue where incorrect logo appeared on small screen devices

## 2.0.0

* UX refresh, with Material icons and light mode.  Try out light mode using the menu on the main screen.
* Fixed bug where some items had wrong format displayed in search results
* Hide Series/Subject/ISBN rows in Details view if empty
* Fix Title and Author for ILL checkouts
* Remove Donate from Search menu

## 1.0.4

* Fixed invalid barcode error for patrons with non-Codabar barcodes
* Fixed crash renewing floating asset copy
* Bug fixes
