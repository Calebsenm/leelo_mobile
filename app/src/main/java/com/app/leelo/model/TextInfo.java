package com.app.leelo.model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public class TextInfo {
    private Long id;

    @ColumnInfo(name = "title")
    private String title;

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
}
