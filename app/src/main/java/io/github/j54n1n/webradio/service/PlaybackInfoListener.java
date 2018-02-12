package io.github.j54n1n.webradio.service;

import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Listener to provide state updates from
 * {@link io.github.j54n1n.webradio.service.players.MediaPlayerAdapter} (the media player)
 * to {@link io.github.j54n1n.webradio.PlaybackService} (the service that holds our
 * {@link android.support.v4.media.session.MediaSessionCompat}).
 */
public abstract class PlaybackInfoListener {

    public abstract void onPlaybackStateChange(PlaybackStateCompat state);

    public void onPlaybackCompleted() { }
}
