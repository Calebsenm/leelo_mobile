package com.app.leelo.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.model.Text;
import com.app.leelo.model.TextInfo;
import java.util.List;

public class TextViewModel extends ViewModel {

    private final TextRepository repository;

    public final LiveData<List<TextInfo>> texts;
    public final LiveData<List<TextInfo>> searchResults;

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>(
        ""
    );
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(
        false
    );
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public TextViewModel(TextRepository repository) {
        this.repository = repository;
        this.texts = repository.getAllTextInfo();

        this.searchResults = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return texts;
            }
            return repository.searchText(query);
        });
    }

    public LiveData<TextEntity> getTextById(long id) {
        return repository.getTextById(id);
    }

    // CREATE
    public void insertText(Text text) {
        insertText(text, null);
    }

    public void insertText(Text text, Runnable onSuccess) {
        isLoading.setValue(true);
        repository.insertText(text, (success, id) -> {
            isLoading.postValue(false);
            if (success) {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                error.postValue("Error al guardar el texto");
            }
        });
    }

    public void updateText(Text text) {
        updateText(text, null);
    }

    public void updateText(Text text, Runnable onSuccess) {
        isLoading.setValue(true);
        repository.updateText(text, (success, id) -> {
            isLoading.postValue(false);
            if (success) {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                error.postValue("Error al actualizar el texto");
            }
        });
    }

    // DELETE
    public void deleteText(long id) {
        isLoading.setValue(true);
        repository.deleteText(id, (success, deletedId) -> {
            isLoading.postValue(false);
            if (!success) error.postValue("No se pudo eliminar el texto");
        });
    }

    // SEARCH
    public void search(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<List<TextInfo>> getTexts() {
        return texts;
    }

    public LiveData<List<TextInfo>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void clearError() {
        error.setValue(null);
    }
}
