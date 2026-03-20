package com.app.leelo.domain.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.model.Text;
import com.app.leelo.model.TextInfo;
import java.util.List;

public interface TextRepository {

    LiveData<List<TextInfo>> getAllTextInfo();
    LiveData<TextEntity> getTextById(long id);
    LiveData<List<TextInfo>> searchText(String query);

    void insertText(Text text, OnOperationCallback callback);
    void updateText(Text text, OnOperationCallback callback);
    void updateReadingProgress(long id, int currentPage, int totalPages, OnOperationCallback callback);
    void deleteText(long id, OnOperationCallback callback);

    interface OnOperationCallback {
        void onComplete(boolean success, long id);
    }

    final class RepositoryProvider {
        private static volatile TextRepository instance;

        public static TextRepository getInstance(Context context) {
            if (instance == null) {
                synchronized (TextRepository.class) {
                    if (instance == null) {
                        instance = com.app.leelo.data.repository.TextRepositoryImpl.getInstance(context);
                    }
                }
            }
            return instance;
        }
    }
}
