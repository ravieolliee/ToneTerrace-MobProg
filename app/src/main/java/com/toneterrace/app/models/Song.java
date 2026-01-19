package com.toneterrace.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

// This annotation tells Room to create a table named "songs"
@Entity(tableName = "songs")
public class Song implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id; // Unique ID for every song

    public String title;
    public String artist;
    public String album;
    public String duration; // e.g., "3:45"
    public String filePath; // URL (online) or Path (local file)
    public String albumArtUrl; // Image URL
    public boolean isFavorite; // To mark favorites later

    // Empty Constructor (Required by Room)
    public Song() {
    }

    // Constructor for creating new songs easily
    public Song(String title, String artist, String album, String duration, String filePath, String albumArtUrl) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.filePath = filePath;
        this.albumArtUrl = albumArtUrl;
        this.isFavorite = false;
    }
}