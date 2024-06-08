# Fix Caching Issues

The Hemlock app caches many Evergreen responses.  This provides a better user experience, and conserves
mobile data.  But sometimes, you will change something in the Evergreen database and wonder why
it doesn't show up in the app.  The kinds of things cached are:
* the org tree
* org settings, e.g. `hemlock.events_calendar_url` or `credit.payments.allow`

For details on how the caching is implemented, see [notes on caching](../notes-on-caching.md).

## How to bust the cache for you yourself right now

Clear the app cache (Android only, under App info >> Storage & cache >> Clear cache),
then force-quit the app.

## How to invalidate the cache for all patrons

You can invalidate the cache for all patrons.  To do that,
add a setting `hemlock.cache_key` to the org with ID 1.  Use a STRING value of today's date in
ISO format, e.g. `20230416`.  Add a number to the end if you change it more than once per day.
The format is not itself important, just that it is different from prior values.

See [Add Hemlock-specific Org Unit Settings](add-hemlock-org-unit-settings.md)
for the specific details of how to add this org unit setting.
