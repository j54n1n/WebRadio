package io.github.j54n1n.webradio;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import io.github.j54n1n.webradio.client.MediaBrowserAdapter;
import io.github.j54n1n.webradio.service.contentcatalogs.WebRadioLibrary;

public class MainActivity extends AppCompatActivity {

    private ImageView streamArt;
    private TextView streamTitle;
    private TextView streamArtist;
    private ImageView streamControlsImage;

    private MediaBrowserAdapter mediaBrowserAdapter;
    private boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        mediaBrowserAdapter = new MediaBrowserAdapter(this);
        mediaBrowserAdapter.addListener(new MediaBrowserListener());
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaBrowserAdapter.onStop();
    }

    public void onPreviousClicked(View view) {
        mediaBrowserAdapter.getTransportControls().skipToPrevious();
    }

    public void onPlayPauseClicked(View view) {
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
