package com.toneterrace.app.activities;

import android.app.AlertDialog;
import android.media.audiofx.Equalizer;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Build;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.toneterrace.app.services.MusicService;
import com.toneterrace.app.api.LyricsApi;
import com.toneterrace.app.api.RetrofitClient;
import com.toneterrace.app.models.LyricsResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.toneterrace.app.R;
import com.toneterrace.app.models.Song;
import com.toneterrace.app.ui.AudioVisualizerView;

import java.io.IOException;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ImageView imgAlbumArt, btnPlayPause, btnNext, btnPrev;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvLyrics;
    private SeekBar seekBar;
    private AudioVisualizerView visualizerView;

    private MediaPlayer mediaPlayer;
    private Visualizer visualizer;
    private Song currentSong;
    private boolean isPlaying = false;
    private Handler handler = new Handler();
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;

    private ImageView btnEqualizer; // New Button
    private List<Song> songList;    // New List
    private int songIndex;          // New Index

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 1. Get Data from Intent
        songIndex = getIntent().getIntExtra("SONG_INDEX", 0);
        songList = (List<Song>) getIntent().getSerializableExtra("PLAYLIST");

        if (songList != null && !songList.isEmpty()) {
            currentSong = songList.get(songIndex);
        }
        initViews();
        setupPlayer();
        fetchLyrics();
    }

    private void initViews() {
        imgAlbumArt = findViewById(R.id.imgPlayerAlbumArt);
        tvTitle = findViewById(R.id.tvPlayerTitle);
        tvArtist = findViewById(R.id.tvPlayerArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvLyrics = findViewById(R.id.tvLyrics);
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        visualizerView = findViewById(R.id.visualizerView);
        btnEqualizer = findViewById(R.id.btnEqualizer);

        btnPlayPause.setOnClickListener(v -> {
            if(!isPlaying) {
                btnPlayPause.setImageDrawable(getDrawable(R.drawable.ic_pause));
                togglePlayPause();
            }else{
                btnPlayPause.setImageDrawable(getDrawable(R.drawable.ic_play));
                togglePlayPause();
            }
        });

        // NEW: Next / Prev
        btnNext.setOnClickListener(v -> {
            if (musicService != null) musicService.playNext();
        });

        btnPrev.setOnClickListener(v -> {
            if (musicService != null) musicService.playPrev();
        });

        // NEW: Equalizer
        btnEqualizer.setOnClickListener(v -> showEqualizerDialog());

        if (currentSong != null) {
            tvTitle.setText(currentSong.title);
            tvArtist.setText(currentSong.artist);
            Glide.with(this).load(currentSong.albumArtUrl).circleCrop().into(imgAlbumArt);
        }
    }

    private void setupPlayer() {
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent); // Start it so it keeps running
        }

        // SeekBar Logic
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Only seek if the USER dragged it
                if (fromUser && musicService != null) {
                    musicService.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Update SeekBar every second
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicBound && musicService.isPng()) {
                    int mCurrentPosition = musicService.getPosn(); // Use Service, not mediaPlayer
                    seekBar.setProgress(mCurrentPosition);
                    tvCurrentTime.setText(formatTime(mCurrentPosition));
                }
                handler.postDelayed(this, 1000); // Repeat every 1 second
            }
        });
    }

    // New method called after service connects
    private void playAudio() {
        if (musicService != null && currentSong != null) {
//          musicService.playSong(currentSong.filePath, currentSong.title);
            musicService.playSong();
            setupVisualizer(); // Re-link visualizer

            tvTotalTime.setText(formatTime(musicService.getDur()));
            seekBar.setMax(musicService.getDur());
        }
    }

    private void startPlayback() {
        mediaPlayer.start();
        isPlaying = true;
        // visual update for button (change icon if you have one)

        setupVisualizer();
    }

    private void togglePlayPause() {
        if (musicService != null && musicBound) {
            if (musicService.isPng()) {
                musicService.pausePlayer();
                isPlaying = false;
            } else {
                musicService.resumePlayer();
                isPlaying = true;
            }
        }
    }

    // VISUALIZER LOGIC (MANDATORY REQUIREMENT)
    private void setupVisualizer() {
        if (visualizer != null) return;

        // Link visualizer to MediaPlayer ID
        visualizer = new Visualizer(musicService.getAudioSessionId());
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                // Send data to Custom View
                visualizerView.updateVisualizer(waveform);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                // Not using FFT for this simple bar view
            }
        }, Visualizer.getMaxCaptureRate() / 2, true, false);

        visualizer.setEnabled(true);
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicBound = true;

            // 1. Pass the list to Service
            musicService.setPlaylist(songList);
            musicService.setSong(songIndex);

            // 2. Play
            musicService.playSong();

            setupVisualizer();

            // 3. Setup Listener for Auto-Next
            musicService.setCallback(new MusicService.OnSongChangedListener() {
                @Override
                public void onSongChanged(Song newSong) {
                    // Update UI on Main Thread
                    runOnUiThread(() -> {
                        currentSong = newSong;
                        tvTitle.setText(newSong.title);
                        tvArtist.setText(newSong.artist);
                        tvTotalTime.setText(newSong.duration); // You might need to re-format logic if duration is raw ms
                        seekBar.setMax(musicService.getDur());
                        Glide.with(PlayerActivity.this).load(newSong.albumArtUrl).circleCrop().into(imgAlbumArt);

                        // Re-fetch lyrics for the new song
                        setupVisualizer();
                        fetchLyrics();
                    });
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private void fetchLyrics() {
        if (currentSong == null) return;

        tvLyrics.setText("Searching for lyrics...");

        String cleanTitle = currentSong.title.trim(); // .trim() removes accidental spaces
        String cleanArtist = currentSong.artist.trim();

        // 1. Log what we are sending
        android.util.Log.d("API_DEBUG", "Asking for: " + cleanArtist + " - " + cleanTitle);

        RetrofitClient.getLyricsApi().getLyrics(cleanArtist, cleanTitle)
                .enqueue(new Callback<LyricsResponse>() {
                    @Override
                    public void onResponse(Call<LyricsResponse> call, Response<LyricsResponse> response) {
                        // 2. Log the result code (200 = OK, 404 = Not Found)
                        android.util.Log.d("API_DEBUG", "Response Code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            String lyricsText = response.body().getLyrics();
                            android.util.Log.d("API_DEBUG", "Lyrics length: " + (lyricsText != null ? lyricsText.length() : "null"));

                            if (lyricsText != null && !lyricsText.isEmpty()) {
                                tvLyrics.setText(lyricsText);
                            } else {
                                tvLyrics.setText("Lyrics not found (Empty body).");
                            }
                        } else {
                            // 3. Log the error if it failed
                            try {
                                android.util.Log.e("API_DEBUG", "Error Body: " + response.errorBody().string());
                            } catch (Exception e) { e.printStackTrace(); }
                            tvLyrics.setText("No lyrics found for this song.");
                        }
                    }

                    @Override
                    public void onFailure(Call<LyricsResponse> call, Throwable t) {
                        // 4. Log network errors (No internet, timeout, etc.)
                        android.util.Log.e("API_DEBUG", "Network Failure: " + t.getMessage());
                        tvLyrics.setText("Error fetching lyrics. Check internet.");
                    }
                });
    }

    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void showEqualizerDialog() {
        if (musicService == null || musicService.getEqualizer() == null) {
            Toast.makeText(this, "Equalizer not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Equalizer equalizer = musicService.getEqualizer();
        short bands = equalizer.getNumberOfBands();
        short minEQLevel = equalizer.getBandLevelRange()[0];
        short maxEQLevel = equalizer.getBandLevelRange()[1];

        // Create a layout dynamically
        LinearLayout eqLayout = new LinearLayout(this);
        eqLayout.setOrientation(LinearLayout.VERTICAL);
        eqLayout.setPadding(30, 30, 30, 30);
        eqLayout.setBackgroundColor(android.graphics.Color.BLACK);

        for (short i = 0; i < bands; i++) {
            final short bandIndex = i;

            // Band Frequency Label (e.g., "60Hz")
            TextView freqView = new TextView(this);
            freqView.setText((equalizer.getCenterFreq(bandIndex) / 1000) + " Hz");
            freqView.setTextColor(android.graphics.Color.WHITE);
            freqView.setGravity(Gravity.CENTER_HORIZONTAL);
            eqLayout.addView(freqView);

            // Slider
            SeekBar bar = new SeekBar(this);
            bar.setMax(maxEQLevel - minEQLevel);
            bar.setProgress(equalizer.getBandLevel(bandIndex) - minEQLevel);

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        equalizer.setBandLevel(bandIndex, (short) (progress + minEQLevel));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            eqLayout.addView(bar);
        }

        // Show Dialog
        new AlertDialog.Builder(this)
                .setTitle("Equalizer")
                .setView(eqLayout)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicBound) {
            unbindService(musicConnection);
            musicBound = false;
        }
        // Don't release mediaPlayer here anymore, the Service handles it.
        if (visualizer != null) {
            visualizer.release();
        }
        handler.removeCallbacksAndMessages(null);
    }
}