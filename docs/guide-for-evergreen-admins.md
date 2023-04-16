# Evergreen Administrators Guide

## Events

The Hemlock app can display an Events button on the main screen, which opens the browser on
the events calendar for the patron's home org.  This button shows up if:
1. it is enabled in the app config
2. the patron's home org has a value for "hemlock.events_calendar_url"
3. this value is a valid URL

## Caching, or why doesn't the Events button show up after I added the URL?

The Hemlock app caches many Evergreen responses, for a better user experience, and to conserve
mobile data.  But sometimes, you will change something in the Evergreen database and wonder why
it doesn't show up in the app.  The kinds of things cached are:
* the org tree
* org settings, e.g. "hemlock.events_calendar_url" or "credit.payments.allow"

For details on how the caching is implemented, see [notes on caching](notes-on-caching.md).

### How to bust the cache for you yourself right now

There are 2 ways:
1. Clear the app cache, then force-quit the app, or
2. Visit the Library Info page for the org, then force-quit the app

### How to invalidate the cache for all patrons

This is rarely necessary, but you can invalidate the cache for all patrons.  To do this:
* on the org with ID 1, add a setting "hemlock.cache_key" to today's date in ISO format, e.g. "20230416".

The format is not itself important, just that it is different from prior values.
