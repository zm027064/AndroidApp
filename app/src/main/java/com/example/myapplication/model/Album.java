
package com.example.myapplication.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Album implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private List<Photo> photos;

    public Album(String name) {
        this.name = name;
        this.photos = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void addPhoto(Photo photo) {
        if (!photos.contains(photo)) {
            photos.add(photo);
        }
    }

    public void removePhoto(Photo photo) {
        photos.remove(photo);
    }

    public Photo getPhotoAt(int index) {
        if (index >= 0 && index < photos.size()) {
            return photos.get(index);
        }
        return null;
    }

    public int getPhotoCount() {
        return photos.size();
    }

    public int getPhotoIndex(Photo photo) {
        return photos.indexOf(photo);
    }

    public boolean movePhotoToAlbum(Photo photo, Album targetAlbum) {
        if (photos.contains(photo)) {
            photos.remove(photo);
            targetAlbum.addPhoto(photo);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Album)) return false;
        Album other = (Album) obj;
        return this.name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
}
