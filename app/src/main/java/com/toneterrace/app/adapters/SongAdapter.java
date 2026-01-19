package com.toneterrace.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.toneterrace.app.R;
import com.toneterrace.app.models.Song;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private Context context;
    private List<Song> songList;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SongAdapter(Context context, List<Song> songList, OnSongClickListener listener) {
        this.context = context;
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);

        holder.tvSongNumber.setText(String.valueOf(position + 1));
        holder.tvSongTitle.setText(song.title);
        holder.tvSongArtist.setText(song.artist);
        holder.tvDuration.setText(song.duration);

        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongNumber, tvSongTitle, tvSongArtist, tvDuration;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongNumber = itemView.findViewById(R.id.tvSongNumber);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}