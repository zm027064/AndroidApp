
package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.TagAdapter;
import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.Tag;
import com.example.myapplication.model.TagType;
import com.example.myapplication.util.SearchManager;
import com.example.myapplication.util.TagManager;

import java.util.List;

public class PhotoActivity extends AppCompatActivity implements TagAdapter.OnTagClickListener {

    private Album album;
    private java.util.List<Album> allAlbums;
    private int photoIndex;
    private Photo currentPhoto;
    private List<Photo> albumPhotos;

    private ImageView photoView;
    private TextView photoFilename;
    private RecyclerView tagsList;
    private TagAdapter tagAdapter;
    private TextView noTagsMessage;
    private boolean slideshowMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        album = (Album) getIntent().getSerializableExtra("album");
        allAlbums = com.example.myapplication.util.DataStore.getAlbums(this);

        // Replace album reference with the canonical instance from DataStore (match by name)
        if (album != null) {
            album = com.example.myapplication.util.DataStore.findAlbumByName(album.getName());
        }
        photoIndex = getIntent().getIntExtra("photoIndex", 0);

        if (album == null) {
            finish();
            return;
        }

        albumPhotos = album.getPhotos();
        currentPhoto = albumPhotos.get(photoIndex);

        photoView = findViewById(R.id.photo_view);
        photoFilename = findViewById(R.id.photo_filename);
        noTagsMessage = findViewById(R.id.no_tags_message);

        // Setup tags RecyclerView
        tagsList = findViewById(R.id.tags_list);
        tagAdapter = new TagAdapter(this, currentPhoto.getTags(), this);
        tagsList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tagsList.setAdapter(tagAdapter);

        updatePhoto();

        // Setup buttons
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        Button prevButton = findViewById(R.id.prev_button);
        prevButton.setOnClickListener(v -> previousPhoto());

        Button nextButton = findViewById(R.id.next_button);
        nextButton.setOnClickListener(v -> nextPhoto());

        Button addTagButton = findViewById(R.id.add_tag_button);
        addTagButton.setOnClickListener(v -> showAddTagDialog());

        Button deleteTagButton = findViewById(R.id.delete_tag_button);
        deleteTagButton.setOnClickListener(v -> showDeleteTagDialog());

        Button slideshowButton = findViewById(R.id.slideshow_button);
        slideshowButton.setOnClickListener(v -> toggleSlideshow());

