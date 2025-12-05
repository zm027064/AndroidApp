
package com.example.myapplication.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Photo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String imagePath;
    private String filename;
    private List<Tag> tags;

    public Photo(String imagePath) {
        this.imagePath = imagePath;
        this.filename = new java.io.File(imagePath).getName();
        this.tags = new ArrayList<>();
    }

    public Photo(String imagePath, String filename) {
        this.imagePath = imagePath;
        this.filename = filename;
        this.tags = new ArrayList<>();
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void addTag(Tag tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    public List<Tag> getTagsByType(TagType type) {
        List<Tag> result = new ArrayList<>();
        for (Tag tag : tags) {
            if (tag.getTagType() == type) {
                result.add(tag);
            }
        }
        return result;
    }
}
