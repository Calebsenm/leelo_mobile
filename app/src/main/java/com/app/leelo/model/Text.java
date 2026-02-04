package com.app.leelo.model;

import java.time.LocalDate;

public class Text {

    private  Long idText;
    private  String title;
    private  String text;
    private LocalDate creationDate;

    public  Text(){
    }

    public Long getIdText() {
        return idText;
    }

    public void setIdText(Long idText) {
        this.idText = idText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

}
