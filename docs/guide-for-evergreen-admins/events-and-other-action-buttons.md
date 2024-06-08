# Events and Other Action Buttons

The Hemlock app can display an Events button on the main screen, which opens the browser on
the events calendar for the patron's home org.  If the app is using the main grid screen (Acorn Catalog),
there are additional action buttons.

The Events button must be enabled in the app config (resource `ou_enable_events_button`).

The main grid screen must be enabled in the app source code; right now there is no config for it.

An action button will appear up if:
1. it is enabled in the app config
2. the patron's home org has a value for the custom org setting
3. the setting's value is a valid URL

See [Add Hemlock-specific Org Unit Settings](add-hemlock-org-unit-settings.md)
for the list of org unit settings,
and [Fix Caching Issues](fix-caching-issues.md)
for how the app's cache will affect the rollout of your new settings.
