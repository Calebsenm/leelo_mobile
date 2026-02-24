package com.app.leelo.ui;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.app.leelo.R;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.domain.repository.WordRepository;
import com.app.leelo.model.Word;
import com.app.leelo.util.ReadingPreferences;
import com.app.leelo.utils.TextPaginationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView pageIndicator;
    private TextView textTitle;
    private List<String> pages = new ArrayList<>();
    private PageAdapter adapter;
    private TextRepository textRepository;
    private WordRepository wordRepository;
    private long textId;
    private String title;
    private boolean isDataLoaded = false;
    private float currentTextSize = 16f;
    private TextView previewText;
    private ReadingPreferences readingPrefs;
    private Map<String, Word.State> savedWordsState = new HashMap<>();
    private Map<String, String> savedWordsMeaning = new HashMap<>();
    private boolean isInitialLoad = true;
    private boolean isSavingWord = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        textRepository = TextRepository.RepositoryProvider.getInstance(this);
        wordRepository = WordRepository.RepositoryProvider.getInstance(this);
        readingPrefs = ReadingPreferences.getInstance(this);
        currentTextSize = readingPrefs.getTextSize();

        textId = getIntent().getLongExtra("text_id", -1);
        title = getIntent().getStringExtra("title");

        initViews();
        setupToolbar(title);
        
        if (textId != -1) {
            loadTextData();
        } else {
            showErrorAndFinish();
        }
        
        loadSavedWordsOnce();
    }

    private void loadSavedWordsOnce() {
        wordRepository.getAllWords().observe(this, words -> {
            if (isSavingWord) {
                return;
            }
            
            savedWordsState.clear();
            savedWordsMeaning.clear();
            
            android.util.Log.d("LOAD_WORDS", "Loading words, count: " + (words != null ? words.size() : 0));
            
            if (words != null) {
                for (Word word : words) {
                    if (word.getWord() != null && !word.getWord().isEmpty()) {
                        String key = word.getWord().toLowerCase().trim().replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚÑ]", "");
                        savedWordsState.put(key, word.getState());
                        savedWordsMeaning.put(key, word.getMeaning());
                        android.util.Log.d("LOAD_WORDS", "Word: " + key + " State: " + word.getState());
                    }
                }
            }
            
            android.util.Log.d("LOAD_WORDS", "Saved words state size: " + savedWordsState.size());
            
            if (adapter != null) {
                adapter.updateWords(new HashMap<>(savedWordsState));
            } else {
                android.util.Log.w("LOAD_WORDS", "Adapter is null, words will be loaded when adapter is created");
            }
        });
    }

    private void loadTextData() {
        showLoading(true);
        textRepository.getTextById(textId).observe(this, text -> {
            showLoading(false);
            if (text != null && text.content != null) {
                if (title == null) {
                    title = text.title;
                    setupToolbar(title);
                }
                processTextInChunks(text.content);
            } else {
                showErrorAndFinish();
            }
        });
    }

    private void processTextInChunks(String fullText) {
        TextPaginationUtils.PageMetrics metrics = TextPaginationUtils.calculatePageMetrics(
            getResources().getDisplayMetrics()
        );
        pages = TextPaginationUtils.paginateText(fullText, metrics);
        
        if (adapter == null) {
            adapter = new PageAdapter(pages, currentTextSize, savedWordsState, this);
            viewPager.setAdapter(adapter);
            
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updatePageIndicator(position);
                    if (position > 0) {
                        textTitle.setVisibility(View.GONE);
                    } else {
                        textTitle.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else {
            adapter.notifyDataSetChanged();
        }
        
        updatePageIndicator(0);
        isDataLoaded = true;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void updatePageIndicator(int position) {
        int currentPage = position + 1;
        int totalPages = pages.size();
        
        if (pageIndicator != null) {
            pageIndicator.setText("Page " + currentPage + "/" + totalPages);
        }
        
        if (progressBar != null && totalPages > 0) {
            int progress = (currentPage * 100) / totalPages;
            progressBar.setProgress(progress);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (!show && isDataLoaded && progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorAndFinish() {
        finish();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        progressBar = findViewById(R.id.progressBar);
        pageIndicator = findViewById(R.id.pageIndicator);
        textTitle = findViewById(R.id.textTitle);
    }

    private void setupToolbar(String title) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        
        toolbar.inflateMenu(R.menu.reading_menu);
        
        toolbar.setNavigationOnClickListener(v -> finish());
        
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                showReadingSettingsDialog();
                return true;
            }
            return false;
        });
        
        if (textTitle != null && title != null) {
            textTitle.setText(title);
        }
    }

    private void showReadingSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reading_settings, null);
        
        Slider fontSizeSlider = dialogView.findViewById(R.id.fontSizeSlider);
        previewText = dialogView.findViewById(R.id.previewText);
        
        fontSizeSlider.setValue(currentTextSize);
        
        fontSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            currentTextSize = value;
            if (previewText != null) {
                previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
            }
        });
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Configuración de lectura")
            .setView(dialogView)
            .setPositiveButton("Aplicar", (dialog, which) -> applyTextSizeToAllPages())
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void applyTextSizeToAllPages() {
        readingPrefs.setTextSize(currentTextSize);
        if (adapter != null) {
            adapter.setTextSize(currentTextSize);
        }
    }
    
    public void showWordDialog(String selectedWord) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_word, null);
        
        TextView selectedWordText = dialogView.findViewById(R.id.selectedWord);
        TextInputEditText meaningInput = dialogView.findViewById(R.id.meaningInput);
        RadioButton radioNew = dialogView.findViewById(R.id.radioNew);
        RadioButton radioLearning = dialogView.findViewById(R.id.radioLearning);
        RadioButton radioLearned = dialogView.findViewById(R.id.radioLearned);
        
        selectedWordText.setText(selectedWord);
        
        String wordKey = selectedWord.toLowerCase().trim().replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚÑ]", "");
        Word.State currentState = savedWordsState.get(wordKey);
        String currentMeaning = savedWordsMeaning.get(wordKey);
        boolean wordExists = currentState != null;
        
        if (currentMeaning != null) {
            meaningInput.setText(currentMeaning);
        }
        
        if (currentState == Word.State.LEARNING) {
            radioLearning.setChecked(true);
        } else if (currentState == Word.State.LEARNED) {
            radioLearned.setChecked(true);
        } else {
            radioNew.setChecked(true);
        }
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle(wordExists ? "Editar palabra" : "Agregar palabra")
            .setView(dialogView)
            .setPositiveButton("Guardar", (dialog, which) -> {
                String meaning = meaningInput.getText() != null ? 
                    meaningInput.getText().toString() : "";
                Word.State state = Word.State.NEW;
                
                if (radioLearning.isChecked()) {
                    state = Word.State.LEARNING;
                } else if (radioLearned.isChecked()) {
                    state = Word.State.LEARNED;
                }
                
                saveWordToDatabase(wordKey, meaning, state);
            })
            .setNegativeButton("Cancelar", null);
        
        if (wordExists) {
            builder.setNeutralButton("Eliminar", (dialog, which) -> {
                deleteWordFromDatabase(wordKey);
            });
        }
        
        builder.show();
    }

    private void saveWordToDatabase(String word, String meaning, Word.State state) {
        isSavingWord = true;
        
        final boolean wasAlreadySaved = savedWordsState.containsKey(word);
        
        savedWordsState.put(word, state);
        savedWordsMeaning.put(word, meaning);
        
        if (adapter != null) {
            adapter.updateWords(new HashMap<>(savedWordsState));
        }
        
        Word wordObj = new Word();
        wordObj.setWord(word);
        wordObj.setMeaning(meaning);
        wordObj.setState(state);
        
        if (wasAlreadySaved) {
            wordRepository.getWordByText(word).observe(this, existingWord -> {
                if (existingWord != null && existingWord.getId() != null) {
                    existingWord.setMeaning(meaning);
                    existingWord.setState(state);
                    wordRepository.updateWord(existingWord, (success, id) -> {
                        android.util.Log.d("SAVE_WORD", "Updated: " + word + " state: " + state);
                        isSavingWord = false;
                    });
                }
            });
        } else {
            wordRepository.insertWord(wordObj, (success, id) -> {
                android.util.Log.d("SAVE_WORD", "Inserted: " + word + " state: " + state);
                isSavingWord = false;
            });
        }
    }

    private void deleteWordFromDatabase(String word) {
        isSavingWord = true;
        
        savedWordsState.remove(word);
        savedWordsMeaning.remove(word);
        
        if (adapter != null) {
            adapter.updateWords(new HashMap<>(savedWordsState));
        }
        
        wordRepository.getWordByText(word).observe(this, w -> {
            if (w != null && w.getId() != null) {
                wordRepository.deleteWord(w.getId(), (success, id) -> {
                    android.util.Log.d("DELETE_WORD", "Deleted: " + word);
                    isSavingWord = false;
                });
            } else {
                isSavingWord = false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

        private final List<String> pages;
        private float textSize;
        private Map<String, Word.State> wordStates = new HashMap<>();
        private final ReadingActivity activity;

        public PageAdapter(List<String> pages, float textSize, Map<String, Word.State> wordStates, ReadingActivity activity) {
            this.pages = pages;
            this.textSize = textSize;
            this.wordStates = wordStates;
            this.activity = activity;
        }

        public void setTextSize(float textSize) {
            this.textSize = textSize;
            notifyDataSetChanged();
        }

        public void updateWords(Map<String, Word.State> wordStates) {
            this.wordStates.clear();
            this.wordStates.putAll(wordStates);
            android.util.Log.d("ADAPTER", "Updated words: " + this.wordStates);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.bind(pages.get(position), textSize, wordStates, activity);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            private final TextView pageText;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                pageText = itemView.findViewById(R.id.pageText);
            }

            public void bind(String text, float textSize, Map<String, Word.State> wordStates, ReadingActivity activity) {
                pageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                
                SpannableString spannable = new SpannableString(text);
                
                android.util.Log.d("BIND", "wordStates size: " + (wordStates != null ? wordStates.size() : 0));
                
                int textLength = text.length();
                int i = 0;
                
                while (i < textLength) {
                    char c = text.charAt(i);
                    
                    if (Character.isLetter(c)) {
                        int start = i;
                        StringBuilder wordBuilder = new StringBuilder();
                        
                        while (i < textLength && Character.isLetter(text.charAt(i))) {
                            wordBuilder.append(text.charAt(i));
                            i++;
                        }
                        
                        String originalWord = wordBuilder.toString();
                        String cleanWord = originalWord.toLowerCase().replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚÑ]", "");
                        
                        Word.State state = wordStates.get(cleanWord);
                        
                        android.util.Log.d("BIND", "Word: '" + cleanWord + "' State: " + state);
                        
                        int color;
                        
                        if (state == null) {
                            color = itemView.getContext().getColor(R.color.word_new);
                        } else if (state == Word.State.LEARNING) {
                            color = itemView.getContext().getColor(R.color.word_learning);
                        } else if (state == Word.State.LEARNED) {
                            color = itemView.getContext().getColor(R.color.word_learned);
                        } else {
                            color = itemView.getContext().getColor(R.color.word_new);
                        }
                        
                        String finalWord = cleanWord;
                        final int finalColor = color;
                        ClickableSpan clickableSpan = new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                activity.showWordDialog(finalWord);
                            }

                            @Override
                            public void updateDrawState(@NonNull TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setUnderlineText(false);
                                ds.setColor(finalColor);
                            }
                        };
                        
                        spannable.setSpan(new ForegroundColorSpan(color), start, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannable.setSpan(clickableSpan, start, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        i++;
                    }
                }
                
                pageText.setText(spannable);
                pageText.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
}
