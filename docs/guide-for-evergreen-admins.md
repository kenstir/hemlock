# Evergreen Administrators Guide

## Events and other Special Buttons

The Hemlock app can display an Events button on the main screen, which opens the browser on
the events calendar for the patron's home org.  If the app is using the main grid screen (Acorn Catalog),
there are additional special buttons.

The Events button must be enabled in the app config (resource `ou_enable_events_button`).

The main grid screen must be enabled in the app source code; right now there is no config for it.

These special buttons appear up if:
1. they are enabled in the app config
2. the patron's home org has a value for a custom org setting (see below for a list of the org settings)
3. this value is a valid URL

Custom org settings for the Hemlock apps

| Org Setting | Button | Description |
| ----------- | ------ | ----------- |
| `hemlock.eresources_url`      | Ebooks & Digital | URL of digital resources page |
| `hemlock.events_calendar_url` | Events           | events calendar |
| `hemlock.meeting_rooms_url`   | Meeting Rooms    | meeting rooms signup page |
| `hemlock.museum_passes_url`   | Museum Passes    | museum passes signup page |


## Caching, or why doesn't the Events button show up after I added the URL?

The Hemlock app caches many Evergreen responses, for a better user experience, and to conserve
mobile data.  But sometimes, you will change something in the Evergreen database and wonder why
it doesn't show up in the app.  The kinds of things cached are:
* the org tree
* org settings, e.g. `hemlock.events_calendar_url` or `credit.payments.allow`

For details on how the caching is implemented, see [notes on caching](notes-on-caching.md).

### How to bust the cache for you yourself right now

There are 2 ways:
1. Clear the app cache (Android only), then force-quit the app, or
2. Visit the Library Info page for the org, then force-quit the app

### How to invalidate the cache for all patrons

This is rarely necessary, but you can invalidate the cache for all patrons.  To do that,
add a setting `hemlock.cache_key` to the org with ID 1.  Use a value of today's date in
ISO format, e.g. `20230416`.  Add a number to the end if you change it more than once per day.
The format is not itself important, just that it is different from prior values.
