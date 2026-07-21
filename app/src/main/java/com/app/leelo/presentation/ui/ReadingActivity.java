package com.app.leelo.presentation.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.app.leelo.R;
import com.app.leelo.domain.model.Word;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.domain.repository.WordRepository;
import com.app.leelo.util.ReadingPreferences;
import com.app.leelo.util.TextPaginationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadingActivity extends AppCompatActivity {

    private static final int PAGE_CONTENT_PADDING_DP = 40;
    private ViewPager2 viewPager;
    private TextView pageIndicator;
    private TextView textTitle;
    private final List<String> pages = new ArrayList<>();
    private PageAdapter adapter;
    private TextRepository textRepository;
    private WordRepository wordRepository;
    private long textId;
    private String title;
    private boolean isDataLoaded = false;
    private float currentTextSize = 16f;
    private TextView previewText;
    private ReadingPreferences readingPrefs;
    private final Map<String, Word.State> savedWordsState = new HashMap<>();
    private final Map<String, String> savedWordsMeaning = new HashMap<>();
    private boolean isSavingWord = false;
    private String loadedContent;
    private boolean hasRestoredSavedPage = false;
    private int savedCurrentPage = 1;
    private int lastSavedPage = -1;
    private int lastSavedTotalPages = -1;

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

            if (words != null) {
                for (Word word : words) {
                    String rawWord = word.getWord();
                    if (rawWord == null || rawWord.isEmpty()) {
                        continue;
                    }

                    String key = normalizeWordKey(rawWord);
                    if (key.isEmpty()) {
                        continue;
                    }

                    savedWordsState.put(key, word.getState());
                    savedWordsMeaning.put(key, word.getMeaning());
                }
            }

            if (adapter != null) {
                adapter.updateWords(new HashMap<>(savedWordsState));
            }
        });
    }

    private void loadTextData() {
        textRepository.getTextById(textId).observe(this, text -> {
            if (text != null && text.content != null) {
                if (title == null) {
                    title = text.title;
                    setupToolbar(title);
                }
                savedCurrentPage = Math.max(1, text.currentPage);

                if (!text.content.equals(loadedContent)) {
                    loadedContent = text.content;
                    hasRestoredSavedPage = false;
                    processTextInChunks(text.content);
                } else if (!hasRestoredSavedPage && !pages.isEmpty()) {
                    restoreSavedPageIfNeeded();
                }
            } else {
                showErrorAndFinish();
            }
        });
    }

    private void processTextInChunks(String fullText) {
        if (viewPager.getWidth() == 0 || viewPager.getHeight() == 0) {
            viewPager.post(() -> processTextInChunks(fullText));
            return;
        }

        int horizontalPaddingPx = dpToPx(PAGE_CONTENT_PADDING_DP);
        int verticalPaddingPx = dpToPx(PAGE_CONTENT_PADDING_DP);
        int pageWidth = Math.max(1, viewPager.getWidth() - horizontalPaddingPx);
        int pageHeight = Math.max(1, viewPager.getHeight() - verticalPaddingPx);

        TextPaginationUtils.PageMetrics metrics = TextPaginationUtils.calculatePageMetrics(
                pageWidth,
                pageHeight,
                getResources().getDisplayMetrics().scaledDensity,
                currentTextSize
        );
        List<String> paginatedPages = TextPaginationUtils.paginateText(fullText, metrics);
        pages.clear();
        pages.addAll(paginatedPages);

        if (adapter == null) {
            adapter = new PageAdapter(pages, currentTextSize, savedWordsState, this);
            viewPager.setAdapter(adapter);

            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updatePageIndicator(position);
                    textTitle.setVisibility(position > 0 ? View.GONE : View.VISIBLE);
                    persistReadingProgress(position);
                }
            });
        } else {
            adapter.refreshContent(currentTextSize);
        }

        restoreSavedPageIfNeeded();
        isDataLoaded = true;
    }

    private void restoreSavedPageIfNeeded() {
        if (pages.isEmpty()) {
            updatePageIndicator(0);
            return;
        }

        int restoredPosition = Math.max(0, Math.min(savedCurrentPage - 1, pages.size() - 1));
        hasRestoredSavedPage = true;
        viewPager.setCurrentItem(restoredPosition, false);
        updatePageIndicator(restoredPosition);
        textTitle.setVisibility(restoredPosition > 0 ? View.GONE : View.VISIBLE);
        persistReadingProgress(restoredPosition);
    }

    private void updatePageIndicator(int position) {
        int currentPage = position + 1;
        int totalPages = pages.size();

        if (pageIndicator != null) {
            pageIndicator.setText("Page " + currentPage + "/" + totalPages);
        }

    }

    private void persistReadingProgress(int position) {
        int currentPage = position + 1;
        int totalPages = pages.size();

        if (textId == -1 || totalPages <= 0) {
            return;
        }

        if (currentPage == lastSavedPage && totalPages == lastSavedTotalPages) {
            return;
        }

        lastSavedPage = currentPage;
        lastSavedTotalPages = totalPages;
        textRepository.updateReadingProgress(textId, currentPage, totalPages, (success, id) -> {
        });
    }


    private void showErrorAndFinish() {
        finish();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(2);
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
        if (previewText != null) {
            previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSize);
        }
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
        if (loadedContent != null) {
            hasRestoredSavedPage = false;
            processTextInChunks(loadedContent);
        } else if (adapter != null) {
            adapter.setTextSize(currentTextSize);
        }
    }

    public void showWordDialog(String selectedWord, View anchorView) {
        String wordKey = normalizeWordKey(selectedWord);
        Word.State currentState = savedWordsState.get(wordKey);
        String currentMeaning = savedWordsMeaning.get(wordKey);
        boolean wordExists = currentState != null || !parseMeanings(currentMeaning).isEmpty();

        showWordPreviewPopup(selectedWord, wordKey, currentMeaning, currentState, wordExists, anchorView);
    }

    private void showWordPreviewPopup(
            String selectedWord,
            String wordKey,
            String currentMeaning,
            Word.State currentState,
            boolean wordExists,
            View anchorView
    ) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_word_preview, null);
        TextView previewWordText = popupView.findViewById(R.id.previewWordText);
        TextView previewEmptyText = popupView.findViewById(R.id.previewEmptyText);
        LinearLayout previewMeaningsContainer = popupView.findViewById(R.id.previewMeaningsContainer);
        MaterialButton actionRemove = popupView.findViewById(R.id.actionRemove);
        MaterialButton actionLearning = popupView.findViewById(R.id.actionLearning);
        MaterialButton actionLearned = popupView.findViewById(R.id.actionLearned);
        MaterialButton previewActionButton = popupView.findViewById(R.id.previewActionButton);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                Math.min(dpToPx(320), getResources().getDisplayMetrics().widthPixels - dpToPx(32)),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setElevation(dpToPx(12));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        previewWordText.setText(selectedWord);

        List<String> meanings = parseMeanings(currentMeaning);
        if (wordExists && !meanings.isEmpty()) {
            previewEmptyText.setVisibility(View.GONE);
            previewMeaningsContainer.setVisibility(View.VISIBLE);
            bindMeaningsPreview(previewMeaningsContainer, meanings);
            previewActionButton.setText("Editar");
        } else {
            previewEmptyText.setVisibility(View.VISIBLE);
            previewMeaningsContainer.setVisibility(View.GONE);
            previewActionButton.setText("Agregar significado");
        }

        actionRemove.setOnClickListener(v -> {
            deleteWordFromDatabase(wordKey);
            popupWindow.dismiss();
        });

        actionLearning.setOnClickListener(v -> {
            markButtonSelected(actionLearning, R.color.word_learning);
            markButtonDeselected(actionRemove);
            markButtonDeselected(actionLearned);
            saveWordToDatabase(wordKey, currentMeaning, Word.State.LEARNING);
        });

        actionLearned.setOnClickListener(v -> {
            markButtonSelected(actionLearned, android.R.color.transparent);
            markButtonDeselected(actionRemove);
            markButtonDeselected(actionLearning);
            saveWordToDatabase(wordKey, currentMeaning, Word.State.LEARNED);
        });

        if (currentState == Word.State.LEARNING) {
            markButtonSelected(actionLearning, R.color.word_learning);
        } else if (currentState == Word.State.LEARNED) {
            markButtonSelected(actionLearned, android.R.color.transparent);
        }

        previewActionButton.setOnClickListener(v -> {
            popupWindow.dismiss();
            Word.State latestState = savedWordsState.get(wordKey);
            String latestMeaning = savedWordsMeaning.get(wordKey);
            boolean isWordSaved = latestState != null || !parseMeanings(latestMeaning).isEmpty();
            showWordEditorSheet(selectedWord, wordKey, latestMeaning, latestState, isWordSaved);
        });

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(popupWindow.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int[] anchorLocation = new int[2];
        anchorView.getLocationOnScreen(anchorLocation);

        View rootView = getWindow().getDecorView();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int popupWidth = popupWindow.getWidth();
        int popupHeight = popupView.getMeasuredHeight();
        int screenMargin = dpToPx(16);

        int x = anchorLocation[0] + (anchorView.getWidth() - popupWidth) / 2;
        x = Math.max(screenMargin, Math.min(x, screenWidth - popupWidth - screenMargin));

        int preferredAboveY = anchorLocation[1] - popupHeight - dpToPx(12);
        int fallbackBelowY = anchorLocation[1] + anchorView.getHeight() + dpToPx(12);
        int y = preferredAboveY >= screenMargin
                ? preferredAboveY
                : Math.min(fallbackBelowY, screenHeight - popupHeight - screenMargin);

        popupWindow.showAtLocation(rootView, Gravity.TOP | Gravity.START, x, y);
    }

    private void showWordEditorSheet(
            String selectedWord,
            String wordKey,
            String currentMeaning,
            Word.State currentState,
            boolean wordExists
    ) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.sheet_word_editor, null);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);

        TextView selectedWordText = dialogView.findViewById(R.id.editorWordText);
        LinearLayout meaningInputsContainer = dialogView.findViewById(R.id.meaningInputsContainer);
        MaterialButton addMeaningButton = dialogView.findViewById(R.id.addMeaningButton);
        MaterialButton saveWordButton = dialogView.findViewById(R.id.saveWordButton);
        MaterialButton deleteWordButton = dialogView.findViewById(R.id.deleteWordButton);
        RadioButton radioLearning = dialogView.findViewById(R.id.radioLearning);
        RadioButton radioLearned = dialogView.findViewById(R.id.radioLearned);

        selectedWordText.setText(selectedWord);

        List<String> meanings = parseMeanings(currentMeaning);
        if (meanings.isEmpty()) {
            addMeaningInput(meaningInputsContainer, "");
        } else {
            for (String meaning : meanings) {
                addMeaningInput(meaningInputsContainer, meaning);
            }
        }

        addMeaningButton.setOnClickListener(v -> addMeaningInput(meaningInputsContainer, ""));

        if (currentState == Word.State.LEARNING) {
            radioLearning.setChecked(true);
        } else if (currentState == Word.State.LEARNED) {
            radioLearned.setChecked(true);
        } else {
            radioLearning.setChecked(true);
        }

        if (wordExists) {
            deleteWordButton.setVisibility(View.VISIBLE);
            deleteWordButton.setOnClickListener(v -> {
                dialog.dismiss();
                deleteWordFromDatabase(wordKey);
            });
        }

        saveWordButton.setOnClickListener(v -> {
            String meaning = collectMeanings(meaningInputsContainer);
            Word.State state = Word.State.LEARNING;

            if (radioLearning.isChecked()) {
                state = Word.State.LEARNING;
            } else if (radioLearned.isChecked()) {
                state = Word.State.LEARNED;
            }

            saveWordToDatabase(wordKey, meaning, state);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void markButtonSelected(MaterialButton button, int colorRes) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.TRANSPARENT));
        if (colorRes != android.R.color.transparent) {
            int color = ContextCompat.getColor(this, colorRes);
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
            button.setTextColor(color);
        } else {
            int gray = ContextCompat.getColor(this, R.color.word_learned);
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(gray));
            button.setTextColor(gray);
        }
        button.setStrokeWidth(dpToPx(2));
    }

    private void markButtonDeselected(MaterialButton button) {
        button.setBackgroundTintList(null);
        button.setStrokeWidth(dpToPx(1));
        button.setStrokeColor(null);
        android.util.TypedValue tv = new android.util.TypedValue();
        button.getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
        button.setTextColor(tv.data);
    }

    private void saveWordToDatabase(String word, String meaning, Word.State state) {
        isSavingWord = true;

        boolean wasAlreadySaved = savedWordsState.containsKey(word);

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
                    wordRepository.updateWord(existingWord, (success, id) -> isSavingWord = false);
                }
            });
        } else {
            wordRepository.insertWord(wordObj, (success, id) -> isSavingWord = false);
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
                wordRepository.deleteWord(w.getId(), (success, id) -> isSavingWord = false);
            } else {
                isSavingWord = false;
            }
        });
    }

    private static String normalizeWordKey(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder normalized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char currentChar = value.charAt(i);
            if (Character.isLetter(currentChar)) {
                normalized.append(Character.toLowerCase(currentChar));
            }
        }
        return normalized.toString().trim();
    }

    private void bindMeaningsPreview(LinearLayout container, List<String> meanings) {
        container.removeAllViews();
        for (String meaning : meanings) {
            TextView meaningView = new TextView(this);
            meaningView.setText("\u2022 " + meaning);
            meaningView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            meaningView.setTextColor(resolveThemeColor());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dpToPx(6);
            meaningView.setLayoutParams(params);
            container.addView(meaningView);
        }
    }

    private int resolveThemeColor() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, value, true);
        return value.data;
    }

    private void addMeaningInput(LinearLayout container, String value) {
        View inputRow = LayoutInflater.from(this).inflate(R.layout.item_meaning_input, container, false);
        TextInputEditText meaningInput = inputRow.findViewById(R.id.meaningInput);
        ImageButton removeMeaningButton = inputRow.findViewById(R.id.removeMeaningButton);

        meaningInput.setText(value);
        removeMeaningButton.setOnClickListener(v -> {
            container.removeView(inputRow);
            if (container.getChildCount() == 0) {
                addMeaningInput(container, "");
            }
        });

        container.addView(inputRow);
    }

    private String collectMeanings(LinearLayout container) {
        List<String> meanings = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            TextInputEditText meaningInput = row.findViewById(R.id.meaningInput);
            if (meaningInput == null || meaningInput.getText() == null) {
                continue;
            }

            String meaning = meaningInput.getText().toString().trim();
            if (!meaning.isEmpty()) {
                meanings.add(meaning);
            }
        }
        return TextUtils.join("\n", meanings);
    }

    private List<String> parseMeanings(String rawMeaning) {
        if (rawMeaning == null || rawMeaning.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalized = rawMeaning
                .replace("\r", "\n")
                .replace("•", "\n")
                .replace(";", "\n");

        List<String> meanings = new ArrayList<>();
        for (String piece : Arrays.asList(normalized.split("\n"))) {
            String cleaned = piece.trim();
            if (!cleaned.isEmpty()) {
                meanings.add(cleaned);
            }
        }
        return meanings;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

        private final List<String> pages;
        private final Map<String, Word.State> wordStates = new HashMap<>();
        private final ReadingActivity activity;
        private final LruCache<Integer, CharSequence> styledPageCache;
        private float textSize;

        public PageAdapter(List<String> pages, float textSize, Map<String, Word.State> wordStates, ReadingActivity activity) {
            this.pages = pages;
            this.textSize = textSize;
            this.activity = activity;
            this.wordStates.putAll(wordStates);
            this.styledPageCache = new LruCache<>(Math.max(6, Math.min(pages.size(), 30)));
            setHasStableIds(true);
        }

        public void setTextSize(float textSize) {
            this.textSize = textSize;
            notifyItemRangeChanged(0, getItemCount(), "text_size");
        }

        public void refreshContent(float textSize) {
            this.textSize = textSize;
            styledPageCache.evictAll();
            notifyDataSetChanged();
        }

        public void updateWords(Map<String, Word.State> wordStates) {
            this.wordStates.clear();
            this.wordStates.putAll(wordStates);
            styledPageCache.evictAll();
            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return pages.get(position).hashCode();
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
            holder.bind(getStyledPage(position), textSize);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) {
                holder.updateTextSize(textSize);
                return;
            }
            onBindViewHolder(holder, position);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        private CharSequence getStyledPage(int position) {
            CharSequence cachedPage = styledPageCache.get(position);
            if (cachedPage != null) {
                return cachedPage;
            }

            CharSequence styledPage = buildStyledPage(pages.get(position));
            styledPageCache.put(position, styledPage);
            return styledPage;
        }

        private CharSequence buildStyledPage(String text) {
            SpannableString spannable = new SpannableString(text);
            int textLength = text.length();
            int index = 0;

            while (index < textLength) {
                if (!Character.isLetter(text.charAt(index))) {
                    index++;
                    continue;
                }

                int start = index;
                while (index < textLength && Character.isLetter(text.charAt(index))) {
                    index++;
                }

                String cleanWord = normalizeWordKey(text.substring(start, index));
                if (cleanWord.isEmpty()) {
                    continue;
                }

                Word.State state = wordStates.get(cleanWord);
                int highlightColor = 0;
                if (state == null) {
                    highlightColor = activity.getColor(R.color.word_new);
                } else if (state == Word.State.LEARNING) {
                    highlightColor = activity.getColor(R.color.word_learning);
                }
                if (highlightColor != 0) {
                    spannable.setSpan(
                            new android.text.style.BackgroundColorSpan(highlightColor),
                            start, index,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                spannable.setSpan(
                        new WordClickableSpan(activity, cleanWord),
                        start,
                        index,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            return spannable;
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            private final TextView pageText;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                pageText = itemView.findViewById(R.id.pageText);
                pageText.setMovementMethod(LinkMovementMethod.getInstance());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pageText.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
                    pageText.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
                }
            }

            public void bind(CharSequence text, float textSize) {
                pageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                pageText.setText(text);
            }

            public void updateTextSize(float textSize) {
                pageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            }
        }

        private static class WordClickableSpan extends ClickableSpan {
            private final ReadingActivity activity;
            private final String word;

            private WordClickableSpan(ReadingActivity activity, String word) {
                this.activity = activity;
                this.word = word;
            }

            @Override
            public void onClick(@NonNull View widget) {
                activity.showWordDialog(word, widget);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                android.util.TypedValue tv = new android.util.TypedValue();
                activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
                ds.setColor(tv.data);
            }
        }
    }
}
