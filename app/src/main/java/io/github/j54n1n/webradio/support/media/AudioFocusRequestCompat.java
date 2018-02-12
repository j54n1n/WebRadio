package io.github.j54n1n.webradio.support.media;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.media.AudioAttributesCompat;

/**
 * Compatibility version of an {@link android.media.AudioFocusRequest}.
 */
public class AudioFocusRequestCompat {

    private final int _focusGain;
    private AudioManager.OnAudioFocusChangeListener _onAudioFocusChangeListener;
    private Handler _focusChangeHandler;
    private final AudioAttributesCompat _audioAttributesCompat;

    // Flags
    private final boolean _pauseOnDuck;
    private final boolean _acceptsDelayedFocusGain;

    private AudioFocusRequestCompat(
            int focusGain,
            AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener,
            Handler focusChangeHandler,
            AudioAttributesCompat audioAttributesCompat,
            boolean pauseOnDuck,
            boolean acceptsDelayedFocusGain) {
        _focusGain = focusGain;
        _onAudioFocusChangeListener = onAudioFocusChangeListener;
        _focusChangeHandler = focusChangeHandler;
        _audioAttributesCompat = audioAttributesCompat;
        _pauseOnDuck = pauseOnDuck;
        _acceptsDelayedFocusGain = acceptsDelayedFocusGain;
    }

    /**
     * Returns the type of audio focus request configured for this {@code AudioFocusRequestCompat}.
     * @return one of {@link AudioManager#AUDIOFOCUS_GAIN},
     * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT},
     * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}, and
     * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE}.
     */
    public int getFocusGain() {
        return _focusGain;
    }

    /**
     * Returns the {@link AudioAttributesCompat} set for this {@code AudioFocusRequestCompat}, or
     * the default attributes if none were set.
     * @return non-null {@link AudioAttributesCompat}.
     */
    public AudioAttributesCompat getAudioAttributesCompat() {
        return _audioAttributesCompat;
    }

    /**
     * Returns whether the application that would use this {@code AudioFocusRequestCompat} would
     * pause when it is requested to duck.
     * @return the duck/pause behavior.
     */
    public boolean willPauseWhenDucked() {
        return _pauseOnDuck;
    }

