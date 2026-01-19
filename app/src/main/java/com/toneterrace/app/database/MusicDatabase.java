package com.toneterrace.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.toneterrace.app.models.Song;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Song.class}, version = 1)
public abstract class MusicDatabase extends RoomDatabase {

    public abstract SongDao songDao();

    // Singleton Pattern: Ensures only one instance of the DB exists
    private static volatile MusicDatabase INSTANCE;

    // Executor for running DB tasks in background (Database operations cannot run on Main Thread!)
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public static MusicDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MusicDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MusicDatabase.class, "toneterrace_database")
                            .allowMainThreadQueries() // WARNING: For student project simplicity ONLY.
                            // In a real job, never allow main thread queries (it freezes UI).
                            // We use it here to avoid complex callback logic for now.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}