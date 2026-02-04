package com.app.leelo.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import com.app.leelo.data.repository.TextRepository;
import com.app.leelo.model.TextInfo;

public class TextViewModel extends ViewModel {

    private final MutableLiveData<List<TextInfo>> texts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    private final TextRepository repository;

    public TextViewModel(TextRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<TextInfo>> getTexts() {
        return texts;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadTexts() {
        loading.setValue(true);
        repository.getAllTextInfoAsync(list -> {
            loading.postValue(false);
            texts.postValue(list);
        });
    }

    public void deleteText(long id) {
        loading.setValue(true);
        repository.deleteText(id, success -> {
            loading.postValue(false);
            if (success) {
                loadTexts();
            } else {
                error.postValue("No se pudo eliminar");
            }
        });
    }
}
