package io.github.j54n1n.webradio;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.github.j54n1n.webradio.client.BrowseIndexPagerAdapter;
import io.github.j54n1n.webradio.client.Connection;
import io.github.j54n1n.webradio.service.contentcatalogs.radiotime.OPML;
import io.github.j54n1n.webradio.service.contentcatalogs.radiotime.model.BrowseIndexItem;

public class Main2Activity extends AppCompatActivity {

    private static final String TAG = Main2Activity.class.getSimpleName();

    private BrowseIndexPagerAdapter browseIndexPagerAdapter;
    private List<BrowseIndexItem> browseIndexItems;
    private RequestQueue requestQueue;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);

        requestQueue = Connection.getInstance(this).getRequestQueue();
        browseIndexItems = new ArrayList<>();
        parseJson();

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }

    // TODO: move to OPML class; set http headers ect.
    private void parseJson() {
        final String baseUrl = "http://opml.radiotime.com/Browse.ashx";
        final String url = baseUrl
                + "?partnerId=" + OPML.getPartnerId()
                + "&serial=" + OPML.getSerial(this)
                + "&render=json";
        Log.d(TAG, "url = " + url);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("body");
                    for(int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        String text = item.getString("text");
                        String url = item.getString("URL");
                        String key = item.getString("key");
                        if("settings".equalsIgnoreCase(key)) {
                            continue;
                        }
                        browseIndexItems.add(new BrowseIndexItem(text, url, key));
                    }
                    // Create the adapter that will return a fragment for each of the primary
                    // sections of the activity.
                    browseIndexPagerAdapter = new BrowseIndexPagerAdapter(
                            getSupportFragmentManager(), browseIndexItems);
                    // Set up the ViewPager with the sections adapter.
                    mViewPager.setAdapter(browseIndexPagerAdapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        requestQueue.add(request);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
