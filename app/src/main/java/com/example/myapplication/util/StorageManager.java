package com.example.myapplication.util;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.Tag;
import com.example.myapplication.model.TagType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * StorageManager: JSON-backed persistence for albums/photos/tags.
 * Keeps a cached in-memory reference so Activities operate on the same instances.
 */
public class StorageManager {
    private static final String ALBUMS_FILENAME = "albums.json";
    private static final String TAG = "StorageManager";
    private static List<Album> cachedAlbums = null;

    public static synchronized void saveAlbums(Context context, List<Album> albums) {
        try {
            if (albums == null) albums = new ArrayList<>();
            cachedAlbums = albums;

            JSONArray root = new JSONArray();
            for (Album a : albums) {
                JSONObject albumObj = new JSONObject();
                albumObj.put("name", a.getName());
                JSONArray photosArr = new JSONArray();
                for (Photo p : a.getPhotos()) {
                    JSONObject pObj = new JSONObject();
                    pObj.put("imagePath", p.getImagePath());
                    pObj.put("filename", p.getFilename());
                    JSONArray tagsArr = new JSONArray();
                    for (Tag t : p.getTags()) {
                        JSONObject tObj = new JSONObject();
                        tObj.put("type", t.getTagType().getDisplayName());
                        tObj.put("value", t.getTagValue());
                        tagsArr.put(tObj);
                    }
                    pObj.put("tags", tagsArr);
                    photosArr.put(pObj);
                }
                albumObj.put("photos", photosArr);
                root.put(albumObj);
            }

            File filesDir = context.getFilesDir();
            File outFile = new File(filesDir, ALBUMS_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
                fos.write(bytes);
                fos.flush();
            }
            Log.d(TAG, "Saved " + albums.size() + " albums to " + outFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving albums (json)", e);
        }
    }

    public static synchronized List<Album> loadAlbums(Context context) {
        if (cachedAlbums != null) {
            Log.d(TAG, "Returning cached albums (in-memory)");
            return cachedAlbums;
        }

        try {
            File filesDir = context.getFilesDir();
            File inFile = new File(filesDir, ALBUMS_FILENAME);
            if (!inFile.exists()) {
                cachedAlbums = new ArrayList<>();
                return cachedAlbums;
            }

            byte[] data;
            try (FileInputStream fis = new FileInputStream(inFile)) {
                data = fis.readAllBytes();
            }

            String content = new String(data, StandardCharsets.UTF_8);
            JSONArray root = new JSONArray(content);
            List<Album> albums = new ArrayList<>();
            for (int i = 0; i < root.length(); i++) {
                JSONObject albumObj = root.getJSONObject(i);
                String name = albumObj.optString("name", "");
                Album album = new Album(name);
                JSONArray photosArr = albumObj.optJSONArray("photos");
                if (photosArr != null) {
                    for (int j = 0; j < photosArr.length(); j++) {
                        JSONObject pObj = photosArr.getJSONObject(j);
                        String imagePath = pObj.optString("imagePath", null);
                        String filename = pObj.optString("filename", null);
                        Photo photo = (filename != null)
                                ? new Photo(imagePath, filename)
                                : new Photo(imagePath);
                        JSONArray tagsArr = pObj.optJSONArray("tags");
                        if (tagsArr != null && tagsArr.length() > 0) {
                            for (int k = 0; k < tagsArr.length(); k++) {
                                JSONObject tObj = tagsArr.getJSONObject(k);
                                String type = tObj.optString("type", "Person");
                                String value = tObj.optString("value", "");
                                Tag tag = new Tag(type, value);
                                photo.addTag(tag);
                            }
                        }
                        album.addPhoto(photo);
                    }
                }
                albums.add(album);
            }
            cachedAlbums = albums;
            Log.d(TAG, "Loaded " + albums.size() + " albums from " + inFile.getAbsolutePath());
            return albums;
        } catch (Exception e) {
            Log.e(TAG, "Error loading albums (json)", e);
            cachedAlbums = new ArrayList<>();
            return cachedAlbums;
        }
    }

    public static List<String> getAllAlbumNames(List<Album> albums) {
        List<String> names = new ArrayList<>();
        if (albums == null) return names;
        for (Album a : albums) names.add(a.getName());
        return names;
    }
}
