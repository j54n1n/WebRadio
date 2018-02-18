package io.github.j54n1n.webradio.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.v4.media.AudioAttributesCompat;
import android.support.v4.media.MediaMetadataCompat;

import io.github.j54n1n.webradio.support.media.AudioFocusRequestCompat;

public abstract class PlayerAdapter {

    private static final IntentFilter INTENT_FILTER_AUDIO_NOISY =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private boolean isAudioNoisyReceiverRegistered = false;
    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if(isPlaying()) {
                    pause();
                }
            }
        }
    };

    private final Context applicationContext;
    private final AudioFocusHelper audioFocusHelper;
    private final AudioFocusListener audioFocusListener;
    private final AudioFocusRequestCompat audioFocusRequest;

    private boolean isResumingOnFocusGain = false;

    public PlayerAdapter(@NonNull final Context context) {
        applicationContext = context;
        audioFocusHelper = new AudioFocusHelper(applicationContext);
        audioFocusListener = new AudioFocusListener();
        final AudioAttributesCompat audioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .build();
        audioFocusRequest = new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build();
    }

    public abstract void playFromMedia(MediaMetadataCompat metadata);

    public abstract MediaMetadataCompat getCurrentMedia();

    public abstract String getCurrentMediaId();

    public abstract boolean isPlaying();

    public final void play() {
        if(audioFocusHelper.requestAudioFocus(audioFocusRequest)) {
            registerAudioNoisyReceiver();
            onPlay();
        }
    }

    /**
     * Called when media is ready to be played and indicates the app has audio focus.
     */
    protected abstract void onPlay();

    public final void pause() {
        if(!isResumingOnFocusGain) {
            audioFocusHelper.abandonAudioFocus();
        }
        unregisterAudioNoisyReceiver();
        onPause();
    }

    /**
     * Called when media must be paused.
     */
    protected abstract void onPause();

    public final void stop() {
        audioFocusHelper.abandonAudioFocus();
        unregisterAudioNoisyReceiver();
        onStop();
    }

    /**
     * Called when the media must be stopped. The player should clean up resources at this
     * point.
     */
    protected abstract void onStop();

    public abstract void seekTo(long position);

    public abstract void setVolume(float volume);

    private void registerAudioNoisyReceiver() {
        if (!isAudioNoisyReceiverRegistered) {
            applicationContext.registerReceiver(audioNoisyReceiver, INTENT_FILTER_AUDIO_NOISY);
            isAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (isAudioNoisyReceiverRegistered) {
            applicationContext.unregisterReceiver(audioNoisyReceiver);
            isAudioNoisyReceiverRegistered = false;
        }
    }

    /**
     * Implementation of an Android Oreo inspired {@link AudioManager.OnAudioFocusChangeListener}.
     */
    final class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {

        private static final float MEDIA_VOLUME_DEFAULT = 1.0f;
        private static final float MEDIA_VOLUME_DUCK = 0.2f;

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if(isResumingOnFocusGain) {
                        play();
                        isResumingOnFocusGain = false;
                    } else if(isPlaying()) {
                        setVolume(MEDIA_VOLUME_DEFAULT);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if(!audioFocusHelper.willPauseWhenDucked()) {
                        setVolume(MEDIA_VOLUME_DUCK);
                    }
                    break;
                    // This stream doesn't duck, so fall through and handle it the
                    // same as if it were an AUDIOFOCUS_LOSS_TRANSIENT.
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    isResumingOnFocusGain = isPlaying();
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    isResumingOnFocusGain = false;
                    stop();
                    audioFocusHelper.abandonAudioFocus();
                    break;
            }
        }
    }
}
