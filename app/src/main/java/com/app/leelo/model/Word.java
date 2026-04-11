package com.app.leelo.model;

public class Word {

    public enum State {
        LEARNING(2),
        LEARNED(3);

        private final int value;
        State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static State fromValue(int value) {
            for (State state : values()) {
                if (state.value == value) return state;
            }
            return LEARNING;
        }
    }

    private Long id;
    private String word;
    private String meaning;
    private State state;
    private Long createdAt;
    private Long updatedAt;

    public Word() {
        this.state = State.LEARNING;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
