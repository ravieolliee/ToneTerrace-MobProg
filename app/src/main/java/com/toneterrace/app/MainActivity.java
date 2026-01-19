package com.toneterrace.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.toneterrace.app.utils.MusicLoader;
import androidx.appcompat.widget.SearchView;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.media.MediaScannerConnection;
import android.os.Environment;

import com.toneterrace.app.adapters.AlbumAdapter;
import com.toneterrace.app.database.MusicDatabase;
import com.toneterrace.app.models.Song;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MusicDatabase db;
    private RecyclerView recyclerView;
    private AlbumAdapter adapter;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = MusicDatabase.getDatabase(this);

//      seedDatabase();
        checkPermissionAndLoad();

        // 2. Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewAlbums);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchView = findViewById(R.id.searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAlbums(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAlbums(newText); // Real-time search
                return true;
            }
        });

        // 1. Bind View
        ImageView btnRefresh = findViewById(R.id.btnRefresh);

        // 2. Set Listener
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Scanning device storage...", Toast.LENGTH_SHORT).show();
            forceScanAndLoad(); // Use the new method
        });

        // 3. Get Data (Grouped by Album)
        List<Song> uniqueAlbums = db.songDao().getUniqueAlbums();

        // 4. Initialize Adapter
        adapter = new AlbumAdapter(this, uniqueAlbums, new AlbumAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(Song song) {
                // Navigate to Detail Page
                Intent intent = new Intent(MainActivity.this, com.toneterrace.app.activities.AlbumDetailActivity.class);

                // Pass the song object so the next screen knows which album to load
                intent.putExtra("ALBUM_DATA", song);

                startActivity(intent);
            }
        });

        // 5. Set Adapter
        recyclerView.setAdapter(adapter);
    }
    private void forceScanAndLoad() {
        // Path to standard Download folder
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

        // Scan the folder
        MediaScannerConnection.scanFile(this, new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        // Once scan is done, run our normal Import logic
                        runOnUiThread(() -> {
                            android.util.Log.d("SCAN", "Scan finished: " + path);
                            importMusic(); // Call the original loader
                        });
                    }
                });
    }

    private void filterAlbums(String query) {
        List<Song> filteredList;

        if (query.isEmpty()) {
            // If empty, show all unique albums again
            filteredList = db.songDao().getUniqueAlbums();
        } else {
            // Search in DB (We already wrote this query in Phase 4!)
            filteredList = db.songDao().searchSongs(query);

            // Note: Our DAO returns individual songs.
            // For the Home Page, you might want to filter duplicate albums manually here
            // if the search returns multiple songs from the same album.
        }

        // Update the Adapter
        // We need to add a method to AlbumAdapter to update data dynamically
        adapter.updateList(filteredList);
    }

    private void checkPermissionAndLoad() {
        List<String> permissionsToRequest = new ArrayList<>();

        // 1. Determine Storage Permission (Based on Android Version)
        String storagePermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        // Check if Storage is granted
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission);
        }

        // 2. Check Microphone Permission (REQUIRED for Visualizer)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        // 3. Request missing permissions or Proceed
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 200);
        } else {
            // All permissions already granted
            importMusic();
        }
    }

    // Handle user response to permission popup
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 200) {
            boolean storageGranted = false;
            boolean audioGranted = false;

            // Loop through results to see what was granted
            for (int i = 0; i < permissions.length; i++) {
                String perm = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (perm.equals(Manifest.permission.READ_MEDIA_AUDIO) ||
                            perm.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        storageGranted = true;
                    }
                    if (perm.equals(Manifest.permission.RECORD_AUDIO)) {
                        audioGranted = true;
                    }
                }
            }

            if (storageGranted) {
                importMusic();
                if (!audioGranted) {
                    Toast.makeText(this, "Microphone denied. Visualizer will be disabled.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot load songs.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void importMusic() {
        new Thread(() -> {
            // 1. Scan device
            List<Song> deviceSongs = MusicLoader.loadSongs(this);

            // 2. Clear OLD data to prevent duplicates (The Fix!)
            db.songDao().deleteAll();
            // Note: You need to add this method to SongDao (see Step 3)

            // 3. Insert NEW data
            if (!deviceSongs.isEmpty()) {
                db.songDao().insertAll(deviceSongs);

                runOnUiThread(() -> {
                    List<Song> allSongs = db.songDao().getUniqueAlbums();
                    adapter.updateList(allSongs);
                    Toast.makeText(MainActivity.this, "Library Updated: " + deviceSongs.size() + " songs", Toast.LENGTH_SHORT).show();
                });
            } else {
                // If device is empty, maybe add 1 working dummy song for testing
                addWorkingDummySong();
            }
        }).start();
    }

    private void addWorkingDummySong() {
        // Only runs if no local music found
        List<Song> dummy = new ArrayList<>();
        // This is a REAL URL that actually plays music
        dummy.add(new Song("Sci-Fi", "Bensound", "Demo Album", "2:00",
                "https://www.bensound.com/bensound-music/bensound-scifi.mp3", ""));
        dummy.add(new Song(
                "Impact Moderato",
                "Kevin MacLeod",
                "YouTube Audio Library",
                "3:00",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                "https://via.placeholder.com/150"
        ));
        dummy.add(new Song(
                "Impact Allegro",
                "Kevin MacLeod",
                "YouTube Audio Library",
                "3:15",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                "https://via.placeholder.com/150"
        ));

        db.songDao().insertAll(dummy);

        runOnUiThread(() -> {
            adapter.updateList(db.songDao().getUniqueAlbums());
        });
    }

}