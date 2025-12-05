
package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Photo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<Photo> photos;
    private Context context;
    private OnPhotoClickListener listener;
    private Set<Integer> selectedPositions = new HashSet<>();
    private boolean isSelectionMode = false;

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
        void onPhotoLongClick(Photo photo);
    }

    public PhotoAdapter(Context context, List<Photo> photos, OnPhotoClickListener listener) {
        this.context = context;
        this.photos = photos;
        this.listener = listener;
    }

    public PhotoAdapter(Context context, List<Photo> photos) {
        this.context = context;
        this.photos = photos;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);
        
        // Load and display thumbnail with error handling
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photo.getImagePath());
            if (bitmap != null) {
                holder.thumbnail.setImageBitmap(bitmap);
            } else {
                // Set placeholder if bitmap is null
                holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Exception e) {
            // Set placeholder on error
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.filename.setText(photo.getFilename());
        holder.checkbox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkbox.setChecked(selectedPositions.contains(position));

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(position);
                holder.checkbox.setChecked(selectedPositions.contains(position));
            } else if (listener != null) {
                listener.onPhotoClick(photo);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onPhotoLongClick(photo);
                setSelectionMode(true);
                toggleSelection(position);
                holder.checkbox.setChecked(true);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public void setSelectionMode(boolean enabled) {
        if (!enabled) {
            selectedPositions.clear();
        }
        isSelectionMode = enabled;
        notifyDataSetChanged();
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
    }

    public Set<Integer> getSelectedPositions() {
        return new HashSet<>(selectedPositions);
    }

    public List<Photo> getSelectedPhotos() {
        List<Photo> selected = new java.util.ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos < photos.size()) {
                selected.add(photos.get(pos));
            }
        }
        return selected;
    }

    public void clearSelection() {
        selectedPositions.clear();
        setSelectionMode(false);
    }

    public void updatePhotos(List<Photo> newPhotos) {
        this.photos = newPhotos;
        clearSelection();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView filename;
        CheckBox checkbox;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.photo_thumbnail);
            filename = itemView.findViewById(R.id.photo_filename);
            checkbox = itemView.findViewById(R.id.photo_checkbox);
        }
    }
}