    /**
     * Returns whether the application that would use this {@code AudioFocusRequestCompat} supports
     * a focus gain granted after a temporary request failure.
     * @return whether delayed focus gain is supported.
     */
    public boolean acceptsDelayedFocusGain() {
        return _acceptsDelayedFocusGain;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public AudioFocusRequest unwrap() {
        return new AudioFocusRequest.Builder(_focusGain)
                .setAudioAttributes(getAudioAttributes())
                .setAcceptsDelayedFocusGain(_acceptsDelayedFocusGain)
                .setWillPauseWhenDucked(_pauseOnDuck)
                .setOnAudioFocusChangeListener(_onAudioFocusChangeListener, _focusChangeHandler)
                .build();
    }

    private AudioAttributes getAudioAttributes() {
        final AudioAttributesCompat attributes = getAudioAttributesCompat();
        return (attributes != null) ? (AudioAttributes) (attributes.unwrap()) : null;
    }

    public AudioManager.OnAudioFocusChangeListener getOnAudioFocusChangeListener() {
        return _onAudioFocusChangeListener;
    }

    /**
     * Builder for an {@link AudioFocusRequestCompat}.
     */
    public static final class Builder {
        private int _focusGain;
        private AudioManager.OnAudioFocusChangeListener _onAudioFocusChangeListener;
        private Handler _focusChangeHandler;
        private AudioAttributesCompat _audioAttributesCompat;

        // Flags
        private boolean _pauseOnDuck;
        private boolean _acceptsDelayedFocusGain;

        /**
         * Constructs a new {@code Builder}, and specifies how audio focus
         * will be requested. Valid values for focus requests are
         * {@link AudioManager#AUDIOFOCUS_GAIN},
         * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT},
         * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}, and
         * {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE}.
         * <p>By default there is no focus change listener, delayed focus is not supported, ducking
         * is suitable for the application, and the <code>AudioAttributes</code>
         * have a usage of {@link AudioAttributesCompat#USAGE_MEDIA}.
         * @param focusGain the type of audio focus gain that will be requested
         * @throws IllegalArgumentException thrown when an invalid focus gain type is used
         */
        public Builder(int focusGain) {
            setFocusGain(focusGain);
        }

        /**
         * Constructs a new {@code Builder} with all the properties of the
         * {@code AudioFocusRequestCompat} passed as parameter.
         * Use this method when you want a new request to differ only by some properties.
         * @param requestToCopy the non-null {@code AudioFocusRequestCompat} to build a duplicate
         *                      from.
         * @throws IllegalArgumentException thrown when a null {@code AudioFocusRequestCompat} is
         * used.
         */
        public Builder(@NonNull AudioFocusRequestCompat requestToCopy) {
            if(requestToCopy == null) {
                throw new IllegalArgumentException("Illegal null AudioFocusRequestCompat");
            }
            _focusGain = requestToCopy._focusGain;
            _onAudioFocusChangeListener = requestToCopy._onAudioFocusChangeListener;
            _focusChangeHandler = requestToCopy._focusChangeHandler;
            _audioAttributesCompat = requestToCopy._audioAttributesCompat;
            _pauseOnDuck = requestToCopy._pauseOnDuck;
            _acceptsDelayedFocusGain = requestToCopy._acceptsDelayedFocusGain;
        }

        /**
         * Sets the type of focus gain that will be requested.
         * Use this method to replace the focus gain when building a request by modifying an
         * existing {@code AudioFocusRequestCompat} instance.
         * @param focusGain the type of audio focus gain that will be requested.
         * @return this {@code Builder} instance
         * @throws IllegalArgumentException thrown when an invalid focus gain type is used
         */
        public @NonNull Builder setFocusGain(int focusGain) {
            boolean isValidFocusGain = false;
            switch(focusGain) {
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
                    isValidFocusGain = true;
            }
            if (!isValidFocusGain) {
                throw new IllegalArgumentException("Illegal audio focus gain type " + focusGain);
            }
            _focusGain = focusGain;
            return this;
        }

        /**
         * Sets the listener called when audio focus changes after being requested with
         *   {@link AudioManager#requestAudioFocus}, and until being abandoned with
         *   {@link AudioManager#abandonAudioFocusRequest}.
         *   Note that only focus changes (gains and losses) affecting the focus owner are reported,
         *   not gains and losses of other focus requesters in the system.<br>
         *   Notifications are delivered on the main {@link Looper}.
         * @param listener the listener receiving the focus change notifications.
         * @return this {@code Builder} instance.
         * @throws NullPointerException thrown when a null focus listener is used.
         */
        public @NonNull Builder setOnAudioFocusChangeListener(
                @NonNull AudioManager.OnAudioFocusChangeListener listener) {
            if(listener == null) {
                throw new NullPointerException("Illegal null focus listener");
            }
            return setOnAudioFocusChangeListener(listener, new Handler(Looper.getMainLooper()));
        }

        /**
         * Sets the listener called when audio focus changes after being requested with
         *   {@link AudioManager#requestAudioFocus}, and until being abandoned with
         *   {@link AudioManager#abandonAudioFocusRequest}.
         *   Note that only focus changes (gains and losses) affecting the focus owner are reported,
         *   not gains and losses of other focus requesters in the system.
         * @param listener the listener receiving the focus change notifications.
         * @param handler the {@link Handler} for the thread on which to execute
         *   the notifications.
         * @return this {@code Builder} instance.
         * @throws NullPointerException thrown when a null focus listener or handler is used.
         */
        public @NonNull Builder setOnAudioFocusChangeListener(
                @NonNull AudioManager.OnAudioFocusChangeListener listener,
                @NonNull Handler handler) {
            if(listener == null || handler == null) {
                throw new NullPointerException("Illegal null focus listener or handler");
            }
            _onAudioFocusChangeListener = listener;
            _focusChangeHandler = handler;
            return this;
        }

        /**
         * Sets the {@link AudioAttributesCompat} to be associated with the focus request, and which
         * describe the use case for which focus is requested.
         * As the focus requests typically precede audio playback, this information is used on
         * certain platforms to declare the subsequent playback use case. It is therefore good
         * practice to use in this method the same {@code AudioAttributes} as used for
         * playback, see for example {@link android.media.MediaPlayer#setAudioAttributes} in
         * {@code MediaPlayer} or {@link android.media.AudioTrack.Builder#setAudioAttributes}
         * in {@code AudioTrack}.
         * @param attributes the {@link AudioAttributesCompat} for the focus request.
         * @return this {@code Builder} instance.
         * @throws NullPointerException thrown when using null for the attributes.
         */
        public @NonNull Builder setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
            if(attributes == null) {
                throw new NullPointerException("Illegal null AudioAttributes");
            }
            _audioAttributesCompat = attributes;
            return this;
        }

        /**
         * Declare the intended behavior of the application with regards to audio ducking.
         * See more details in the {@link AudioFocusRequestCompat} class documentation.
         * @param pauseOnDuck use {@code true} if the application intends to pause audio playback
         *    when losing focus with
         *    {@link AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK}.
         *    If {@code true}, note that you must also set a focus listener to receive such an
         *    event, with
         *    {@link #setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener, Handler)}.
         * @return this {@code Builder} instance.
         */
        public @NonNull Builder setWillPauseWhenDucked(boolean pauseOnDuck) {
            _pauseOnDuck = pauseOnDuck;
            return this;
        }

        /**
         * Marks this focus request as compatible with delayed focus.
         * See more details about delayed focus in the {@link AudioFocusRequestCompat} class
         * documentation.
         * @param acceptsDelayedFocusGain use {@code true} if the application supports delayed
         *    focus. If {@code true}, note that you must also set a focus listener to be notified
         *    of delayed focus gain, with
         *    {@link #setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener, Handler)}.
         * @return this {@code Builder} instance
         */
        public @NonNull Builder setAcceptsDelayedFocusGain(boolean acceptsDelayedFocusGain) {
            _acceptsDelayedFocusGain = acceptsDelayedFocusGain;
            return this;
        }

        /**
         * Builds a new {@code AudioFocusRequestCompat} instance combining all the information
         * gathered by this {@code Builder}'s configuration methods.
         * @return the {@code AudioFocusRequestCompat} instance qualified by all the properties set
         *   on this {@code Builder}.
         * @throws IllegalStateException thrown when attempting to build a focus request that is set
         *    to accept delayed focus, or to pause on duck, but no focus change listener was set.
         */
        public AudioFocusRequestCompat build() {
            if((_acceptsDelayedFocusGain || _pauseOnDuck) && (_onAudioFocusChangeListener == null)) {
                throw new IllegalStateException(
                        "Can't use delayed focus or pause on duck without a listener");
            }
            return new AudioFocusRequestCompat(_focusGain,
                    _onAudioFocusChangeListener,
                    _focusChangeHandler,
                    _audioAttributesCompat,
                    _pauseOnDuck,
                    _acceptsDelayedFocusGain);
        }
    }
}
