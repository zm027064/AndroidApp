package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {

    private List<Tag> tags;
    private Context context;
    private OnTagClickListener listener;
    private Set<Integer> selectedPositions = new HashSet<>();
    private boolean isSelectionMode = false;
    private boolean showCheckbox = false;

    public interface OnTagClickListener {
        void onTagClick(Tag tag);
        void onTagDelete(Tag tag);
    }

    public TagAdapter(Context context, List<Tag> tags) {
        this.context = context;
        this.tags = tags;
    }

    public TagAdapter(Context context, List<Tag> tags, OnTagClickListener listener) {
        this.context = context;
        this.tags = tags;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        Tag tag = tags.get(position);
        holder.tagText.setText(tag.toString());
        
        if (showCheckbox) {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(selectedPositions.contains(position));
            holder.deleteButton.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                toggleSelection(position);
                holder.checkbox.setChecked(selectedPositions.contains(position));
            });
        } else {
            holder.checkbox.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.VISIBLE);

            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTagDelete(tag);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTagClick(tag);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public void setSelectionMode(boolean enabled, boolean showCheckbox) {
        if (!enabled) {
            selectedPositions.clear();
        }
        this.isSelectionMode = enabled;
        this.showCheckbox = showCheckbox;
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

    public void setSelectedPositions(Set<Integer> positions) {
        selectedPositions.clear();
        if (positions != null) selectedPositions.addAll(positions);
        notifyDataSetChanged();
    }

    public List<Tag> getSelectedTags() {
        List<Tag> selected = new java.util.ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos < tags.size()) {
                selected.add(tags.get(pos));
            }
        }
        return selected;
    }

    public void clearSelection() {
        selectedPositions.clear();
        setSelectionMode(false, false);
    }

    public void updateTags(List<Tag> newTags) {
        this.tags = newTags;
        clearSelection();
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tagText;
        CheckBox checkbox;
        android.widget.ImageButton deleteButton;

        TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagText = itemView.findViewById(R.id.tag_text);
            checkbox = itemView.findViewById(R.id.tag_checkbox);
            deleteButton = itemView.findViewById(R.id.tag_delete_button);
        }
    }
}
