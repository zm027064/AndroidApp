package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;

import com.example.myapplication.adapter.AlbumAdapter;
import com.example.myapplication.model.Album;
import com.example.myapplication.util.DataStore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AlbumAdapter.OnAlbumClickListener {

    private static final int ALBUM_REQUEST_CODE = 1;
    private List<Album> albums;
    private RecyclerView albumRecyclerView;
    private AlbumAdapter albumAdapter;
    private Album selectedAlbum = null;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request runtime permissions for storage access
        requestStoragePermissions();

        // One-time initialization: create `stock` album from bundled assets (if present)
        ensureInitialStock();

        // Load albums from centralized DataStore
        albums = DataStore.getAlbums(this);
        // Remove any preloaded 'New York' location tags once (migration/cleanup)
        removePreloadedNewYorkTags();
        // Ensure the New York location tag is present for matching stock images (idempotent)
        ensureNewYorkTagsPresent();
        if (albums == null) albums = new ArrayList<>();

        // Setup RecyclerView
        albumRecyclerView = findViewById(R.id.album_list);
        emptyMessage = findViewById(R.id.empty_albums_message);
        albumAdapter = new AlbumAdapter(this, albums, this);
        albumRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        albumRecyclerView.setAdapter(albumAdapter);

        updateEmptyState();

        // Setup buttons
        Button createButton = findViewById(R.id.create_album_button);
        createButton.setOnClickListener(v -> createAlbum());

        Button searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> openSearch());
    }

    private void ensureInitialStock() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean initialized = prefs.getBoolean("initialized", false);
        // If we've already initialized once, never run the import again.
        if (initialized) return;

        // Create the stock album and populate from assets/stock if available
        try {
            String stockAlbumName = "stock";
            boolean created = com.example.myapplication.util.DataStore.createAlbum(this, stockAlbumName);

            String[] assetFiles = null;
            try {
                assetFiles = getAssets().list("stock");
            } catch (Exception ignored) {}

            java.io.File imagesDir = new java.io.File(getFilesDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();

            if (assetFiles != null && assetFiles.length > 0) {
                for (String fname : assetFiles) {
                    try (java.io.InputStream in = getAssets().open("stock/" + fname)) {
                        java.io.File out = new java.io.File(imagesDir, fname);
                        try (java.io.OutputStream os = new java.io.FileOutputStream(out)) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
                            os.flush();
                        }

                                String savedPath = out.getAbsolutePath();
                                com.example.myapplication.model.Photo p = com.example.myapplication.util.DataStore.addPhoto(this, stockAlbumName, savedPath, fname);
                    } catch (Exception ex) {
                        android.util.Log.w("MainActivity", "Failed to copy asset: " + fname, ex);
                    }
                }
            }

            // Also detect any drawables or raw resources named starting with 'stock' and copy them
            try {
                String pkg = getPackageName();

                // Try to load drawables via reflection at runtime to avoid compile-time dependency
                try {
                    Class<?> dcls = Class.forName(pkg + ".R$drawable");
                    java.lang.reflect.Field[] fields = dcls.getFields();
                    for (java.lang.reflect.Field f : fields) {
                        String name = f.getName();
                        if (name != null && name.startsWith("stock")) {
                            try {
                                int resId = f.getInt(null);
                                try (java.io.InputStream din = getResources().openRawResource(resId)) {
                                    java.io.File out = new java.io.File(imagesDir, name + ".png");
                                    try (java.io.OutputStream os = new java.io.FileOutputStream(out)) {
                                        byte[] buf = new byte[8192];
                                        int r;
                                        while ((r = din.read(buf)) != -1) os.write(buf, 0, r);
                                        os.flush();
                                    }
                                    String savedPath = out.getAbsolutePath();
                                    com.example.myapplication.util.DataStore.addPhoto(this, stockAlbumName, savedPath, out.getName());
                                }
                            } catch (Exception ex) {
                                android.util.Log.w("MainActivity", "Failed to extract drawable: " + name, ex);
                            }
                        }
                    }
                } catch (ClassNotFoundException cnf) {
                    android.util.Log.d("MainActivity", "No R.drawable class found to scan for stock images");
                }

                // Try raw resources similarly
                try {
                    Class<?> rcls = Class.forName(pkg + ".R$raw");
                    java.lang.reflect.Field[] rawFields = rcls.getFields();
                    for (java.lang.reflect.Field f : rawFields) {
                        String name = f.getName();
                        if (name != null && name.startsWith("stock")) {
                            try {
                                int resId = f.getInt(null);
                                try (java.io.InputStream rin = getResources().openRawResource(resId)) {
                                    java.io.File out = new java.io.File(imagesDir, name + ".dat");
                                    try (java.io.OutputStream os = new java.io.FileOutputStream(out)) {
                                        byte[] buf = new byte[8192];
                                        int r;
                                        while ((r = rin.read(buf)) != -1) os.write(buf, 0, r);
                                        os.flush();
                                    }
                                    String savedPath = out.getAbsolutePath();
                                    com.example.myapplication.util.DataStore.addPhoto(this, stockAlbumName, savedPath, out.getName());
                                }
                            } catch (Exception ex) {
                                android.util.Log.w("MainActivity", "Failed to extract raw resource: " + name, ex);
                            }
                        }
                    }
                } catch (ClassNotFoundException cnf) {
                    android.util.Log.d("MainActivity", "No R.raw class found to scan for stock images");
                }
            } catch (Exception e) {
                android.util.Log.w("MainActivity", "Error scanning resources for stock images", e);
            }

        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error during initial stock setup", e);
        }

        // Mark initialization completed so this runs only once.
        prefs.edit().putBoolean("initialized", true).apply();
    }

    private void removePreloadedNewYorkTags() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean cleaned = prefs.getBoolean("removed_ny_tags", false);
        if (cleaned) return;

        try {
            List<com.example.myapplication.model.Album> all = com.example.myapplication.util.DataStore.getAlbums(this);
            if (all == null) return;
            for (com.example.myapplication.model.Album a : all) {
                if (a == null) continue;
                for (com.example.myapplication.model.Photo p : new java.util.ArrayList<>(a.getPhotos())) {
                    if (p == null) continue;
                    for (com.example.myapplication.model.Tag t : new java.util.ArrayList<>(p.getTags())) {
                        if (t == null) continue;
                        if (t.getTagType() == com.example.myapplication.model.TagType.LOCATION &&
                                t.getTagValue() != null && t.getTagValue().equalsIgnoreCase("New York")) {
                            com.example.myapplication.util.DataStore.removeTag(this, a.getName(), p.getImagePath(), p.getFilename(), t);
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.w("MainActivity", "Error removing preloaded New York tags", e);
        }

        prefs.edit().putBoolean("removed_ny_tags", true).apply();
    }

    private void ensureNewYorkTagsPresent() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean added = prefs.getBoolean("added_ny_tags", false);
        if (added) return;

        try {
            java.util.List<com.example.myapplication.model.Album> all = com.example.myapplication.util.DataStore.getAlbums(this);
            if (all == null) return;
            for (com.example.myapplication.model.Album a : all) {
                if (a == null) continue;
                for (com.example.myapplication.model.Photo p : new java.util.ArrayList<>(a.getPhotos())) {
                    if (p == null) continue;
                    String fname = p.getFilename() != null ? p.getFilename().toLowerCase() : "";
                    String path = p.getImagePath() != null ? p.getImagePath().toLowerCase() : "";
                    // Heuristics for New York-related filenames
                    boolean matches = fname.contains("newyork") || fname.contains("new_york") || fname.contains("new-york")
                            || fname.contains("nyc") || fname.contains("manhattan") || fname.contains("ny_") || fname.contains("_ny")
                            || path.contains("newyork") || path.contains("new_york") || path.contains("nyc") || path.contains("manhattan") || path.contains("/ny/");

                    if (!matches) continue;

                    boolean has = false;
                    for (com.example.myapplication.model.Tag t : p.getTags()) {
                        if (t == null) continue;
                        if (t.getTagType() == com.example.myapplication.model.TagType.LOCATION && t.getTagValue() != null
                                && t.getTagValue().equalsIgnoreCase("New York")) {
                            has = true; break;
                        }
                    }
                    if (!has) {
                        com.example.myapplication.model.Tag ny = new com.example.myapplication.model.Tag(com.example.myapplication.model.TagType.LOCATION, "New York");
                        com.example.myapplication.util.DataStore.addTag(this, a.getName(), p.getImagePath(), p.getFilename(), ny);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.w("MainActivity", "Error ensuring New York tags", e);
        }

        prefs.edit().putBoolean("added_ny_tags", true).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh albums each time activity resumes so counts reflect external changes
        albums = DataStore.getAlbums(this);
        if (albumAdapter != null) {
            albumAdapter.updateAlbums(albums);
        }
        updateEmptyState();
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        1001);
            }
        } else {
            // For API < 33, use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1001);
            }
        }
    }

    private void createAlbum() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Album");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_album, null);
        EditText input = view.findViewById(R.id.album_name_input);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Wire up the buttons inside the inflated layout to avoid duplicate dialog buttons
        Button okBtn = view.findViewById(R.id.dialog_ok_button);
        Button cancelBtn = view.findViewById(R.id.dialog_cancel_button);

        okBtn.setOnClickListener(v -> {
            String albumName = input.getText().toString().trim();
                if (!albumName.isEmpty()) {
                    if (albumExists(albumName)) {
                        Toast.makeText(MainActivity.this, "Album already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        DataStore.createAlbum(MainActivity.this, albumName);
                        albums = DataStore.getAlbums(MainActivity.this);
                        albumAdapter.updateAlbums(albums);
                        updateEmptyState();
                        dialog.dismiss();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Enter album name", Toast.LENGTH_SHORT).show();
                }
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void renameAlbum(Album album) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Album");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_album, null);
        EditText input = view.findViewById(R.id.album_name_input);
        input.setText(album.getName());
        builder.setView(view);
        AlertDialog dialogRename = builder.create();

        Button okBtnRename = view.findViewById(R.id.dialog_ok_button);
        Button cancelBtnRename = view.findViewById(R.id.dialog_cancel_button);

        okBtnRename.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                if (!newName.equals(album.getName()) && albumExists(newName)) {
                    Toast.makeText(MainActivity.this, "Album already exists", Toast.LENGTH_SHORT).show();
                } else {
                    DataStore.renameAlbum(MainActivity.this, album.getName(), newName);
                    albums = DataStore.getAlbums(MainActivity.this);
                    albumAdapter.updateAlbums(albums);
                    dialogRename.dismiss();
                }
            }
        });

        cancelBtnRename.setOnClickListener(v -> dialogRename.dismiss());

        dialogRename.show();
    }

    private void deleteAlbum(Album album) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Album?");
        builder.setMessage("Are you sure you want to delete \"" + album.getName() + "\"?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            DataStore.deleteAlbum(MainActivity.this, album.getName());
            albums = DataStore.getAlbums(MainActivity.this);
            albumAdapter.updateAlbums(albums);
            updateEmptyState();
            Toast.makeText(MainActivity.this, "Album deleted", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openSearch() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }

    private void updateEmptyState() {
        if (albums.isEmpty()) {
            albumRecyclerView.setVisibility(RecyclerView.GONE);
            emptyMessage.setVisibility(TextView.VISIBLE);
        } else {
            albumRecyclerView.setVisibility(RecyclerView.VISIBLE);
            emptyMessage.setVisibility(TextView.GONE);
        }
    }

    private boolean albumExists(String name) {
        for (Album album : albums) {
            if (album.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAlbumClick(Album album) {
        Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
        intent.putExtra("album", album);
        // Pass the full albums list so AlbumActivity can update the same instances
        intent.putExtra("allAlbums", new java.util.ArrayList<>(albums));
        startActivityForResult(intent, ALBUM_REQUEST_CODE);
    }

    @Override
    public void onAlbumLongClick(Album album) {
        String[] options = {"Rename", "Delete", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(album.getName());
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                renameAlbum(album);
            } else if (which == 1) {
                deleteAlbum(album);
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ALBUM_REQUEST_CODE && resultCode == RESULT_OK) {
            // Child activity may have modified data; refresh from DataStore
            albums = DataStore.getAlbums(this);
            albumAdapter.updateAlbums(albums);
            updateEmptyState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // DataStore persists changes immediately; no explicit save required here.
    }
}
