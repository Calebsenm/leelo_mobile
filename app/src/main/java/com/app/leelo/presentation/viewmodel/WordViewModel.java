package com.app.leelo.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.app.leelo.domain.repository.WordRepository;
import com.app.leelo.model.Word;

import java.util.List;

public class WordViewModel extends ViewModel {

    private final WordRepository repository;

    public final LiveData<List<Word>> allWords;
    public final LiveData<List<Word>> newWords;
    public final LiveData<List<Word>> learningWords;
    public final LiveData<List<Word>> learnedWords;

    public final LiveData<Integer> totalCount;
    public final LiveData<Integer> newCount;
    public final LiveData<Integer> learningCount;
    public final LiveData<Integer> learnedCount;

    private final MutableLiveData<FilterType> currentFilter = new MutableLiveData<>(FilterType.ALL);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public enum FilterType {
        ALL, LEARNING, LEARNED
    }

    public WordViewModel(WordRepository repository) {
        this.repository = repository;

        this.allWords = repository.getAllWords();
        this.newWords = repository.getWordsByState(Word.State.NEW);
        this.learningWords = repository.getWordsByState(Word.State.LEARNING);
        this.learnedWords = repository.getWordsByState(Word.State.LEARNED);

        this.totalCount = repository.getTotalCount();
        this.newCount = repository.getCountByState(Word.State.NEW);
        this.learningCount = repository.getCountByState(Word.State.LEARNING);
        this.learnedCount = repository.getCountByState(Word.State.LEARNED);
    }

    public LiveData<List<Word>> getFilteredWords() {
        return Transformations.switchMap(currentFilter, filter -> {
            switch (filter) {

                case LEARNING:
                    return learningWords;
                case LEARNED:
                    return learnedWords;
                default:
                    return allWords;
            }
        });
    }

    public void setFilter(FilterType filter) {
        currentFilter.setValue(filter);
    }

    public FilterType getCurrentFilter() {
        return currentFilter.getValue();
    }

    public LiveData<Word> getWordByText(String word) {
        return repository.getWordByText(word);
    }

    public void insertWord(Word word) {
        isLoading.setValue(true);
        repository.insertWord(word, (success, id) -> {
            isLoading.postValue(false);
            if (!success) error.postValue("Error al guardar la palabra");
        });
    }

    public void updateWord(Word word) {
        isLoading.setValue(true);
        repository.updateWord(word, (success, id) -> {
            isLoading.postValue(false);
            if (!success) error.postValue("Error al actualizar la palabra");
        });
    }

    public void updateWordState(long id, Word.State newState) {
        isLoading.setValue(true);
        
        getWordByIdLive(id).observeForever(word -> {
            if (word != null) {
                word.setState(newState);
                repository.updateWord(word, (success, id1) -> {
                    isLoading.postValue(false);
                    if (!success) error.postValue("Error al actualizar el estado");
                });
            }
        });
    }

    private LiveData<Word> getWordByIdLive(long id) {
        return repository.getWordById(id);
    }

    public void deleteWord(long id) {
        isLoading.setValue(true);
        repository.deleteWord(id, (success, deletedId) -> {
            isLoading.postValue(false);
            if (!success) error.postValue("No se pudo eliminar la palabra");
        });
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
