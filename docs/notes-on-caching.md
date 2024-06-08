# Notes on Caching

The Hemlock app adds 2 parameters to most HTTP GET requests:
* `_ck=clientCacheKey`
* `_sk=serverCacheKey`

***clientCacheKey*** is the app versionCode.
***serverCacheKey*** is the server ils-version appended with hemlock.cache_key.

In this way we force cache misses in three situations:
1. An app upgrade.
2. A server upgrade.  Server upgrades sometimes involve incompatible IDL which would otherwise cause OSRF decode crashes.
3. Evergreen admin action.  Changing `hemlock.cache_key` on orgID=1 is a final override that is needed only to push out org tree or org URL changes immediately.

