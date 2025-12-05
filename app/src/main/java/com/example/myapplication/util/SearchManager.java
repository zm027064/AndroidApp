package com.example.myapplication.util;

import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.Tag;
import com.example.myapplication.model.TagType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchManager {

    public enum SearchOperator {
        AND, OR
    }

    public static List<Photo> searchByTag(List<Album> albums, TagType tagType, String tagValue) {
        List<Photo> results = new ArrayList<>();
        if (albums == null || tagType == null || tagValue == null) return results;
        String searchValue = tagValue.toLowerCase(Locale.ROOT);

        for (Album album : albums) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getTagType() == tagType) {
                        String tv = tag.getTagValue();
                        if (tv != null && tv.toLowerCase(Locale.ROOT).startsWith(searchValue)) {
                            if (!results.contains(photo)) results.add(photo);
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }

    public static List<Photo> searchByTags(List<Album> albums,
                                           TagType tagType1, String tagValue1,
                                           TagType tagType2, String tagValue2,
                                           SearchOperator operator) {
        List<Photo> r1 = searchByTag(albums, tagType1, tagValue1);
        List<Photo> r2 = searchByTag(albums, tagType2, tagValue2);

        if (operator == SearchOperator.AND) {
            r1.retainAll(r2);
            return r1;
        } else {
            Set<Photo> combined = new HashSet<>(r1);
            combined.addAll(r2);
            return new ArrayList<>(combined);
        }
    }

    public static List<String> getTagValueSuggestions(List<Album> albums, TagType tagType) {
        Set<String> suggestions = new HashSet<>();
        if (albums == null || tagType == null) return new ArrayList<>();
        for (Album album : albums) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getTagType() == tagType && tag.getTagValue() != null) {
                        suggestions.add(tag.getTagValue());
                    }
                }
            }
        }
        List<String> sorted = new ArrayList<>(suggestions);
        java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    public static List<String> getAutocompleteSuggestions(List<Album> albums,
                                                          TagType tagType, String prefix) {
        List<String> all = getTagValueSuggestions(albums, tagType);
        List<String> filtered = new ArrayList<>();
        if (prefix == null) return filtered;
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String s : all) {
            if (s != null && s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
