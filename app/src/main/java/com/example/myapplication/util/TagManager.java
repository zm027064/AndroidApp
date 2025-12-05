package com.example.myapplication.util;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.Tag;
import com.example.myapplication.model.TagType;

import java.util.List;
import java.util.Locale;
import android.util.Log;

public class TagManager {
    private static final String TAG = "TagManager";

    public static void addTag(Context context, List<Album> allAlbums, Album album, Photo photo, Tag tag) {
        if (tag == null) return;
        if (tag.getTagType() == null) return;

        String albumName = album != null ? album.getName() : null;
        String imagePath = photo != null ? photo.getImagePath() : null;
        String filename = photo != null ? photo.getFilename() : null;

        boolean ok = DataStore.addTag(context, albumName, imagePath, filename, tag);
        if (ok) {
            Log.d(TAG, "Added tag '" + tag.toString() + "' to photo '" + (filename != null ? filename : imagePath) + "'");
        } else {
            Log.d(TAG, "Failed to add tag (may be duplicate or photo not found): " + tag.toString());
        }
    }

    public static void removeTag(Context context, List<Album> allAlbums, Album album, Photo photo, Tag tag) {
        if (tag == null) return;
        String albumName = album != null ? album.getName() : null;
        String imagePath = photo != null ? photo.getImagePath() : null;
        String filename = photo != null ? photo.getFilename() : null;

        boolean ok = DataStore.removeTag(context, albumName, imagePath, filename, tag);
        if (ok) {
            Log.d(TAG, "Removed tag '" + tag.toString() + "' from photo '" + (filename != null ? filename : imagePath) + "'");
        } else {
            Log.d(TAG, "Failed to remove tag (not found): " + tag.toString());
        }
    }

    private static Album findAlbumByName(List<Album> albums, String name) {
        if (albums == null || name == null) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        for (Album a : albums) {
            if (a.getName() != null && a.getName().toLowerCase(Locale.ROOT).equals(lower)) return a;
        }
        return null;
    }

    // EXIF write removed: tags are stored in JSON file only.

    private static Photo findPhotoByPath(Album album, String path) {
        if (album == null || path == null) return null;
        for (Photo p : album.getPhotos()) {
            if (path.equals(p.getImagePath())) return p;
        }
        return null;
    }

    private static Photo findPhotoByFilename(Album album, String filename) {
        if (album == null || filename == null) return null;
        for (Photo p : album.getPhotos()) {
            if (filename.equals(p.getFilename())) return p;
        }
        return null;
    }
}
