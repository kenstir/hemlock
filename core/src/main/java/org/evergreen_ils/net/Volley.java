package org.evergreen_ils.net;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;

import com.android.volley.*;
import com.android.volley.toolbox.ImageLoader;

import net.kenstir.hemlock.logging.Log;

// code adapted from http://developer.android.com/training/volley/
public class Volley {
    private static Volley mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;

    private Volley(Context context) {
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        mCtx = context.getApplicationContext();
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(500);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static synchronized void init(Context context) {
        getInstance(context);
    }

    public static Volley getInstance() {
        return mInstance;
    }

    public static synchronized Volley getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Volley(context);
            //VolleyLog.DEBUG = true;
        }
        return mInstance;
    }

    protected RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = com.android.volley.toolbox.Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public static Response.ErrorListener logErrorListener(final String TAG) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String msg = volleyError.getMessage();
                if (volleyError instanceof TimeoutError) {
                    msg = "Timeout after " + volleyError.getNetworkTimeMs() + "ms";
                }
                if (!TextUtils.isEmpty(msg)) {
                    Log.d(TAG, msg);
                }
            }
        };
    }
}
