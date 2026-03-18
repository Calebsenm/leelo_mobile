package com.app.leelo.model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public class TextInfo {
    private Long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "current_page")
    private int currentPage;

    @ColumnInfo(name = "total_pages")
    private int totalPages;

    public TextInfo() {}

    @Ignore
    public TextInfo(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
