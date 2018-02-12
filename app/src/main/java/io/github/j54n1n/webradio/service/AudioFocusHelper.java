package io.github.j54n1n.webradio.service;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.media.AudioAttributesCompat;
import android.util.Log;

import io.github.j54n1n.webradio.support.media.AudioFocusRequestCompat;

/**
 * Helper class for managing audio focus related tasks.
 */
public class AudioFocusHelper {

    private static final String TAG = AudioFocusHelper.class.getSimpleName();

    private final AudioFocusHelperImpl impl;

    public AudioFocusHelper(@NonNull final Context context) {
        final AudioManager audioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            impl = new AudioFocusHelperImplApi26(audioManager);
        } else {
            impl = new AudioFocusHelperImplBase(audioManager);
        }
    }

    /**
     * Requests audio focus for the player.
     * @param audioFocusRequest The audio focus request to perform.
     * @return {@code true} if audio focus was granted, {@code false} otherwise.
     */
    public boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequest) {
        return impl.requestAudioFocus(audioFocusRequest);
    }

    /**
     * Abandons audio the previously set audio focus.
     */
    public void abandonAudioFocus() {
        impl.abandonAudioFocus();
    }

    public boolean willPauseWhenDucked() {
        return impl.willPauseWhenDucked();
    }

    /* package */ interface AudioFocusHelperImpl {
        boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequest);
        void abandonAudioFocus();
        boolean willPauseWhenDucked();
    }

    private static class AudioFocusHelperImplBase implements AudioFocusHelperImpl {

        protected final AudioManager audioManager;
        private AudioFocusRequestCompat audioFocusRequest;

        public AudioFocusHelperImplBase(AudioManager audioManager) {
            this.audioManager = audioManager;
        }

        @Override
        public boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequest) {
            // Save the focus request.
            this.audioFocusRequest = audioFocusRequest;
            // Check for possible problems...
            if(audioFocusRequest.acceptsDelayedFocusGain()) {
                // Make an exception to allow the developer to more easily find this code path.
                final String msg = "Cannot request delayed focus gain";
                final String message = msg + " on API " + Build.VERSION.SDK_INT;
                final Throwable tr = new UnsupportedOperationException(message).fillInStackTrace();
                Log.w(TAG, msg, tr);
            }
            final AudioManager.OnAudioFocusChangeListener listener =
                    this.audioFocusRequest.getOnAudioFocusChangeListener();
            final int streamType = audioFocusRequest.getAudioAttributesCompat().getLegacyStreamType();
            final int focusGain = audioFocusRequest.getFocusGain();
            final int result = audioManager.requestAudioFocus(listener, streamType, focusGain);
            return (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }

        @Override
        public void abandonAudioFocus() {
            if(audioFocusRequest == null) {
                return;
            }
            final AudioManager.OnAudioFocusChangeListener listener =
                    audioFocusRequest.getOnAudioFocusChangeListener();
            audioManager.abandonAudioFocus(listener);
        }

        @Override
        public boolean willPauseWhenDucked() {
            if(audioFocusRequest == null) {
                return false;
            }
            final AudioAttributesCompat audioAttributes =
                    audioFocusRequest.getAudioAttributesCompat();
            final boolean pauseWhenDucked = audioFocusRequest.willPauseWhenDucked();
            final boolean isSpeech = (audioAttributes != null) &&
                    (audioAttributes.getContentType() == AudioAttributesCompat.CONTENT_TYPE_SPEECH);
            return (pauseWhenDucked || isSpeech);
        }
    }

    private static class AudioFocusHelperImplApi26 extends AudioFocusHelperImplBase {

        private AudioFocusRequest audioFocusRequest;

        public AudioFocusHelperImplApi26(AudioManager audioManager) {
            super(audioManager);
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.O)
        public boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequest) {
            // Save the focus request.
            this.audioFocusRequest = audioFocusRequest.unwrap();
            final int result = audioManager.requestAudioFocus(this.audioFocusRequest);
            return (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void abandonAudioFocus() {
            if(audioFocusRequest == null) {
                return;
            }
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        }
    }
}
