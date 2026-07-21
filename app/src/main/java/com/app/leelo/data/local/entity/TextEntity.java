package com.app.leelo.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;


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

    @ColumnInfo(name = "current_page")
    public int currentPage;

    @ColumnInfo(name = "total_pages")
    public int totalPages;

    public TextEntity() {
        this.currentPage = 0;
        this.totalPages = 0;
    }

    /*
    public LocalDate getCreationDateAsLocalDate() {
        if (creationDate == null) return null;
        return new java.util.Date(creationDate).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }
   */

    @Override
    public String toString() {
        return "TextEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") + '\'' +
                ", creationDate=" + creationDate +
                ", currentPage=" + currentPage +
                '}';
    }
}