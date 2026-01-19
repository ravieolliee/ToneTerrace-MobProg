package com.toneterrace.app.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.toneterrace.app.models.Song;

import java.util.ArrayList;
import java.util.List;

public class MusicLoader {

    public static List<Song> loadSongs(Context context) {
        List<Song> songList = new ArrayList<>();

        // 1. Define the Uri (Where to look)
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        // 2. Define Columns (What data we want)
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID // We use this to find album art
        };

        // 3. Filter (Only music, skip notifications/ringtones)
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        // 4. Run the Query
        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                null
        )) {
            // 5. Loop through results
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String album = cursor.getString(albumColumn);
                    long durationMs = cursor.getLong(durationColumn);
                    long albumId = cursor.getLong(albumIdColumn);

                    // Build the Content Uri (The playable path)
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    String path = contentUri.toString();

                    // Build Album Art Uri
                    Uri albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), albumId);
                    String artUrl = albumArtUri.toString();

                    // Format Duration
                    String durationStr = formatTime(durationMs);

                    // Create Song Object
                    Song song = new Song(title, artist, album, durationStr, path, artUrl);
                    songList.add(song);
                }
            }
        } catch (Exception e) {
            Log.e("MusicLoader", "Error loading music", e);
        }

        return songList;
    }

    private static String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}