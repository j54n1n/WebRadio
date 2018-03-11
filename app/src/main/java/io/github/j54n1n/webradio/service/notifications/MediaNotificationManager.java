package io.github.j54n1n.webradio.service.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import io.github.j54n1n.webradio.PlayerActivity;
import io.github.j54n1n.webradio.PlaybackService;
import io.github.j54n1n.webradio.R;
import io.github.j54n1n.webradio.service.contentcatalogs.WebRadioLibrary;

/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is
 * required so that the music service don't get killed during playback.
 */
public class MediaNotificationManager {

    public static final int NOTIFICATION_ID = 1;

    private static final String TAG = MediaNotificationManager.class.getSimpleName();

    private static final String CHANNEL_ID =
            PlaybackService.class.getPackage().getName() + ".channel";
    private static final int REQUEST_CODE = 2;

    private final PlaybackService service;
    private final NotificationManager notificationManager;
    private final NotificationCompat.Action actionPlay;
    private final NotificationCompat.Action actionPause;
    private final NotificationCompat.Action actionNext;
    private final NotificationCompat.Action actionPrev;

    public MediaNotificationManager(PlaybackService service) {
        this.service = service;
        notificationManager =
                (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        actionPlay = new NotificationCompat.Action(
                R.drawable.ic_play_arrow_white_24dp,
                service.getString(R.string.label_play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_PLAY));
        actionPause = new NotificationCompat.Action(
                R.drawable.ic_pause_white_24dp,
                service.getString(R.string.label_pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_PAUSE));
        actionNext = new NotificationCompat.Action(
                R.drawable.ic_skip_next_white_24dp,
                service.getString(R.string.label_next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        actionPrev = new NotificationCompat.Action(
                R.drawable.ic_skip_previous_white_24dp,
                service.getString(R.string.label_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        // Cancel all notifications to handle the case where the Service was killed and restarted
        // by the system.
        if(notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public Notification getNotification(MediaMetadataCompat metadata,
                                        @NonNull PlaybackStateCompat state,
                                        MediaSessionCompat.Token token) {
        final boolean isPlaying = (state.getState() == PlaybackStateCompat.STATE_PLAYING);
        MediaDescriptionCompat description = metadata.getDescription();
        NotificationCompat.Builder builder =
                buildNotification(state, token, isPlaying, description);
        return builder.build();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        if(notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            CharSequence name = "MediaSession";
            // The user-visible description of the channel.
            String description = "MediaSession Player";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Configure the notification channel.
            channel.setDescription(description);
            channel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            // TODO: Customize notification color and pattern.
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(
                    new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "createChannel: New channel created");
        } else {
            Log.d(TAG, "createChannel: Existing channel reused");
        }
    }

    private NotificationCompat.Builder buildNotification(@NonNull PlaybackStateCompat state,
                                                         MediaSessionCompat.Token token,
                                                         boolean isPlaying,
                                                         MediaDescriptionCompat description) {
        // Create the (mandatory) notification channel when running on Android Oreo.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(); // TODO: Provide alternative notification light path for pre Android O.
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANNEL_ID);
        builder.setStyle(new MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2)
                // For backwards compatibility with Android L and earlier.
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_STOP)))
                .setColor(ContextCompat.getColor(service, R.color.notification_bg))
                .setSmallIcon(R.drawable.ic_stat_image_audiotrack)
                // Open app pending intent that is fired when user clicks on notification.
                .setContentIntent(createContentIntent())
                // Title - Usually Song name.
                .setContentTitle(description.getTitle())
                // Subtitle - Usually Artist name.
                .setContentText(description.getSubtitle())
                .setLargeIcon(WebRadioLibrary.getAlbumBitmap(service, description.getMediaId()))
                // When notification is deleted (when playback is paused and notification can be
                // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service, PlaybackStateCompat.ACTION_STOP))
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        // If skip to next action is enabled.
        if((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(actionPrev);
        }
        builder.addAction(isPlaying ? actionPause : actionPlay);
        // If skip to prev action is enabled.
        if((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            builder.addAction(actionNext);
        }
        return builder;
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(service, PlayerActivity.class);
        // Reuse existing activity task. See also AndroidManifest.xml
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                service, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
