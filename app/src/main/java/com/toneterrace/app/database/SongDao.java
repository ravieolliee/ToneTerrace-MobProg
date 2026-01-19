package com.toneterrace.app.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.toneterrace.app.models.Song;

import java.util.List;

@Dao
public interface SongDao {

    // 1. Insert a new song
    @Insert
    void insertSong(Song song);

    // 2. Insert multiple songs (for initializing data)
    @Insert
    void insertAll(List<Song> songs);

    // 3. Get ALL songs
    @Query("SELECT * FROM songs")
    List<Song> getAllSongs();

    // 4. Get songs by specific Album (for Album Detail Page)
    @Query("SELECT * FROM songs WHERE album = :albumName")
    List<Song> getSongsByAlbum(String albumName);

    // 5. Get all unique Albums (For Home Page)
    // We group by album so we don't see duplicate album cards
    @Query("SELECT * FROM songs GROUP BY album")
    List<Song> getUniqueAlbums();

    // 6. Search functionality
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    List<Song> searchSongs(String query);

    // 7. Favorites
    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    List<Song> getFavorites();

    @Query("SELECT COUNT(*) FROM songs WHERE title = :title AND artist = :artist")
    int exists(String title, String artist);

    @Query("DELETE FROM songs")
    void deleteAll();

    @Update
    void updateSong(Song song);

    @Delete
    void deleteSong(Song song);
}