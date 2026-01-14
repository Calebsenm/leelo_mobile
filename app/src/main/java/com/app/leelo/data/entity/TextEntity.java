package com.app.leelo.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

import java.time.LocalDate;


@Entity(tableName = "texts")
public class TextEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public Long id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "creation_date")
    public Long creationDate;

    @ColumnInfo(name = "modification_date")
    public Long modificationDate;

    public TextEntity() {}

    public TextEntity(String title, String content) {
        this.title = title;
        this.content = content;
        long now = System.currentTimeMillis();
        this.creationDate = now;
        this.modificationDate = now;
    }

    public LocalDate getCreationDateAsLocalDate() {
        if (creationDate == null) return null;
        return new java.util.Date(creationDate).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    @Override
    public String toString() {
        return "TextEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }
}