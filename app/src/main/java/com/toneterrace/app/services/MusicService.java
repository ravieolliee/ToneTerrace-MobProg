package com.toneterrace.app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.toneterrace.app.R;
import com.toneterrace.app.activities.PlayerActivity;
import com.toneterrace.app.models.Song;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {

    private MediaPlayer mediaPlayer;
    private Equalizer mEqualizer; // Equalizer Object
    private final IBinder musicBind = new MusicBinder();

    // Playlist Management
    private List<Song> songs = new ArrayList<>();
    private int songPosn = 0;

    // Callback to update UI when song changes automatically
    private OnSongChangedListener callback;

    public interface OnSongChangedListener {
        void onSongChanged(Song newSong);
    }

    public void setCallback(OnSongChangedListener callback) {
        this.callback = callback;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songPosn = 0;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        createNotificationChannel();
    }

    // 1. SET PLAYLIST
    public void setPlaylist(List<Song> theSongs) {
        songs = theSongs;
    }

    public void setSong(int songIndex) {
        songPosn = songIndex;
    }

    // 2. PLAY SONG logic
    public void playSong() {
        mediaPlayer.reset();

        // Validation
        if (songs == null || songPosn < 0 || songPosn >= songs.size()) return;

        Song playSong = songs.get(songPosn);
        String songPath = playSong.filePath;

        try {
            if (songPath.startsWith("content://")) {
                mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(songPath));
            } else {
                mediaPlayer.setDataSource(songPath);
            }
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();

                // Initialize Equalizer after MediaPlayer is ready
                setupEqualizer();

                showNotification(playSong.title);
                // Notify Activity to update UI (Title, Artist, Album Art)
                if (callback != null) callback.onSongChanged(playSong);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. SEEKING
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    // 4. NEXT / PREV
    public void playPrev() {
        songPosn--;
        if (songPosn < 0) songPosn = songs.size() - 1;
        playSong();
    }

    public void playNext() {
        songPosn++;
        if (songPosn >= songs.size()) songPosn = 0;
        playSong();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // Auto-play next song when current one ends
        if (mediaPlayer.getCurrentPosition() > 0) {
            mediaPlayer.reset();
            playNext();
        }
    }

    // 5. EQUALIZER SETUP
    public void setupEqualizer() {
        if (mEqualizer != null) {
            // If exists, detach to avoid conflicts
            try { mEqualizer.release(); } catch (Exception e) {}
        }

        // Create new Equalizer linked to this MediaPlayer
        mEqualizer = new Equalizer(0, mediaPlayer.getAudioSessionId());
        mEqualizer.setEnabled(true);
    }

    public Equalizer getEqualizer() {
        return mEqualizer;
    }

    // Standard Controls
    public int getPosn() { return mediaPlayer.getCurrentPosition(); }
    public int getDur() { return mediaPlayer.getDuration(); }
    public boolean isPng() { return mediaPlayer.isPlaying(); }
    public void pausePlayer() { mediaPlayer.pause(); }
    public void resumePlayer() { mediaPlayer.start(); }
    public Song getCurrentSong() {
        if(songs != null && !songs.isEmpty()) return songs.get(songPosn);
        return null;
    }
    public int getAudioSessionId() { return mediaPlayer.getAudioSessionId(); }

    private void showNotification(String title) {
        Intent notIntent = new Intent(this, PlayerActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MUSIC_CHANNEL")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Playing")
                .setContentText(title)
                .setContentIntent(pendInt)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "MUSIC_CHANNEL", "Music Playback", NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return musicBind; }
    @Override public boolean onUnbind(Intent intent) { return true; } // Allow rebind

    @Override
    public void onDestroy() {
        if (mEqualizer != null) mEqualizer.release();
        if (mediaPlayer != null) mediaPlayer.release();
        super.onDestroy();
    }
}