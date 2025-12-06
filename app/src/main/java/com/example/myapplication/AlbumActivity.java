package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.PhotoAdapter;
import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.util.DataStore;

import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private static final int PICK_IMAGE = 1;
    private static final int PHOTO_REQUEST_CODE = 2;
    private Album album;
    private List<Album> allAlbums;
    private RecyclerView photoGrid;
    private PhotoAdapter photoAdapter;
    private TextView albumTitle;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        album = (Album) getIntent().getSerializableExtra("album");
        if (album != null) {
            album = DataStore.findAlbumByName(album.getName());
        }

        if (album == null) {
            finish();
            return;
        }

        allAlbums = DataStore.getAlbums(this);

        albumTitle = findViewById(R.id.album_name_title);
        albumTitle.setText(album.getName());

        emptyMessage = findViewById(R.id.empty_photos_message);
        photoGrid = findViewById(R.id.photo_grid);
        photoAdapter = new PhotoAdapter(this, album.getPhotos(), this);
        photoGrid.setLayoutManager(new GridLayoutManager(this, 3));
        photoGrid.setAdapter(photoAdapter);

        updateEmptyState();

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.album_options_menu).setOnClickListener(v -> showAlbumOptions());
        findViewById(R.id.add_photo_button).setOnClickListener(v -> addPhoto());
        findViewById(R.id.remove_photo_button).setOnClickListener(v -> removePhoto());
        findViewById(R.id.move_photo_button).setOnClickListener(v -> movePhoto());
    }

    @Override
    protected void onResume() {
        super.onResume();
        allAlbums = DataStore.getAlbums(this);
        if (album != null) {
            album = DataStore.findAlbumByName(album.getName());
            if (album != null) {
                albumTitle.setText(album.getName());
                photoAdapter.updatePhotos(album.getPhotos());
                updateEmptyState();
            } else {
                finish();
            }
        }
    }

    private void addPhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PICK_IMAGE);
    }

    private void showAlbumOptions() {
        String[] options = {"Rename Album", "Delete Album", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Album Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) renameAlbum();
                    else if (which == 1) deleteAlbum();
                })
                .show();
    }

    private void renameAlbum() {
        if (album == null) return;
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(album.getName());
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(this)
                .setTitle("Rename Album")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (DataStore.renameAlbum(this, album.getName(), newName)) {
                            album = DataStore.findAlbumByName(newName);
                            albumTitle.setText(newName);
                            setResult(RESULT_OK);
                            Toast.makeText(this, "Album renamed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Rename failed: album with that name already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAlbum() {
        if (album == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Album?")
                .setMessage("Delete '" + album.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DataStore.deleteAlbum(this, album.getName());
                    Toast.makeText(this, "Album deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removePhoto() {
        if (album == null || album.getPhotos().isEmpty()) {
            Toast.makeText(this, "No photos to remove", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Photo> photos = album.getPhotos();
        CharSequence[] items = new CharSequence[photos.size()];
        for (int i = 0; i < photos.size(); i++) {
            items[i] = photos.get(i).getFilename();
        }

        ArrayList<Photo> photosToRemove = new ArrayList<>();

        new AlertDialog.Builder(this)
                .setTitle("Select photos to remove")
                .setMultiChoiceItems(items, null, (dialog, which, isChecked) -> {
                    Photo selected = photos.get(which);
                    if (isChecked) {
                        photosToRemove.add(selected);
                    } else {
                        photosToRemove.remove(selected);
                    }
                })
                .setPositiveButton("Remove", (dialog, which) -> {
                    if (photosToRemove.isEmpty()) {
                        Toast.makeText(this, "No photos selected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (Photo p : photosToRemove) {
                        DataStore.removePhotoById(this, album.getName(), p.getId());
                    }
                    album = DataStore.findAlbumByName(album.getName());
                    photoAdapter.updatePhotos(album.getPhotos());
                    updateEmptyState();
                    setResult(RESULT_OK);
                    Toast.makeText(this, photosToRemove.size() + " photo(s) removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void movePhoto() {
        if (album == null || album.getPhotos().isEmpty()) {
            Toast.makeText(this, "No photos to move", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Photo> photos = album.getPhotos();
        CharSequence[] items = new CharSequence[photos.size()];
        for (int i = 0; i < photos.size(); i++) {
            items[i] = photos.get(i).getFilename();
        }

        ArrayList<Photo> photosToMove = new ArrayList<>();

        new AlertDialog.Builder(this)
                .setTitle("Select photos to move")
                .setMultiChoiceItems(items, null, (dialog, which, isChecked) -> {
                    Photo selected = photos.get(which);
                    if (isChecked) {
                        photosToMove.add(selected);
                    } else {
                        photosToMove.remove(selected);
                    }
                })
                .setPositiveButton("Next", (dialog, which) -> {
                    if (photosToMove.isEmpty()) {
                        Toast.makeText(this, "No photos selected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showAlbumSelectionDialog(photosToMove);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAlbumSelectionDialog(ArrayList<Photo> photosToMove) {
        allAlbums = DataStore.getAlbums(this);
        List<Album> otherAlbums = new ArrayList<>();
        for (Album a : allAlbums) {
            if (!a.getName().equals(album.getName())) {
                otherAlbums.add(a);
            }
        }

        if (otherAlbums.isEmpty()) {
            Toast.makeText(this, "Create another album to move photos", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] albumNames = new String[otherAlbums.size()];
        for (int i = 0; i < otherAlbums.size(); i++) {
            albumNames[i] = otherAlbums.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Move to Album")
                .setItems(albumNames, (dialog, which) -> {
                    Album targetAlbum = otherAlbums.get(which);
                    int movedCount = 0;
                    for (Photo p : photosToMove) {
                        if (DataStore.movePhotoById(this, album.getName(), targetAlbum.getName(), p.getId())) {
                            movedCount++;
                        }
                    }
                    album = DataStore.findAlbumByName(album.getName());
                    photoAdapter.updatePhotos(album.getPhotos());
                    updateEmptyState();
                    setResult(RESULT_OK);
                    Toast.makeText(this, movedCount + " photo(s) moved to " + targetAlbum.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            int addedCount = 0;
            if (data.getClipData() != null) { // Multiple images
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    if (DataStore.addPhoto(this, album.getName(), imageUri) != null) {
                        addedCount++;
                    }
                }
            } else if (data.getData() != null) { // Single image
                Uri imageUri = data.getData();
                if (DataStore.addPhoto(this, album.getName(), imageUri) != null) {
                    addedCount++;
                }
            }

            if (addedCount > 0) {
                album = DataStore.findAlbumByName(album.getName());
                photoAdapter.updatePhotos(album.getPhotos());
                updateEmptyState();
                setResult(RESULT_OK);
                Toast.makeText(this, addedCount + " photo(s) added", Toast.LENGTH_SHORT).show();
            } else {
                 Toast.makeText(this, "Could not add photo(s)", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            album = DataStore.findAlbumByName(album.getName());
            if (album != null) {
                photoAdapter.updatePhotos(album.getPhotos());
                updateEmptyState();
            }
        }
    }

    private void updateEmptyState() {
        if (album == null || album.getPhotos().isEmpty()) {
            photoGrid.setVisibility(RecyclerView.GONE);
            emptyMessage.setVisibility(TextView.VISIBLE);
        } else {
            photoGrid.setVisibility(RecyclerView.VISIBLE);
            emptyMessage.setVisibility(TextView.GONE);
        }
    }

    @Override
    public void onPhotoClick(Photo photo) {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("photoId", photo.getId());
        startActivityForResult(intent, PHOTO_REQUEST_CODE);
    }

    @Override
    public void onPhotoLongClick(Photo photo) {
        Toast.makeText(this, "Long-clicked on " + photo.getFilename(), Toast.LENGTH_SHORT).show();
    }
}
