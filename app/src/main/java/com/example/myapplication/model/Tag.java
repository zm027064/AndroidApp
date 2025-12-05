
package com.example.myapplication.model;

import java.io.Serializable;

public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private TagType tagType;
    private String tagValue;

    public Tag(TagType tagType, String tagValue) {
        this.tagType = tagType;
        this.tagValue = tagValue;
    }

    public Tag(String tagTypeStr, String tagValue) {
        this.tagType = TagType.fromString(tagTypeStr);
        this.tagValue = tagValue;
    }

    public TagType getTagType() {
        return tagType;
    }

    public String getTagValue() {
        return tagValue;
    }

    @Override
    public String toString() {
        return tagType.getDisplayName() + ": " + tagValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Tag)) return false;
        Tag other = (Tag) obj;
        return this.tagType.equals(other.tagType) &&
               this.tagValue.equalsIgnoreCase(other.tagValue);
    }

    @Override
    public int hashCode() {
        return (tagType.toString() + tagValue.toLowerCase()).hashCode();
    }
}
