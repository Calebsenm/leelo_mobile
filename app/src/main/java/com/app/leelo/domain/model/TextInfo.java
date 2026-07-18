package com.app.leelo.domain.model;

import androidx.room.Ignore;

public class TextInfo {
    private Long id;
    private String title;
    private int currentPage;
    private int totalPages;

    public TextInfo() {}

    @Ignore
    public TextInfo(Long id, String title, int currentPage, int totalPages) {
        this.id = id;
        this.title = title;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
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
