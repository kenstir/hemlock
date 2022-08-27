package org.opensrf.net.http;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.opensrf.*;
import org.opensrf.util.*;


/**
 * Manages connection parameters and thread limiting for opensrf json gateway connections.
 */

public class HttpConnection {

    /** Compiled URL object */
    protected URL url;

    public HttpConnection(String fullUrl) throws java.net.MalformedURLException {
        url = new URL(fullUrl);
    }
}


