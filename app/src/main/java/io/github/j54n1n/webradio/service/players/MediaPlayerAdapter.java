package io.github.j54n1n.webradio.service.players;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.AudioAttributesCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import io.github.j54n1n.webradio.service.PlaybackInfoListener;
import io.github.j54n1n.webradio.service.PlayerAdapter;
import io.github.j54n1n.webradio.service.contentcatalogs.WebRadioLibrary;
import io.github.j54n1n.webradio.support.media.AudioFocusRequestCompat;

public class MediaPlayerAdapter extends PlayerAdapter {

    private final Context context;
    private MediaPlayer mediaPlayer;
    private String streamUri;
    private PlaybackInfoListener playbackInfoListener;
    private MediaMetadataCompat currentMedia;
    private int state;
    private boolean currentMediaPlayedToCompletion;

    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo()
    // while not playing.
    private int seekWhileNotPlaying = -1;

    public MediaPlayerAdapter(@NonNull final Context context, PlaybackInfoListener listener) {
        super(context);
        this.context = context.getApplicationContext();
        playbackInfoListener = listener;
        mediaPlayer = null;
    }

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {Activity} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {Activity} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private void initializeMediaPlayer() {
        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            // Use new API instead of setAudioStreamType(STREAM_MUSIC). See also AudioFocusHelper.
            final AudioAttributesCompat audioAttributes = new AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                    .build();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final AudioAttributes attrs = (AudioAttributes) audioAttributes.unwrap();
                mediaPlayer.setAudioAttributes(attrs);
            } else {
                final int streamType = audioAttributes.getLegacyStreamType();
                mediaPlayer.setAudioStreamType(streamType);
            }
            mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playbackInfoListener.onPlaybackCompleted();
                    // Set the state to "paused" because it most closely matches the state
                    // in MediaPlayer with regards to available state transitions compared
                    // to "stop".
                    // Paused allows: seekTo(), start(), pause(), stop()
                    // Stop allows: stop()
                    setNewState(PlaybackStateCompat.STATE_PAUSED);
                }
            });
        }
    }

    // This is the main reducer for the player state machine.
    private void setNewState(@PlaybackStateCompat.State int newPlayerState) {
        state = newPlayerState;
        // Whether playback goes to completion, or whether it is stopped, the
        // currentMediaPlayedToCompletion is set to true.
        if(state == PlaybackStateCompat.STATE_STOPPED) {
            currentMediaPlayedToCompletion = true;
        }
        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        final long reportPosition;
        if(seekWhileNotPlaying >= 0) {
            reportPosition = seekWhileNotPlaying;
            if(state == PlaybackStateCompat.STATE_PLAYING) {
                seekWhileNotPlaying = -1;
            }
        } else {
            reportPosition = (mediaPlayer == null) ? 0 : mediaPlayer.getCurrentPosition();
        }

        final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions());
        stateBuilder.setState(state,
                reportPosition,
                1.0f,
                SystemClock.elapsedRealtime());
        playbackInfoListener.onPlaybackStateChange(stateBuilder.build());
    }

    /**
     * Set the current capabilities available on this session. Note: If a capability is not
     * listed in the bitmask of capabilities then the MediaSession will not handle it. For
     * example, if you don't want ACTION_STOP to be handled by the MediaSession, then don't
     * included it in the bitmask that's returned.
     */
    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        // TODO: Edit available actions.
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        switch(state) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_STOP;
                break;
            default:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    @Override
    public void playFromMedia(MediaMetadataCompat metadata) {
        currentMedia = metadata;
        final String mediaId = metadata.getDescription().getMediaId();
        playStream(WebRadioLibrary.getWebRadioUrl(mediaId));
    }

    @Override
    public MediaMetadataCompat getCurrentMedia() {
        return currentMedia;
    }

    @Override
    public String getCurrentMediaId() {
        return ((currentMedia == null) ? null : currentMedia.getDescription().getMediaId());
    }

    private void playStream(String streamUrl) {
        boolean mediaChanged = (streamUri == null || !streamUrl.equals(streamUri));
        if(currentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media file for playback.
            mediaChanged = true;
            currentMediaPlayedToCompletion = false;
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play();
            }
            return;
        } else {
            release();
        }
        streamUri = streamUrl;
        initializeMediaPlayer();
        try {
            mediaPlayer.setDataSource(context, Uri.parse(streamUri));;
        } catch(Exception e) {
            throw new RuntimeException("Failed to open stream: " + streamUri, e);
        }
        try {
            mediaPlayer.prepare();
        } catch(Exception e) {
            throw new RuntimeException("Failed to prepare stream: " + streamUri, e);
        }
        play();
    }

    @Override
    public boolean isPlaying() {
        return ((mediaPlayer != null) && mediaPlayer.isPlaying());
    }

    @Override
    protected void onPlay() {
        if((mediaPlayer != null) && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            setNewState(PlaybackStateCompat.STATE_PLAYING);
        }
    }

    @Override
    protected void onPause() {
        if((mediaPlayer != null) && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setNewState(PlaybackStateCompat.STATE_PAUSED);
        }
    }

    @Override
    protected void onStop() {
        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        setNewState(PlaybackStateCompat.STATE_STOPPED);
        release();
    }

    private void release() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void seekTo(long position) {
        if(mediaPlayer != null) {
            if(!mediaPlayer.isPlaying()) {
                seekWhileNotPlaying = (int) position;
            }
            mediaPlayer.seekTo((int) position);
            // Set the state (to the current state) because the position changed and should
            // be reported to clients.
            setNewState(state);
        }
    }

    @Override
    public void setVolume(float volume) {
        if(mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }
}
