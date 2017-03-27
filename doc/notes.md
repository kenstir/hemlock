How to login and redirect to a URL
----------------------------------

    /eg/opac/login?username=$user&password=$pass&redirect_to=$relative_url
    e.g. /eg/opac/login?redirect_to=%2Feg%2Fopac%2Fmyopac%2Fprefs_settings

Interesting URLs in the OPAC
----------------------------

Change Username - 					/eg/opac/myopac/update_username
Search and History Preferences -	/eg/opac/myopac/prefs_settings

How to read patron messages
---------------------------
    api_name      => 'open-ils.actor.message.retrieve',
    authoritative => 1,
    signature     => q/
        Returns a list of notes for a given user, not
        including ones marked deleted
        @param authtoken The login session key
        @param patronid patron ID
        @param options hash containing optional limit and offset
    /
