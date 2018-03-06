package io.github.j54n1n.webradio.service.contentcatalogs.radiotime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

import io.github.j54n1n.webradio.BuildConfig;

// TODO: Urls, keys ect.
public class OPML {

    private static String serial = null;
    private static final String PREF_SERIAL = "PREF_OPML_SERIAL";

    public static String getPartnerId() {
        return BuildConfig.KEY_OPML_PARTNER_ID;
    }

    @SuppressLint("ApplySharedPref")
    public synchronized static String getSerial(Context context) {
        if (serial == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_SERIAL, Context.MODE_PRIVATE);
            serial = sharedPrefs.getString(PREF_SERIAL, null);
            if (serial == null) {
                serial = UUID.randomUUID().toString().replaceAll("-", "");
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_SERIAL, serial);
                editor.commit();
            }
        }
        return serial;
    }

}
