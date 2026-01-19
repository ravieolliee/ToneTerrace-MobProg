package com.toneterrace.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.toneterrace.app.R;
import com.toneterrace.app.models.Song;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private Context context;
    private List<Song> albumList; // We use Song objects to represent Albums
    private OnAlbumClickListener listener;

    // Interface for handling clicks
    public interface OnAlbumClickListener {
        void onAlbumClick(Song song);
    }

    // Constructor: Context first, then List, then Listener
    public AlbumAdapter(Context context, List<Song> albumList, OnAlbumClickListener listener) {
        this.context = context;
        this.albumList = albumList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout (item_album.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Song currentAlbum = albumList.get(position);

        holder.tvAlbumTitle.setText(currentAlbum.album);
        holder.tvArtistName.setText(currentAlbum.artist);

        // Use Glide to load image from URL
        // We use a placeholder if the URL is empty or fails
        if (currentAlbum.albumArtUrl != null && !currentAlbum.albumArtUrl.isEmpty()) {
            Glide.with(context)
                    .load(currentAlbum.albumArtUrl)
                    .placeholder(R.drawable.ic_launcher_background) // Show this while loading
                    .into(holder.imgAlbumArt);
        } else {
            holder.imgAlbumArt.setImageResource(R.drawable.ic_launcher_background);
        }

        // Handle Click
        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(currentAlbum));
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    // ViewHolder Class: Holds references to the UI elements in item_album.xml
    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAlbumArt;
        TextView tvAlbumTitle, tvArtistName;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAlbumArt = itemView.findViewById(R.id.imgAlbumArt);
            tvAlbumTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
        }
    }

    // Method to update data for Search
    public void updateList(List<Song> newList) {
        this.albumList = newList;
        notifyDataSetChanged(); // Refreshes the UI instantly
    }

}