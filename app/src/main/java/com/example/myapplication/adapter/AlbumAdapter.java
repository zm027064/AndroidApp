package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private List<Album> albums;
    private Context context;
    private OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
        void onAlbumLongClick(Album album);
    }

    public AlbumAdapter(Context context, List<Album> albums, OnAlbumClickListener listener) {
        this.context = context;
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.albumName.setText(album.getName());
        holder.photoCount.setText(album.getPhotoCount() + " photos");

        // Choose a random photo as the album thumbnail when photos exist
        if (album.getPhotoCount() > 0) {
            try {
                int idx = new java.util.Random().nextInt(album.getPhotoCount());
                Photo p = album.getPhotos().get(idx);
                String path = p.getImagePath();
                if (path != null) {
                    Bitmap bmp = BitmapFactory.decodeFile(path);
                    if (bmp != null) {
                        holder.thumbnail.setImageBitmap(bmp);
                    } else {
                        holder.thumbnail.setImageResource(R.drawable.ic_photo_placeholder);
                    }
                } else {
                    holder.thumbnail.setImageResource(R.drawable.ic_photo_placeholder);
                }
            } catch (Exception e) {
                holder.thumbnail.setImageResource(R.drawable.ic_photo_placeholder);
            }
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_photo_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlbumClick(album);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAlbumLongClick(album);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void updateAlbums(List<Album> newAlbums) {
        this.albums = newAlbums;
        notifyDataSetChanged();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView albumName;
        TextView photoCount;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.album_thumbnail);
            albumName = itemView.findViewById(R.id.album_name);
            photoCount = itemView.findViewById(R.id.album_photo_count);
        }
    }
}
