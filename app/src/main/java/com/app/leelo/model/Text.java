package com.app.leelo.model;

import java.time.LocalDate;

public class Text {

    private  Long idText;
    private  String tittle;
    private  String text;
    private LocalDate creationWordDate;

    public  Text(){

    }

    public Long getIdText() {
        return idText;
    }
    public void setIdText(Long idText) {
        this.idText = idText;
    }
    public String getTittle() {
        return tittle;
    }
    public void setTittle(String tittle) {
        this.tittle = tittle;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public LocalDate getCreationDate() {
        return creationWordDate;
    }
    public void setCreationDate(LocalDate creationDate) {
        this.creationWordDate = creationDate;
    }


}
