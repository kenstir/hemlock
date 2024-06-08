# Add Hemlock-specific Org Unit Settings

There are several org unit settings that customize the behavior of the Hemlock apps.
In order to use them, you may need to
1. Create the Org Unit Setting Types
2. Add the Org Unit Settings

## Reference

| Name                          | Datatype | Label                         | Description                                                                                                                      |
|-------------------------------|----------|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `hemlock.eresources_url`      | string   | "Ebooks & Digital" button URL | URL target of the "Ebooks & Digital" action button.  Appears only if app uses main grid, and if setting is on patron's home org. | 
| `hemlock.events_calendar_url` | string   | "Events" button URL           | URL target of the "Events" action button.  Appears only if configured, and if settings is on patron's home org.                  |
| `hemlock.meeting_rooms_url`   | string   | "Meeting Rooms" button URL    | URL target of the "Meeting Rooms" action button.  Appears only if app uses main grid, and if setting is on patron's home org.    |
| `hemlock.museum_passes_url`   | string   | "Museum Passes" button URL    | URL target of the "Museum Passes" action button.  Appears only if app uses main grid, and if setting is on patron's home org.    |                                                                                                                                  |
| `hemlock.cache_key`           | string   | App cache-busting key         | Use this setting on org 1 (consortium) to invalidate the app cache for all patrons.  E.g. "20230608".                            |

## Create the Org Unit Setting Types

As an EG admin, login to the Staff client and go to Administration >> Server Administration >> Org Unit Setting Types

Procedure:
* Click `New Organizational Unit Setting Type`
* Enter the Datatype, Description, Label, and Name from the table above
* Click Save

## Add the Org Unit Settings

As an EG or Library admin, login to the Staff client and go to Administration >> Local Administration >> Library Settings Editor

Procedure:
1. Filter to "hemlock" settings
2. Choose the proper Context Location
3. Add each setting you want
4. Repeat steps 2 and 3 for every context
