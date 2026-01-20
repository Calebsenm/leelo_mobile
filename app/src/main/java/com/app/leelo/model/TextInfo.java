package com.app.leelo.model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public class TextInfo {
    public Long id;

    @ColumnInfo(name = "title")
    public String titulo;

    public TextInfo() {}

    @Ignore
    public TextInfo(Long id, String titulo) {
        this.id = id;
        this.titulo = titulo;
    }

}