package com.example.myapplication.util;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.Tag;
import com.example.myapplication.model.TagType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Centralized data store that exposes high-level operations (create/delete/rename albums,
 * add/remove/move photos, add/remove tags, search) and persists immediately to a single
 * JSON file via StorageManager.
 */
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
        albumsCache.remove(a);
        persist(context);
        return true;
    }

    public static synchronized boolean renameAlbum(Context context, String oldName, String newName) {
        ensureLoaded(context);
        Album a = findAlbumByName(oldName);
        if (a == null || newName == null || newName.trim().isEmpty()) return false;
        // if another album with newName exists, fail
        Album existing = findAlbumByName(newName);
        if (existing != null && existing != a) return false;
        a.setName(newName.trim());
        persist(context);
        return true;
    }

    public static synchronized Photo addPhoto(Context context, String albumName, String imagePath, String filename) {
        ensureLoaded(context);
        Album a = findAlbumByName(albumName);
        if (a == null) return null;
        Photo p = (filename != null) ? new Photo(imagePath, filename) : new Photo(imagePath);
        a.addPhoto(p);
        persist(context);
        return p;
    }

    public static synchronized boolean removePhoto(Context context, String albumName, String imagePath, String filename) {
        ensureLoaded(context);
        Album a = findAlbumByName(albumName);
        if (a == null) return false;
        Photo p = findPhotoInAlbum(a, imagePath, filename);
        if (p == null) return false;
        a.removePhoto(p);
        persist(context);
        return true;
    }

    public static synchronized boolean movePhoto(Context context, String fromAlbum, String toAlbum, String imagePath, String filename) {
        ensureLoaded(context);
        Album src = findAlbumByName(fromAlbum);
        Album dst = findAlbumByName(toAlbum);
        if (src == null || dst == null) return false;
        Photo p = findPhotoInAlbum(src, imagePath, filename);
        if (p == null) return false;
        src.removePhoto(p);
        dst.addPhoto(p);
        persist(context);
        return true;
    }

    public static synchronized boolean renamePhoto(Context context, String albumName, String imagePath, String filename, String newFilename) {
        ensureLoaded(context);
        if (newFilename == null || newFilename.trim().isEmpty()) return false;
        Album a = findAlbumByName(albumName);
        Photo p = null;
        if (a != null) p = findPhotoInAlbum(a, imagePath, filename);
        if (p == null && imagePath != null) {
            for (Album aa : albumsCache) {
                Photo pp = findPhotoInAlbum(aa, imagePath, filename);
                if (pp != null) { a = aa; p = pp; break; }
            }
        }
        if (p == null) return false;
        p.setFilename(newFilename.trim());
        persist(context);
        return true;
    }

    public static synchronized boolean addTag(Context context, String albumName, String imagePath, String filename, Tag tag) {
        ensureLoaded(context);
        if (tag == null) return false;
        Album a = findAlbumByName(albumName);
        Photo p = null;
        if (a != null) p = findPhotoInAlbum(a, imagePath, filename);
        // If not found in album, try to find across all albums
        if (p == null && imagePath != null) {
            for (Album aa : albumsCache) {
                Photo pp = findPhotoInAlbum(aa, imagePath, filename);
                if (pp != null) { a = aa; p = pp; break; }
            }
        }
        if (p == null) return false;
        // prevent duplicate tags (case-insensitive)
        for (Tag t : p.getTags()) {
            if (t.getTagType() == tag.getTagType() && t.getTagValue().toLowerCase(Locale.ROOT).equals(tag.getTagValue().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        p.addTag(tag);
        persist(context);
        return true;
    }

    public static synchronized boolean removeTag(Context context, String albumName, String imagePath, String filename, Tag tag) {
        ensureLoaded(context);
        if (tag == null) return false;
        Album a = findAlbumByName(albumName);
        Photo p = null;
        if (a != null) p = findPhotoInAlbum(a, imagePath, filename);
        if (p == null && imagePath != null) {
            for (Album aa : albumsCache) {
                Photo pp = findPhotoInAlbum(aa, imagePath, filename);
                if (pp != null) { a = aa; p = pp; break; }
            }
        }
        if (p == null) return false;
        Tag toRemove = null;
        for (Tag t : p.getTags()) {
            if (t.getTagType() == tag.getTagType() && t.getTagValue().toLowerCase(Locale.ROOT).equals(tag.getTagValue().toLowerCase(Locale.ROOT))) {
                toRemove = t; break;
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
        String lower = name.toLowerCase(Locale.ROOT);
        for (Album a : albumsCache) {
            if (a.getName() != null && a.getName().toLowerCase(Locale.ROOT).equals(lower)) return a;
        }
        return null;
    }

    private static Photo findPhotoInAlbum(Album album, String imagePath, String filename) {
        if (album == null) return null;
        if (imagePath != null) {
            for (Photo p : album.getPhotos()) {
                if (imagePath.equals(p.getImagePath())) return p;
            }
        }
        if (filename != null) {
            for (Photo p : album.getPhotos()) {
                if (filename.equals(p.getFilename())) return p;
            }
        }
        return null;
    }

}