        Button renameButton = findViewById(R.id.rename_button);
        renameButton.setOnClickListener(v -> showRenameDialog());
    }

    private void updatePhoto() {
        if (currentPhoto == null || photoIndex < 0 || photoIndex >= albumPhotos.size()) {
            return;
        }

        currentPhoto = albumPhotos.get(photoIndex);

        // Display photo with error handling
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhoto.getImagePath());
            if (bitmap != null) {
                photoView.setImageBitmap(bitmap);
            } else {
                // Set placeholder if bitmap is null
                photoView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Exception e) {
            // Set placeholder on error
            photoView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Display filename
        photoFilename.setText(currentPhoto.getFilename());

        // Update tags
        tagAdapter.updateTags(currentPhoto.getTags());
        updateTagsVisibility();
    }

    private void updateTagsVisibility() {
        if (currentPhoto.getTags().isEmpty()) {
            noTagsMessage.setVisibility(View.VISIBLE);
        } else {
            noTagsMessage.setVisibility(View.GONE);
        }
    }

    private void previousPhoto() {
        if (photoIndex > 0) {
            photoIndex--;
            updatePhoto();
        } else {
            Toast.makeText(this, "First photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void nextPhoto() {
        if (photoIndex < albumPhotos.size() - 1) {
            photoIndex++;
            updatePhoto();
        } else {
            Toast.makeText(this, "Last photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSlideshow() {
        slideshowMode = !slideshowMode;
        if (slideshowMode) {
            Toast.makeText(this, "Slideshow on - use arrows to navigate", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Slideshow off", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteTagDialog() {
        showDeleteTagDialog(null);
    }

    /**
     * Show delete dialog allowing multi-select. If preselect is non-null, the corresponding tag
     * will be pre-selected in the dialog so the user can quickly remove that one or choose others.
     */
    private void showDeleteTagDialog(Tag preselect) {
        List<Tag> tags = new java.util.ArrayList<>(currentPhoto.getTags());
        if (tags.isEmpty()) {
            Toast.makeText(this, "No tags to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] items = new CharSequence[tags.size()];
        final boolean[] checked = new boolean[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            items[i] = tags.get(i).toString();
            checked[i] = (preselect != null && tags.get(i).equals(preselect));
        }

        java.util.ArrayList<Integer> selectedIndices = new java.util.ArrayList<>();
        // Pre-populate selectedIndices with any preselected index
        for (int i = 0; i < checked.length; i++) if (checked[i]) selectedIndices.add(i);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select tags to delete");
        builder.setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!selectedIndices.contains(which)) selectedIndices.add(which);
            } else {
                selectedIndices.remove(Integer.valueOf(which));
            }
        });

        builder.setPositiveButton("Delete", (dialog, which) -> {
            if (selectedIndices.isEmpty()) {
                Toast.makeText(PhotoActivity.this, "No tags selected", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                List<Tag> toDelete = new java.util.ArrayList<>();
                for (int idx : selectedIndices) {
                    if (idx >= 0 && idx < tags.size()) toDelete.add(tags.get(idx));
                }
                for (Tag tag : toDelete) {
                    TagManager.removeTag(PhotoActivity.this, allAlbums, album, currentPhoto, tag);
                }
                // Refresh from DataStore
                allAlbums = com.example.myapplication.util.DataStore.getAlbums(PhotoActivity.this);
                album = com.example.myapplication.util.DataStore.findAlbumByName(album.getName());
                currentPhoto = album.getPhotoAt(photoIndex);
                if (currentPhoto != null) {
                    tagAdapter.updateTags(currentPhoto.getTags());
                    updateTagsVisibility();
                }
                Toast.makeText(PhotoActivity.this, toDelete.size() + " tag(s) deleted", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("PhotoActivity", "Error deleting tags", e);
                Toast.makeText(PhotoActivity.this, "Error deleting tags", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog d = builder.create();
        d.show();
    }
    private void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Tag");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null);
        Spinner tagTypeSpinner = view.findViewById(R.id.tag_type_spinner);
        AutoCompleteTextView tagValueInput = view.findViewById(R.id.tag_value_input);

        // Setup tag type spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Person", "Location"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner.setAdapter(spinnerAdapter);

        // Setup autocomplete for tag values using DataStore albums
        java.util.List<Album> suggestionSource = allAlbums != null ? allAlbums : java.util.Collections.singletonList(album);
        List<String> suggestions = SearchManager.getTagValueSuggestions(suggestionSource, TagType.PERSON);
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestions);
        tagValueInput.setAdapter(autoCompleteAdapter);
        tagValueInput.setThreshold(1);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        Button okBtn = view.findViewById(R.id.dialog_ok_button);
        Button cancelBtn = view.findViewById(R.id.dialog_cancel_button);

        okBtn.setOnClickListener(v -> {
            String tagTypeStr = tagTypeSpinner.getSelectedItem().toString();
            String tagValue = tagValueInput.getText().toString().trim();

            if (!tagValue.isEmpty()) {
                TagType tagType = TagType.fromString(tagTypeStr);
                Tag newTag = new Tag(tagType, tagValue);
                TagManager.addTag(PhotoActivity.this, allAlbums, album, currentPhoto, newTag);
                // Refresh currentPhoto from DataStore
                allAlbums = com.example.myapplication.util.DataStore.getAlbums(PhotoActivity.this);
                album = com.example.myapplication.util.DataStore.findAlbumByName(album.getName());
                currentPhoto = album.getPhotoAt(photoIndex);
                tagAdapter.updateTags(currentPhoto.getTags());
                updateTagsVisibility();
                Toast.makeText(PhotoActivity.this, "Tag added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showRenameDialog() {
        if (currentPhoto == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Image");

        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(currentPhoto.getFilename());
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(PhotoActivity.this, "Enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean ok = com.example.myapplication.util.DataStore.renamePhoto(PhotoActivity.this, album.getName(), currentPhoto.getImagePath(), currentPhoto.getFilename(), newName);
            if (ok) {
                // Refresh photo and UI
                allAlbums = com.example.myapplication.util.DataStore.getAlbums(PhotoActivity.this);
                album = com.example.myapplication.util.DataStore.findAlbumByName(album.getName());
                currentPhoto = album.getPhotoAt(photoIndex);
                if (currentPhoto != null) {
                    photoFilename.setText(currentPhoto.getFilename());
                }
                Toast.makeText(PhotoActivity.this, "Image renamed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PhotoActivity.this, "Rename failed", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onTagClick(Tag tag) {
        // Can be used for viewing tag details in future
    }

    @Override
    public void onTagDelete(Tag tag) {
        // Open the multi-select delete dialog with this tag pre-selected
        showDeleteTagDialog(tag);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent intent = new Intent();
        intent.putExtra("album", album);
        // Ensure DataStore is up-to-date (TagManager/DataStore already persisted changes)
        setResult(RESULT_OK, intent);
    }

    /**
     * Replace the album entry inside `allAlbums` (match by name) with this.activity's `album` instance.
     */
    private void syncAlbumToAllAlbums() {
        if (allAlbums == null || album == null) return;
        for (int i = 0; i < allAlbums.size(); i++) {
            if (allAlbums.get(i).getName().equals(album.getName())) {
                allAlbums.set(i, album);
                return;
            }
        }
        // If not found, add it
        allAlbums.add(album);
    }
}
