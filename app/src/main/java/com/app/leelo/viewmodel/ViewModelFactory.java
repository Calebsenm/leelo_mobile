package com.app.leelo.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.app.leelo.data.repository.TextRepository;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final TextRepository repository;

    public ViewModelFactory(TextRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TextViewModel.class)) {
            return (T) new TextViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}