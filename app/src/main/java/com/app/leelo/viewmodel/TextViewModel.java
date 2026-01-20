package com.app.leelo.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import com.app.leelo.data.repository.TextRepository;
import com.app.leelo.model.TextInfo;

public class TextViewModel extends ViewModel {

    private final MutableLiveData<List<TextInfo>> texts = new MutableLiveData<>();
    private final TextRepository repository;

    public TextViewModel(TextRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<TextInfo>> getTexts() {
        return texts;
    }

    public void loadTexts() {
        repository.getAllTextInfoAsync(new TextRepository.OnGetAllTextInfoCallback() {
            @Override
            public void onGetAllTextInfoComplete(List<TextInfo> list) {
                texts.postValue(list);
            }
        });
    }
}
