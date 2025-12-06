package com.example.myapplication.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.Tag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataStore {
    private static final String TAG = "DataStore";

    private static List<Album> albumsCache = null;

    private static synchronized void ensureLoaded(Context context) {
        if (albumsCache == null) {
            albumsCache = StorageManager.loadAlbums(context);
            if (albumsCache == null) albumsCache = new ArrayList<>();
        }
    }

    private static synchronized void persist(Context context) {
        if (albumsCache == null) albumsCache = new ArrayList<>();
        StorageManager.saveAlbums(context, albumsCache);
    }

    public static synchronized List<Album> getAlbums(Context context) {
        ensureLoaded(context);
        return albumsCache;
    }

    public static synchronized boolean createAlbum(Context context, String name) {
        ensureLoaded(context);
        if (name == null || name.trim().isEmpty()) return false;
        if (findAlbumByName(name) != null) return false;
        Album a = new Album(name.trim());
        albumsCache.add(a);
        persist(context);
        return true;
    }

    public static synchronized boolean deleteAlbum(Context context, String name) {
        ensureLoaded(context);
        Album a = findAlbumByName(name);
        if (a == null) return false;

        for (Photo p : a.getPhotos()) {
            File f = new File(p.getImagePath());
            if (f.exists()) {
                f.delete();
            }
        }
        albumsCache.remove(a);
        persist(context);
        return true;
    }

    public static synchronized boolean renameAlbum(Context context, String oldName, String newName) {
        ensureLoaded(context);
        Album a = findAlbumByName(oldName);
        if (a == null || newName == null || newName.trim().isEmpty()) return false;
        Album existing = findAlbumByName(newName);
        if (existing != null && existing != a) return false;
        a.setName(newName.trim());
        persist(context);
        return true;
    }

    public static synchronized Photo addPhoto(Context context, String albumName, Uri imageUri) {
        ensureLoaded(context);
        Album a = findAlbumByName(albumName);
        if (a == null) return null;

        String savedPath = saveImageFromUri(context, imageUri);
        if (savedPath == null) {
            return null;
        }

        Photo p = new Photo(savedPath);
        a.addPhoto(p);
        persist(context);
        return p;
    }

    public static synchronized boolean removePhotoById(Context context, String albumName, String photoId) {
        ensureLoaded(context);
        Album a = findAlbumByName(albumName);
        if (a == null || photoId == null) return false;
        Photo p = findPhotoInAlbumById(a, photoId);
        if (p == null) return false;

        File f = new File(p.getImagePath());
        if (f.exists()) {
            f.delete();
        }

        a.removePhoto(p);
        persist(context);
        return true;
    }

    public static synchronized boolean movePhotoById(Context context, String fromAlbumName, String toAlbumName, String photoId) {
        ensureLoaded(context);
        Album src = findAlbumByName(fromAlbumName);
        Album dst = findAlbumByName(toAlbumName);
        if (src == null || dst == null || photoId == null) return false;
        Photo p = findPhotoInAlbumById(src, photoId);
        if (p == null) return false;

        src.removePhoto(p);
        dst.addPhoto(p);
        persist(context);
        return true;
    }

    public static synchronized boolean renamePhotoById(Context context, String photoId, String newFilename) {
        ensureLoaded(context);
        if (newFilename == null || newFilename.trim().isEmpty() || photoId == null) return false;
        Photo p = findPhotoById(photoId);
        if (p == null) return false;
        p.setFilename(newFilename.trim());
        persist(context);
        return true;
    }

    public static synchronized boolean addTag(Context context, String photoId, Tag tag) {
        ensureLoaded(context);
        if (tag == null || photoId == null) return false;
        Photo p = findPhotoById(photoId);
        if (p == null) return false;
        for (Tag t : p.getTags()) {
            if (t.getTagType() == tag.getTagType() && t.getTagValue().equalsIgnoreCase(tag.getTagValue())) {
                return false;
            }
        }
        p.addTag(tag);
        persist(context);
        return true;
    }

    public static synchronized boolean removeTag(Context context, String photoId, Tag tag) {
        ensureLoaded(context);
        if (tag == null || photoId == null) return false;
        Photo p = findPhotoById(photoId);
        if (p == null) return false;
        Tag toRemove = null;
        for (Tag t : p.getTags()) {
            if (t.getTagType() == tag.getTagType() && t.getTagValue().equalsIgnoreCase(tag.getTagValue())) {
                toRemove = t;
                break;
            }
        }
        if (toRemove == null) return false;
        p.removeTag(toRemove);
        persist(context);
        return true;
    }

    public static synchronized Album findAlbumByName(String name) {
        if (albumsCache == null) return null;
        if (name == null) return null;
        for (Album a : albumsCache) {
            if (a.getName().equalsIgnoreCase(name)) return a;
        }
        return null;
    }

    private static Photo findPhotoInAlbumById(Album album, String photoId) {
        if (album == null || photoId == null) return null;
        for (Photo p : album.getPhotos()) {
            if (photoId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    public static synchronized Photo findPhotoById(String photoId) {
        if (photoId == null || albumsCache == null) return null;
        for (Album a : albumsCache) {
            for (Photo p : a.getPhotos()) {
                if (photoId.equals(p.getId())) {
                    return p;
                }
            }
        }
        return null;
    }

    private static String saveImageFromUri(Context context, Uri imageUri) {
        try {
            String imagesDirName = "images";
            File imagesDir = new File(context.getFilesDir(), imagesDirName);
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            String fileName = UUID.randomUUID().toString() + ".jpg";
            File outFile = new File(imagesDir, fileName);

            try (InputStream in = context.getContentResolver().openInputStream(imageUri);
                 OutputStream out = new FileOutputStream(outFile)) {
                if (in == null) {
                    Log.e(TAG, "Could not open input stream for URI: " + imageUri);
                    return null;
                }
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }

            Log.d(TAG, "Saved image to: " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image from URI: " + imageUri, e);
            return null;
        }
    }
}
