package io.github.j54n1n.webradio.service.contentcatalogs;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import io.github.j54n1n.webradio.BuildConfig;
import io.github.j54n1n.webradio.R;

public class WebRadioLibrary {

    private static final TreeMap<String, MediaMetadataCompat> streams = new TreeMap<>();
    private static final HashMap<String, Integer> streamResIds = new HashMap<>();
    private static final HashMap<String, String> streamUrls = new HashMap<>();

    static {
        createMediaMetadataCompat(
                "BBC_World_Service_UK",
                "BBC World Service UK",
                "BBC",
                "International news,...",
                "News",
                "http://sc4.vie.llnw.net:80/stream/bbcwssc_mp1_ws-eieuk",
                R.mipmap.ic_launcher,
                "ic_launcher");
        createMediaMetadataCompat(
                "BBC_Radio_One",
                "BBC Radio One",
                "BBC",
                "The best new music",
                "Music",
                "http://bbcmedia.ic.llnwd.net/stream/bbcmedia_radio1_mf_p?s=1518430077&e=1518444477&h=35085dc1a0bbcf97d25ef1146c1b51a4",
                R.mipmap.ic_launcher,
                "ic_launcher");
    }

    public static String getRoot() {
        return "root";
    }

    private static int getAlbumRes(String mediaId) {
        return streamResIds.containsKey(mediaId) ? streamResIds.get(mediaId) : 0;
    }

    public static String getWebRadioUrl(String mediaId) {
        return streamUrls.containsKey(mediaId) ? streamUrls.get(mediaId) : null;
    }

    public static Bitmap getAlbumBitmap(Context context, String mediaId) {
        return BitmapFactory.decodeResource(context.getResources(),
                WebRadioLibrary.getAlbumRes(mediaId));
    }

    public static List<MediaBrowserCompat.MediaItem> getMediaItems() {
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        for(MediaMetadataCompat metadata : streams.values()) {
            result.add(new MediaBrowserCompat.MediaItem(
                    metadata.getDescription(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }

    public static MediaMetadataCompat getMetadata(Context context, String mediaId) {
        MediaMetadataCompat metadataWithoutBitmap = streams.get(mediaId);
        Bitmap streamArt = getAlbumBitmap(context, mediaId);
        // Since MediaMetadataCompat is immutable, we need to create a copy to set the album art.
        // We don't set it initially on all items so that they don't take unnecessary memory.
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        for (String key :
                new String[]{
                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                        MediaMetadataCompat.METADATA_KEY_ALBUM,
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        MediaMetadataCompat.METADATA_KEY_GENRE,
                        MediaMetadataCompat.METADATA_KEY_TITLE
                }) {
            builder.putString(key, metadataWithoutBitmap.getString(key));
        }
        /*builder.putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                metadataWithoutBitmap.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));*/
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, streamArt);
        return builder.build();
    }

    private static void createMediaMetadataCompat(
            String mediaId,
            String title,
            String artist,
            String album,
            String genre,
            //long duration,
            //TimeUnit durationUnit,
            String streamUrl,
            int albumArtResId,
            String albumArtResName) {
        streams.put(
                mediaId,
                new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                        //.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        //        TimeUnit.MILLISECONDS.convert(duration, durationUnit))
                        .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                getAlbumArtUri(albumArtResName))
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                                getAlbumArtUri(albumArtResName))
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .build());
        streamResIds.put(mediaId, albumArtResId);
        streamUrls.put(mediaId, streamUrl);
    }

    private static String getAlbumArtUri(String albumArtResName) {
        return ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                //BuildConfig.APPLICATION_ID + "/drawable/" + albumArtResName;
                BuildConfig.APPLICATION_ID + "/mipmap/" + "ic_launcher"; //albumArtResName;
    }

    public static String getPreviousStream(String currentMediaId) {
        String prevMediaId = streams.lowerKey(currentMediaId);
        if(prevMediaId == null) {
            prevMediaId = streams.lastKey();
        }
        return prevMediaId;
    }

    public static String getNextStream(String currentMediaId) {
        String nextMediaId = streams.higherKey(currentMediaId);
        if(nextMediaId == null) {
            nextMediaId = streams.firstKey();
        }
        return nextMediaId;
    }
}
