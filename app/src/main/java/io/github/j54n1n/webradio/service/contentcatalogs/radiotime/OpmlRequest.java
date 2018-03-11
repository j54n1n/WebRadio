package io.github.j54n1n.webradio.service.contentcatalogs.radiotime;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OpmlRequest extends JsonObjectRequest {

    public OpmlRequest(int method, String url, JSONObject jsonRequest,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public OpmlRequest(String url, JSONObject jsonRequest,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> params = new HashMap<>();
        params.put("Accept-Language", Locale.getDefault().toString());
        return params;
    }
}
