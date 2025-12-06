
package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
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
    private static final String TAG = "AlbumActivity";
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
        allAlbums = DataStore.getAlbums(this);
        if (album != null && allAlbums != null) {
            album = DataStore.findAlbumByName(album.getName());
        }
        
        if (album == null) {
            finish();
            return;
        }

        albumTitle = findViewById(R.id.album_name_title);
        if (album != null) {
            albumTitle.setText(album.getName());
        }

        emptyMessage = findViewById(R.id.empty_photos_message);
        photoGrid = findViewById(R.id.photo_grid);
        photoAdapter = new PhotoAdapter(this, album.getPhotos(), this);
        photoGrid.setLayoutManager(new GridLayoutManager(this, 3));
        photoGrid.setAdapter(photoAdapter);

        updateEmptyState();

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        Button albumOptionsMenu = findViewById(R.id.album_options_menu);
        albumOptionsMenu.setOnClickListener(v -> showAlbumOptions());

        Button addPhotoButton = findViewById(R.id.add_photo_button);
        addPhotoButton.setOnClickListener(v -> addPhoto());

        Button removePhotoButton = findViewById(R.id.remove_photo_button);
        removePhotoButton.setOnClickListener(v -> removePhoto());

        Button movePhotoButton = findViewById(R.id.move_photo_button);
        movePhotoButton.setOnClickListener(v -> movePhoto());
    }

    private void addPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void showAlbumOptions() {
        String[] options = {"Rename Album", "Delete Album", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Album Options");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                renameAlbum();
            } else if (which == 1) {
                deleteAlbum();
            }
        });
        builder.show();
    }

    private void renameAlbum() {
        if (album == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Album");
        
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(album.getName());
        input.setSelectAllOnFocus(true);
        builder.setView(input);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && album != null) {
                DataStore.renameAlbum(AlbumActivity.this, album.getName(), newName);
                allAlbums = DataStore.getAlbums(AlbumActivity.this);
                album = DataStore.findAlbumByName(newName);
                albumTitle.setText(newName);
                Toast.makeText(AlbumActivity.this, "Album renamed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteAlbum() {
        if (album == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Album?");
        builder.setMessage("Delete '" + album.getName() + "'?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            if (album != null) DataStore.deleteAlbum(AlbumActivity.this, album.getName());
            Toast.makeText(AlbumActivity.this, "Album deleted", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void removePhoto() {
        List<Photo> photos = album.getPhotos();
        if (photos == null || photos.isEmpty()) {
            Toast.makeText(this, "No photos to remove", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] items = new CharSequence[photos.size()];
        final boolean[] checked = new boolean[photos.size()];
        for (int i = 0; i < photos.size(); i++) {
            Photo p = photos.get(i);
            items[i] = p.getFilename() != null ? p.getFilename() : p.getImagePath();
            checked[i] = false;
        }

        final java.util.ArrayList<Integer> selected = new java.util.ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select photos to remove");
        builder.setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!selected.contains(which)) selected.add(which);
            } else {
                selected.remove(Integer.valueOf(which));
            }
        });

        builder.setPositiveButton("Remove", (dialog, which) -> {
            if (selected.isEmpty()) {
                Toast.makeText(AlbumActivity.this, "No photos selected", Toast.LENGTH_SHORT).show();
                return;
            }
            for (int idx : selected) {
                if (idx >= 0 && idx < photos.size()) {
                    Photo photo = photos.get(idx);
                    DataStore.removePhoto(AlbumActivity.this, album.getName(), photo.getImagePath(), photo.getFilename());
                }
            }
            allAlbums = DataStore.getAlbums(AlbumActivity.this);
            album = DataStore.findAlbumByName(album.getName());
            photoAdapter.clearSelection();
            photoAdapter.updatePhotos(album.getPhotos());
            updateEmptyState();
            Toast.makeText(AlbumActivity.this, selected.size() + " photo(s) removed", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void movePhoto() {
        List<Photo> photos = album.getPhotos();
        if (photos == null || photos.isEmpty()) {
            Toast.makeText(this, "No photos to move", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] items = new CharSequence[photos.size()];
        final boolean[] checked = new boolean[photos.size()];
        for (int i = 0; i < photos.size(); i++) {
            Photo p = photos.get(i);
            items[i] = p.getFilename() != null ? p.getFilename() : p.getImagePath();
            checked[i] = false;
        }

        final java.util.ArrayList<Integer> selected = new java.util.ArrayList<>();

        AlertDialog.Builder pickBuilder = new AlertDialog.Builder(this);
        pickBuilder.setTitle("Select photos to move");
        pickBuilder.setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!selected.contains(which)) selected.add(which);
            } else {
                selected.remove(Integer.valueOf(which));
            }
        });

        pickBuilder.setPositiveButton("Next", (dialog, which) -> {
            if (selected.isEmpty()) {
                Toast.makeText(AlbumActivity.this, "No photos selected", Toast.LENGTH_SHORT).show();
                return;
            }
            // Create list of other albums (exclude current album)
            List<Album> otherAlbums = new ArrayList<>();
            for (Album a : allAlbums) {
                if (!a.equals(album)) {
                    otherAlbums.add(a);
                }
            }

            if (otherAlbums.isEmpty()) {
                Toast.makeText(AlbumActivity.this, "Create another album to move photos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show dialog to select target album
            String[] albumNames = new String[otherAlbums.size()];
            for (int i = 0; i < otherAlbums.size(); i++) {
                albumNames[i] = otherAlbums.get(i).getName();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(AlbumActivity.this);
            builder.setTitle("Move to Album");
            builder.setItems(albumNames, (d2, whichAlbum) -> {
                Album targetAlbum = otherAlbums.get(whichAlbum);
                int moved = 0;
                for (int idx : selected) {
                    if (idx >= 0 && idx < photos.size()) {
                        Photo photo = photos.get(idx);
                        boolean ok = DataStore.movePhoto(AlbumActivity.this, album.getName(), targetAlbum.getName(), photo.getImagePath(), photo.getFilename());
                        if (ok) moved++;
                    }
                }
                allAlbums = DataStore.getAlbums(AlbumActivity.this);
                album = DataStore.findAlbumByName(album.getName());
                // Update UI
                photoAdapter.clearSelection();
                photoAdapter.updatePhotos(album.getPhotos());
                updateEmptyState();
                setResult(RESULT_OK);
                Toast.makeText(AlbumActivity.this, moved + " photo(s) moved to " + targetAlbum.getName(), Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        pickBuilder.setNegativeButton("Cancel", null);
        pickBuilder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                String picturePath = null;
                // Try to resolve file path via cursor (may not work on modern devices)
                try {
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            if (columnIndex != -1) {
                                picturePath = cursor.getString(columnIndex);
                            }
                        }
                        cursor.close();
                    }
                } catch (Exception ignored) {}

                // If we couldn't get a direct file path, copy the content URI into app storage
                if (picturePath == null) {
                    picturePath = saveImageFromUri(selectedImage);
                }

                if (picturePath != null) {
                    Photo newPhoto = DataStore.addPhoto(AlbumActivity.this, album.getName(), picturePath, null);
                    Log.d(TAG, "Adding photo: " + picturePath);
                    allAlbums = DataStore.getAlbums(AlbumActivity.this);
                    album = DataStore.findAlbumByName(album.getName());
                    photoAdapter.updatePhotos(album.getPhotos());
                    updateEmptyState();
                    Toast.makeText(this, "Photo added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Unable to add photo", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            // If PhotoActivity returned updated data, merge it into our allAlbums list
            if (data != null) {
                // Child PhotoActivity may have changed tags/photos. Refresh from DataStore.
                allAlbums = DataStore.getAlbums(AlbumActivity.this);
                album = DataStore.findAlbumByName(album.getName());
                photoAdapter.updatePhotos(album.getPhotos());
                updateEmptyState();
            }
            photoAdapter.clearSelection();
        }
    }

    /**
     * Copy image content from URI into app private storage and return the saved absolute path.
     */
    private String saveImageFromUri(Uri imageUri) {
        try {
            String imagesDirName = "images";
            java.io.File imagesDir = new java.io.File(getFilesDir(), imagesDirName);
            if (!imagesDir.exists()) imagesDir.mkdirs();

            String fileName = java.util.UUID.randomUUID().toString() + ".jpg";
            java.io.File outFile = new java.io.File(imagesDir, fileName);

            java.io.InputStream in = getContentResolver().openInputStream(imageUri);
            if (in == null) {
                Log.e(TAG, "Could not open input stream for URI: " + imageUri);
                return null;
            }
            java.io.OutputStream out = new java.io.FileOutputStream(outFile);

            byte[] buf = new byte[8192];
            int len;
            long totalBytes = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                totalBytes += len;
            }
            out.close();
            in.close();

            Log.d(TAG, "Saved image to: " + outFile.getAbsolutePath() + " (" + totalBytes + " bytes)");
            if (outFile.exists()) {
                Log.d(TAG, "File exists and size is: " + outFile.length() + " bytes");
            } else {
                Log.e(TAG, "File was not created!");
                return null;
            }
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error saving image from URI", e);
            e.printStackTrace();
            return null;
        }
    }

    private void updateEmptyState() {
        if (album.getPhotos().isEmpty()) {
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
        intent.putExtra("album", album);
        // Pass full albums list so PhotoActivity can provide suggestions across all albums
        intent.putExtra("allAlbums", new java.util.ArrayList<>(allAlbums));
        intent.putExtra("photo", photo);
        intent.putExtra("photoIndex", album.getPhotos().indexOf(photo));
        startActivityForResult(intent, PHOTO_REQUEST_CODE);
    }

    @Override
    public void onPhotoLongClick(Photo photo) {
        Toast.makeText(this, "Hold to select multiple photos for deletion", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (album != null) {
            Intent intent = new Intent();
            intent.putExtra("album", album);
            setResult(RESULT_OK, intent);
        }
        // DataStore persists changes immediately; nothing to do here.
    }
}
