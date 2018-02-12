package io.github.j54n1n.webradio.client;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.github.j54n1n.webradio.PlaybackService;

/**
 * Adapter for a MediaBrowser that handles connecting, disconnecting, and basic browsing.
 */
public class MediaBrowserAdapter {

    private static final String TAG = MediaBrowserAdapter.class.getSimpleName();

    /**
     * Helper class for easily subscribing to changes in a MediaBrowserService connection.
     */
    public static abstract class MediaBrowserChangeListener {

        public void onConnected(@Nullable MediaControllerCompat mediaController) { }
        public void onMetadataChanged(@Nullable MediaMetadataCompat mediaMetadata) { }
        public void onPlaybackStateChanged(@Nullable PlaybackStateCompat playbackState) { }
    }

    private final Context context;
    private final InternalState state;
    private final MediaBrowserConnectionCallback mediaBrowserConnectionCallback;
    private final MediaControllerCallback mediaControllerCallback;
    private final MediaBrowserSubscriptionCallback mediaBrowserSubscriptionCallback;
    private final List<MediaBrowserChangeListener> listeners;

    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;

    public MediaBrowserAdapter(@NonNull Context context) {
        this.context = context;
        state = new InternalState();
        mediaBrowserConnectionCallback = new MediaBrowserConnectionCallback();
        mediaControllerCallback = new MediaControllerCallback();
        mediaBrowserSubscriptionCallback = new MediaBrowserSubscriptionCallback();
        listeners = new ArrayList<>();
    }

    public void onStart() {
        if(mediaBrowser == null) {
            mediaBrowser = new MediaBrowserCompat(
                    context,
                    new ComponentName(context, PlaybackService.class),
                    mediaBrowserConnectionCallback,
                    null);
            mediaBrowser.connect();
            Log.d(TAG, "onStart: Creating MediaBrowser and connecting");
        }
    }

    public void onStop() {
        if(mediaController != null) {
            mediaController.unregisterCallback(mediaControllerCallback);
            mediaController = null;
        }
        if((mediaBrowser != null) && mediaBrowser.isConnected()) {
            mediaBrowser.disconnect();
            mediaBrowser = null;
        }
        resetState();
        Log.d(TAG, "onStop: Releasing MediaController, Disconnecting from MediaBrowser");
    }

    private interface ListenerCommand {
        void perform(@NonNull MediaBrowserChangeListener listener);
    }

    /**
     * The internal state of the app needs to revert to what it looks like when it started before
     * any connections to the {@link PlaybackService} happens via the
     * {@link android.support.v4.media.session.MediaSessionCompat}.
     */
    private void resetState() {
        state.reset();
        performOnAllListeners(new ListenerCommand() {
            @Override
            public void perform(@NonNull MediaBrowserChangeListener listener) {
                listener.onPlaybackStateChanged(null);
            }
        });
        Log.d(TAG, "resetState: ");
    }

    private void performOnAllListeners(@NonNull ListenerCommand command) {
        for(MediaBrowserChangeListener listener : listeners) {
            if(listener != null) {
                try {
                    command.perform(listener);
                } catch(Exception e) {
                    removeListener(listener);
                }
            }
        }
    }

    public void removeListener(MediaBrowserChangeListener listener) {
        if(listener != null) {
            if(listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }

    public void addListener(MediaBrowserChangeListener listener) {
        if(listener != null) {
            listeners.add(listener);
        }
    }

    public MediaControllerCompat.TransportControls getTransportControls() {
        if(mediaController == null) {
            Log.d(TAG, "getTransportControls: MediaController is null!");
            throw new IllegalStateException();
        }
        return mediaController.getTransportControls();
    }

    // Receives callbacks from the MediaBrowser when it has successfully connected to the
    // MediaBrowserService (PlaybackService).
    class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        // Happens as a result of onStart().
        @Override
        public void onConnected() {
            try {
                // Get a MediaController for the MediaSession.
                mediaController = new MediaControllerCompat(
                        context, mediaBrowser.getSessionToken());
                mediaController.registerCallback(mediaControllerCallback);
                // Sync existing MediaSession state to the UI.
                mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
                mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
                performOnAllListeners(new ListenerCommand() {
                    @Override
                    public void perform(@NonNull MediaBrowserChangeListener listener) {
                        listener.onConnected(mediaController);
                    }
                });
            } catch(RemoteException e) {
                Log.d(TAG, String.format("onConnected: Problem: %s", e.toString()));
                throw new RuntimeException(e);
            }
            mediaBrowser.subscribe(mediaBrowser.getRoot(), mediaBrowserSubscriptionCallback);
        }
    }

    // Receives callbacks from the MediaBrowser when the MediaBrowserService has loaded new media
    // that is ready for playback.
    class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            assert(mediaController != null);
            // Queue up all media items for this simple sample.
            // TODO: Load async.
            for(MediaBrowserCompat.MediaItem mediaItem : children) {
                mediaController.addQueueItem(mediaItem.getDescription());
            }
            // Call "playFromMedia" so the UI is updated.
            mediaController.getTransportControls().prepare();
        }
    }

    // Receives callbacks from the MediaController and updates the UI state,
    // i.e.: Which is the current item, whether it's playing or paused, etc.
    class MediaControllerCallback extends MediaControllerCompat.Callback {

        private boolean isMediaIdEqual(MediaMetadataCompat currentMedia,
                                       MediaMetadataCompat newMedia) {
            if((currentMedia == null) || (newMedia == null)) {
                return false;
            }
            final String currentMediaId =
                    currentMedia.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            final String newMediaId =
                    newMedia.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            return (currentMediaId.equals(newMediaId));
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            // Filtering out needless updates, given that the metadata has not changed.
            if(isMediaIdEqual(metadata, state.getMediaMetadata())) {
                Log.d(TAG, "onMetadataChanged: Filtering out needless onMetadataChanged() update");
                return;
            } else {
                state.setMediaMetadata(metadata);
            }
            performOnAllListeners(new ListenerCommand() {
                @Override
                public void perform(@NonNull MediaBrowserChangeListener listener) {
                    listener.onMetadataChanged(metadata);
                }
            });
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackStateCompat state) {
            MediaBrowserAdapter.this.state.setPlaybackState(state);
            performOnAllListeners(new ListenerCommand() {
                @Override
                public void perform(@NonNull MediaBrowserChangeListener listener) {
                    listener.onPlaybackStateChanged(state);
                }
            });
        }

        // This might happen if the MusicService is killed while the Activity is in the foreground
        // and onStart() has been called (but not onStop()).
        @Override
        public void onSessionDestroyed() {
            resetState();
            onPlaybackStateChanged(null);
            Log.d(TAG, "onSessionDestroyed: PlaybackService died!!!");
            super.onSessionDestroyed();
        }
    }

    // A holder class that contains the internal state.
    private static class InternalState {

        private PlaybackStateCompat playbackState;
        private MediaMetadataCompat mediaMetadata;

        public void reset() {
            playbackState = null;
            mediaMetadata = null;
        }

        public PlaybackStateCompat getPlaybackState() {
            return playbackState;
        }

        public void setPlaybackState(PlaybackStateCompat playbackState) {
            this.playbackState = playbackState;
        }

        public MediaMetadataCompat getMediaMetadata() {
            return mediaMetadata;
        }

        public void setMediaMetadata(MediaMetadataCompat mediaMetadata) {
            this.mediaMetadata = mediaMetadata;
        }
    }
}
