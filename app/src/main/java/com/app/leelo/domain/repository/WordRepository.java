package com.app.leelo.domain.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.app.leelo.domain.model.Word;
import java.util.List;

public interface WordRepository {

    LiveData<List<Word>> getAllWords();
    LiveData<List<Word>> getWordsByState(Word.State state);
    LiveData<Word> getWordByText(String word);
    LiveData<Word> getWordById(long id);
    LiveData<Integer> getTotalCount();
    LiveData<Integer> getCountByState(Word.State state);

    void insertWord(Word word, OnOperationCallback callback);
    void updateWord(Word word, OnOperationCallback callback);
    void deleteWord(long id, OnOperationCallback callback);
    boolean wordExistsSync(String word);

    interface OnOperationCallback {
        void onComplete(boolean success, long id);
    }

    final class RepositoryProvider {
        private static volatile WordRepository instance;

        public static WordRepository getInstance(Context context) {
            if (instance == null) {
                synchronized (WordRepository.class) {
                    if (instance == null) {
                        instance = com.app.leelo.data.repository.WordRepositoryImpl.getInstance(context);
                    }
                }
            }
            return instance;
        }
    }
}
