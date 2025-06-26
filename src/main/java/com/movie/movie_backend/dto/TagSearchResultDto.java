package com.movie.movie_backend.dto;

public class TagSearchResultDto {
    private Long tagId;
    private String tagName;

    public TagSearchResultDto(Long tagId, String tagName) {
        this.tagId = tagId;
        this.tagName = tagName;
    }

    public Long getTagId() { return tagId; }
    public String getTagName() { return tagName; }

    public void setTagId(Long tagId) { this.tagId = tagId; }
    public void setTagName(String tagName) { this.tagName = tagName; }
} 
