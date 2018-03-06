package io.github.j54n1n.webradio;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import io.github.j54n1n.webradio.client.MediaBrowserAdapter;
import io.github.j54n1n.webradio.service.contentcatalogs.WebRadioLibrary;

public class MainActivity extends AppCompatActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    private ImageView streamArt;
    private TextView streamTitle;
    private TextView streamArtist;
    private ImageView streamControlsImage;

    private MediaBrowserAdapter mediaBrowserAdapter;
    private boolean isPlaying;

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        mediaBrowserAdapter = new MediaBrowserAdapter(this);
        mediaBrowserAdapter.addListener(new MediaBrowserListener());

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                requestFromUrl("http://www.android.com");
            }
        }).start();
        */
    }

    private void initUI() {
        streamArt = findViewById(R.id.iv_stream_art);
        streamTitle = findViewById(R.id.tv_stream_title);
        streamArtist = findViewById(R.id.tv_stream_artist);
        streamControlsImage = findViewById(R.id.iv_stream_controls);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowserAdapter.onStart();

        // TODO: 302 redirect from http to https does not work.
        //final String url = "https://www.android.com";
        //final String url = "http://www.android.com";
        final String url = "http://opml.radiotime.com/Search.ashx?query=bbc";
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG,"Error: " + url);
            }
        });
        stringRequest.setTag(TAG);
        //stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        requestQueue.add(stringRequest);
    }

    private static void requestFromUrl(String url) {
        // Choose: HttpUrlConnection, Apache HTTP, Volley
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.connect();
            InputStream stream;
            if((connection.getContentEncoding() != null)
                    && (connection.getContentEncoding().equalsIgnoreCase("gzip"))) {
                stream = new GZIPInputStream(connection.getInputStream());
            } else {
                stream = new BufferedInputStream(connection.getInputStream());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line, response = "";
            while((line = reader.readLine()) != null) {
                response += line + "\n";
            }
            Log.d(url, response);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaBrowserAdapter.onStop();

        if(requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    public void onPreviousClicked(View view) {
        mediaBrowserAdapter.getTransportControls().skipToPrevious();
    }

    public void onPlayPauseClicked(View view) {
        // TODO: Quick hack to get media playing. Improve MediaBrowser.
        mediaBrowserAdapter.getTransportControls().playFromMediaId(
                WebRadioLibrary.getMediaItems().get(0).getMediaId(), null);
        if(isPlaying) {
            mediaBrowserAdapter.getTransportControls().pause();
        } else {
            mediaBrowserAdapter.getTransportControls().play();
        }
    }

    public void onNextClicked(View view) {
        mediaBrowserAdapter.getTransportControls().skipToNext();
    }

    class MediaBrowserListener extends MediaBrowserAdapter.MediaBrowserChangeListener {

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackStateCompat playbackState) {
            isPlaying = ((playbackState != null)
                    && (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING));
            streamControlsImage.setPressed(isPlaying);
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadataCompat mediaMetadata) {
            if(mediaMetadata == null) {
                return;
            }
            final String title = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            final String artist =  mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            final Bitmap art = WebRadioLibrary.getAlbumBitmap(
                    MainActivity.this,
                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
            streamTitle.setText(title);
            streamArtist.setText(artist);
            streamArt.setImageBitmap(art);
        }
    }
}
