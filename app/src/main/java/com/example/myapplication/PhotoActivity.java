package com.example.myapplication;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.TagAdapter;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.Tag;
import com.example.myapplication.model.TagType;
import com.example.myapplication.util.DataStore;
import com.example.myapplication.util.SearchManager;

import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity implements TagAdapter.OnTagClickListener {

    private Photo photo;
    private String photoId;
    private int photoIndex;
    private List<Photo> photoList;

    private ImageView photoView;
    private TextView photoFilename;
    private RecyclerView tagsList;
    private TagAdapter tagAdapter;
    private TextView noTagsMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        photoId = getIntent().getStringExtra("photoId");
        photo = DataStore.findPhotoById(photoId);

        if (photo == null) {
            finish();
            return;
        }

        photoList = getPhotosFromSameAlbum(photoId);
        photoIndex = findPhotoIndexById(photoList, photoId);

        photoView = findViewById(R.id.photo_view);
        photoFilename = findViewById(R.id.photo_filename);
        noTagsMessage = findViewById(R.id.no_tags_message);
        tagsList = findViewById(R.id.tags_list);

        tagAdapter = new TagAdapter(this, photo.getTags(), this);
        tagsList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tagsList.setAdapter(tagAdapter);

        updatePhotoUI();

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.prev_button).setOnClickListener(v -> previousPhoto());
        findViewById(R.id.next_button).setOnClickListener(v -> nextPhoto());
        findViewById(R.id.add_tag_button).setOnClickListener(v -> showAddTagDialog());
        findViewById(R.id.delete_tag_button).setOnClickListener(v -> showDeleteTagDialog(null));
        findViewById(R.id.rename_button).setOnClickListener(v -> showRenameDialog());
    }

    private void updatePhotoUI() {
        if (photo == null) return;

        photoView.setImageBitmap(BitmapFactory.decodeFile(photo.getImagePath()));
        photoFilename.setText(photo.getFilename());
        tagAdapter.updateTags(photo.getTags());
        noTagsMessage.setVisibility(photo.getTags().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void previousPhoto() {
        if (photoIndex > 0) {
            photoIndex--;
            photo = photoList.get(photoIndex);
            photoId = photo.getId();
            updatePhotoUI();
        } else {
            Toast.makeText(this, "First photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void nextPhoto() {
        if (photoIndex < photoList.size() - 1) {
            photoIndex++;
            photo = photoList.get(photoIndex);
            photoId = photo.getId();
            updatePhotoUI();
        } else {
            Toast.makeText(this, "Last photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddTagDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null);
        Spinner tagTypeSpinner = view.findViewById(R.id.tag_type_spinner);
        AutoCompleteTextView tagValueInput = view.findViewById(R.id.tag_value_input);
        Button okButton = view.findViewById(R.id.dialog_ok_button);
        Button cancelButton = view.findViewById(R.id.dialog_cancel_button);

        tagTypeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Person", "Location"}));

        List<String> suggestions = SearchManager.getTagValueSuggestions(DataStore.getAlbums(this), TagType.PERSON);
        tagValueInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestions));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Tag")
                .setView(view)
                .create();

        okButton.setOnClickListener(v -> {
            String tagTypeStr = tagTypeSpinner.getSelectedItem().toString();
            String tagValue = tagValueInput.getText().toString().trim();
            if (!tagValue.isEmpty()) {
                Tag newTag = new Tag(TagType.fromString(tagTypeStr), tagValue);
                if (DataStore.addTag(this, photoId, newTag)) {
                    photo = DataStore.findPhotoById(photoId);
                    updatePhotoUI();
                    setResult(RESULT_OK);
                    Toast.makeText(this, "Tag added", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteTagDialog(Tag preselect) {
        List<Tag> tags = new ArrayList<>(photo.getTags());
        if (tags.isEmpty()) {
            Toast.makeText(this, "No tags to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = new CharSequence[tags.size()];
        boolean[] checked = new boolean[tags.size()];
        ArrayList<Tag> tagsToDelete = new ArrayList<>();

        for (int i = 0; i < tags.size(); i++) {
            items[i] = tags.get(i).toString();
            if (preselect != null && tags.get(i).equals(preselect)) {
                checked[i] = true;
                tagsToDelete.add(preselect);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select tags to delete")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    Tag selected = tags.get(which);
                    if (isChecked) tagsToDelete.add(selected);
                    else tagsToDelete.remove(selected);
                })
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (tagsToDelete.isEmpty()) {
                        Toast.makeText(this, "No tags selected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (Tag t : tagsToDelete) {
                        DataStore.removeTag(this, photoId, t);
                    }
                    photo = DataStore.findPhotoById(photoId);
                    updatePhotoUI();
                    setResult(RESULT_OK);
                    Toast.makeText(this, tagsToDelete.size() + " tag(s) deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameDialog() {
        EditText input = new EditText(this);
        input.setText(photo.getFilename());
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(this)
                .setTitle("Rename Image")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (DataStore.renamePhotoById(this, photoId, newName)) {
                            photo.setFilename(newName);
                            updatePhotoUI();
                            setResult(RESULT_OK);
                            Toast.makeText(this, "Image renamed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<Photo> getPhotosFromSameAlbum(String photoId) {
        for (com.example.myapplication.model.Album a : DataStore.getAlbums(this)) {
            for (Photo p : a.getPhotos()) {
                if (p.getId().equals(photoId)) {
                    return a.getPhotos();
                }
            }
        }
        return new ArrayList<>();
    }

    private int findPhotoIndexById(List<Photo> photos, String photoId) {
        for (int i = 0; i < photos.size(); i++) {
            if (photos.get(i).getId().equals(photoId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onTagClick(Tag tag) {}

    @Override
    public void onTagDelete(Tag tag) {
        showDeleteTagDialog(tag);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}
