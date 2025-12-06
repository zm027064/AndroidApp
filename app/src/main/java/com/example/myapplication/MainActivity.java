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

        // Load albums from centralized DataStore
        albums = DataStore.getAlbums(this);
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
