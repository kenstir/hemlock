package org.opensrf.util;

class JSONParserException extends JSONException {
    public JSONParserException(org.json.JSONException e) {
        super(e.toString());
    }
}
