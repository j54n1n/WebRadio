package io.github.j54n1n.webradio;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.List;

import io.github.j54n1n.webradio.service.PlaybackInfoListener;
import io.github.j54n1n.webradio.service.PlayerAdapter;
import io.github.j54n1n.webradio.service.contentcatalogs.WebRadioLibrary;
import io.github.j54n1n.webradio.service.notifications.MediaNotificationManager;
import io.github.j54n1n.webradio.service.players.MediaPlayerAdapter;

// TODO: Service gets not eliminated when swiped away in Android Auto.
public class PlaybackService extends MediaBrowserServiceCompat {

    private static final String TAG = PlaybackService.class.getSimpleName();

    private static final int MEDIA_SESSION_FLAGS = MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;

    private MediaSessionCompat mediaSession;
    private PlayerAdapter playerAdapter;
    private MediaNotificationManager mediaNotificationManager;
    //private MediaSessionCallback callback;
    private boolean isServiceInStartedState;

    private final MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {

        // TODO: Override queue methods to support FLAG_HANDLES_QUEUE_COMMANDS.

        @Override
        public void onPlay() {
            final String currentMediaId = playerAdapter.getCurrentMediaId();
            if(currentMediaId != null) {
                onPlayFromMediaId(currentMediaId, null);
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mediaSession.setActive(true);
            MediaMetadataCompat mediaMetadata =
                    WebRadioLibrary.getMetadata(getApplicationContext(), mediaId);
            mediaSession.setMetadata(mediaMetadata);
            playerAdapter.playFromMedia(mediaMetadata);
        }

        @Override
        public void onPause() {
            playerAdapter.pause();
        }

        @Override
        public void onSkipToNext() {
            onPlayFromMediaId(WebRadioLibrary.getNextStream(
                    playerAdapter.getCurrentMediaId()), null);
        }

        @Override
        public void onSkipToPrevious() {
            onPlayFromMediaId(WebRadioLibrary.getPreviousStream(
                    playerAdapter.getCurrentMediaId()), null);
        }

        @Override
        public void onStop() {
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG);
        mediaSession.setCallback(callback);
        mediaSession.setFlags(MEDIA_SESSION_FLAGS);
        setSessionToken(mediaSession.getSessionToken());
        // TODO: Check NotificationManager.
        mediaNotificationManager = new MediaNotificationManager(this);
        // TODO: Check PlayerAdapter (PlaybackManager).
        playerAdapter = new MediaPlayerAdapter(
                getApplicationContext(), new MediaPlayerListener());
        /*
        // Create a new MediaSession.
        mediaSession = new MediaSessionCompat(this, TAG);
        callback = new MediaSessionCallback();
        mediaSession.setCallback(callback);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mediaSession.getSessionToken());
        mediaNotificationManager = new MediaNotificationManager(this);
        playerAdapter = new MediaPlayerAdapter(this, new MediaPlayerListener());
        */
        Log.d(TAG, "onCreate: " + TAG + " creating MediaSession and MediaNotificationManager");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mediaNotificationManager.onDestroy();
        playerAdapter.stop();
        mediaSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped and MediaSession released");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 @Nullable Bundle rootHints) {
        Log.d(TAG, "onGetRoot: ");
        //getString(R.string.app_name), // Name visible in Android Auto
        return new BrowserRoot(WebRadioLibrary.getRoot(), null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren: ");
        result.sendResult(WebRadioLibrary.getMediaItems());
    }

    /*
    // TODO: Edit this callback
    // MediaSession Callback: Transport Controls -> MediaPlayerAdapter
    public class MediaSessionCallback extends MediaSessionCompat.Callback {

        private final List<MediaSessionCompat.QueueItem> playlist = new ArrayList<>();
        private int queueIndex = -1;
        private MediaMetadataCompat preparedMedia;

        public MediaSessionCallback() {
            super();
            Log.d(TAG, "MediaSessionCallback: ");
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onAddQueueItem: ");
            playlist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            queueIndex = (queueIndex == -1) ? 0 : queueIndex;
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onRemoveQueueItem: ");
            playlist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            queueIndex = (playlist.isEmpty()) ? -1 : queueIndex;
        }

        @Override
        public void onPrepare() {
            Log.d(TAG, "onPrepare: ");
            if(queueIndex < 0 && playlist.isEmpty()) {
                // Nothing to play.
                return;
            }
            final String mediaId = playlist.get(queueIndex).getDescription().getMediaId();
            preparedMedia = WebRadioLibrary.getMetadata(PlaybackService.this, mediaId);
            mediaSession.setMetadata(preparedMedia);
            if(!mediaSession.isActive()) {
                mediaSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            if(!isReadyToPlay()) {
                // Nothing to play.
                return;
            }
            if(preparedMedia == null) {
                onPrepare();
            }
            playerAdapter.playFromMedia(preparedMedia);
            Log.d(TAG, "onPlayFromMediaId: MediaSession active");
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause: ");
            playerAdapter.pause();
        }

        @Override
        public void onStop() {
            playerAdapter.stop();
            mediaSession.setActive(false);
        }

        @Override
        public void onSkipToNext() {
            queueIndex = (++queueIndex % playlist.size());
            preparedMedia = null;
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            queueIndex = queueIndex > 0 ? queueIndex - 1 : playlist.size() - 1;
            preparedMedia = null;
            onPlay();
        }

        @Override
        public void onSeekTo(long pos) {
            playerAdapter.seekTo(pos);
        }

        private boolean isReadyToPlay() {
            return (!playlist.isEmpty());
        }
    }
    */

    // MediaPlayerAdapter Callback: MediaPlayerAdapter state -> PlaybackService.
    public class MediaPlayerListener extends PlaybackInfoListener {

        private final ServiceManager serviceManager;

        MediaPlayerListener() {
            serviceManager = new ServiceManager();
        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mediaSession.setPlaybackState(state);
            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    serviceManager.moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    serviceManager.updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    serviceManager.moveServiceOutOfStartedState(state);
                    break;
            }
        }

        class ServiceManager {

            private void moveServiceToStartedState(PlaybackStateCompat state) {
                Notification notification =
                        mediaNotificationManager.getNotification(
                                playerAdapter.getCurrentMedia(), state, getSessionToken());
                if (!isServiceInStartedState) {
                    ContextCompat.startForegroundService(
                            PlaybackService.this,
                            new Intent(PlaybackService.this, PlaybackService.class));
                    isServiceInStartedState = true;
                }
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            private void updateNotificationForPause(PlaybackStateCompat state) {
                stopForeground(false);
                Notification notification =
                        mediaNotificationManager.getNotification(
                                playerAdapter.getCurrentMedia(), state, getSessionToken());
                mediaNotificationManager.getNotificationManager()
                        .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                stopSelf();
                isServiceInStartedState = false;
            }
        }
    }
}
