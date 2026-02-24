package com.app.leelo.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.app.leelo.domain.repository.WordRepository;

public class WordViewModelFactory implements ViewModelProvider.Factory {

    private final WordRepository repository;

    public WordViewModelFactory(WordRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WordViewModel.class)) {
            return (T) new WordViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
