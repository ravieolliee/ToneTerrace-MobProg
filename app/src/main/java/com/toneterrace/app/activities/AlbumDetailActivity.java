package com.toneterrace.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.toneterrace.app.R;
import com.toneterrace.app.adapters.SongAdapter;
import com.toneterrace.app.database.MusicDatabase;
import com.toneterrace.app.models.Song;

import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity {

    private ImageView imgAlbumArt;
    private TextView tvAlbumTitle, tvArtist;
    private RecyclerView recyclerView;
    private MusicDatabase db;
    private Song currentAlbumData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        db = MusicDatabase.getDatabase(this);

        // 1. Get Data from Intent
        // We passed the entire 'Song' object representing the album from MainActivity
        currentAlbumData = (Song) getIntent().getSerializableExtra("ALBUM_DATA");

        initViews();
        setupHeader();
        setupSongList();
    }

    private void initViews() {
        imgAlbumArt = findViewById(R.id.imgDetailAlbumArt);
        tvAlbumTitle = findViewById(R.id.tvDetailAlbumTitle);
        tvArtist = findViewById(R.id.tvDetailArtist);
        recyclerView = findViewById(R.id.recyclerViewSongs);
    }

    private void setupHeader() {
        if (currentAlbumData != null) {
            tvAlbumTitle.setText(currentAlbumData.album);
            tvArtist.setText(currentAlbumData.artist);

            if (currentAlbumData.albumArtUrl != null && !currentAlbumData.albumArtUrl.isEmpty()) {
                Glide.with(this)
                        .load(currentAlbumData.albumArtUrl)
                        .placeholder(R.mipmap.ic_album_placeholder) // Show mipmap while loading
                        .error(R.mipmap.ic_album_placeholder)       // Show mipmap if URL is broken
                        .into(imgAlbumArt);
            } else {
                // If NO URL exists at all, explicitly set the mipmap resource
                imgAlbumArt.setImageResource(R.mipmap.ic_album_placeholder);
            }
        }
    }

    private void setupSongList() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (currentAlbumData != null) {
            // QUERY: Get songs ONLY for this album
            List<Song> albumSongs = db.songDao().getSongsByAlbum(currentAlbumData.album);

            SongAdapter adapter = new SongAdapter(this, albumSongs, new SongAdapter.OnSongClickListener() {
                @Override
                public void onSongClick(Song song) {
                    // Create Intent
                    Intent intent = new Intent(AlbumDetailActivity.this, PlayerActivity.class);

                    // PASS THE LIST via a Static Reference in MusicService (Recommended for large lists)
                    // Or since we have a singleton-like service structure, we can pass via Intent for now.
                    // Important: For this specific app structure, we will pass the LIST via Intent
                    // assuming the list isn't massive (less than 2000 songs).

                    intent.putExtra("SONG_INDEX", albumSongs.indexOf(song));
                    intent.putExtra("PLAYLIST", (java.io.Serializable) albumSongs);

                    startActivity(intent);
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }
}