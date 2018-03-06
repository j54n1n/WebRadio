package io.github.j54n1n.webradio.client;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class Connection {

    private static Connection connectionInstance;
    private final RequestQueue requestQueue;

    private Connection(Context context) {
        // getApplicationContext() is the key, else it keeps you from leaking the Activity or
        // BroadcastReceiver if someone passes one in.
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized Connection getInstance(Context context) {
        if(connectionInstance == null) {
            connectionInstance = new Connection(context);
        }
        return connectionInstance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }
}
