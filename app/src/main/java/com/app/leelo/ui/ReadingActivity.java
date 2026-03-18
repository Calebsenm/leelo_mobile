package com.app.leelo.ui;

import android.os.Bundle;
import android.content.res.ColorStateList;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.app.leelo.R;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.domain.repository.WordRepository;
import com.app.leelo.model.Word;
import com.app.leelo.util.ReadingPreferences;
import com.app.leelo.util.WordTextUtils;
import com.app.leelo.utils.TextPaginationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView pageIndicator;
    private TextView textTitle;
    private final List<String> pages = new ArrayList<>();
    private PageAdapter adapter;
    private TextRepository textRepository;
    private WordRepository wordRepository;
    private long textId;
    private String title;
    private String fullText = "";
    private boolean isDataLoaded = false;
    private float currentTextSize = 16f;
    private TextView previewText;
    private ReadingPreferences readingPrefs;
    private final Map<String, Word.State> savedWordsState = new HashMap<>();
    private final Map<String, String> savedWordsMeaning = new HashMap<>();
    private boolean isSavingWord = false;
    private int persistedPage = 0;
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

        observeSavedWords();
    }

    private void observeSavedWords() {
        wordRepository.getAllWords().observe(this, words -> {
            if (isSavingWord) {
                return;
            }

            savedWordsState.clear();
            savedWordsMeaning.clear();

            if (words != null) {
                for (Word word : words) {
                    String key = WordTextUtils.normalizeWord(word.getWord());
                    if (!key.isEmpty()) {
                        savedWordsState.put(key, word.getState());
                        savedWordsMeaning.put(key, word.getMeaning());
                    }
                }
            }

            if (adapter != null) {
                adapter.updateWords(new HashMap<>(savedWordsState));
            }
        });
    }

    private void loadTextData() {
        showLoading(true);
        textRepository.getTextById(textId).observe(this, text -> {
            showLoading(false);
            if (text == null || text.content == null || text.content.trim().isEmpty()) {
                showErrorAndFinish();
                return;
            }

            fullText = text.content;
            persistedPage = Math.max(text.currentPage, 0);
            lastSavedPage = persistedPage;
            lastSavedTotalPages = Math.max(text.totalPages, 0);
            if (title == null) {
                title = text.title;
                setupToolbar(title);
            }
            paginateAndRender(false);
        });
    }

    private void paginateAndRender(boolean preserveCurrentPage) {
        TextPaginationUtils.PageMetrics metrics = TextPaginationUtils.calculatePageMetrics(
                getResources().getDisplayMetrics(),
                currentTextSize
        );
        List<String> paginatedPages = TextPaginationUtils.paginateText(fullText, metrics);
        int targetPage = preserveCurrentPage ? viewPager.getCurrentItem() : persistedPage;

        pages.clear();
        pages.addAll(paginatedPages);

        if (adapter == null) {
            adapter = new PageAdapter(pages, currentTextSize, savedWordsState, this);
            viewPager.setAdapter(adapter);
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updatePageIndicator(position);
                    textTitle.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                    persistReadingProgress(position);
                }
            });
        } else {
            adapter.setTextSize(currentTextSize);
            adapter.updateWords(new HashMap<>(savedWordsState));
            adapter.notifyDataSetChanged();
        }

        int safePage = pages.isEmpty() ? 0 : Math.min(targetPage, pages.size() - 1);
        viewPager.setCurrentItem(safePage, false);
        updatePageIndicator(safePage);
        persistReadingProgress(safePage);
        isDataLoaded = true;
        progressBar.setVisibility(View.VISIBLE);
    }

    private void updatePageIndicator(int position) {
        int totalPages = Math.max(pages.size(), 1);
        int currentPage = Math.min(position + 1, totalPages);

        pageIndicator.setText("Página " + currentPage + "/" + totalPages);
        progressBar.setProgress((currentPage * 100) / totalPages);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show && isDataLoaded) {
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
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.reading_menu);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                showReadingSettingsDialog();
                return true;
            }
            return false;
        });

        if (title != null) {
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
        if (isDataLoaded) {
            paginateAndRender(true);
        }
    }

    public void showWordDialog(String selectedWord, String wordKey) {
        List<String> meanings = parseMeanings(savedWordsMeaning.get(wordKey));
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_word_preview, null);

        TextView previewWord = dialogView.findViewById(R.id.previewWord);
        LinearLayout previewMeaningContainer = dialogView.findViewById(R.id.previewMeaningContainer);
        MaterialButton previewActionButton = dialogView.findViewById(R.id.previewActionButton);
        View quickWordCard = dialogView.findViewById(R.id.quickWordCard);
        boolean wordExists = savedWordsState.containsKey(wordKey);
        boolean hasMeanings = !meanings.isEmpty();

        previewWord.setText(selectedWord);
        bindPreviewMeanings(previewMeaningContainer, wordExists, meanings);
        previewActionButton.setText(hasMeanings ? "Editar" : "Add");

        final androidx.appcompat.app.AlertDialog[] dialogRef = new androidx.appcompat.app.AlertDialog[1];
        View.OnClickListener actionClickListener = v -> {
            if (dialogRef[0] != null) {
                dialogRef[0].dismiss();
            }
            showWordEditorDialog(selectedWord, wordKey, true, !hasMeanings);
        };
        quickWordCard.setOnClickListener(actionClickListener);
        previewActionButton.setOnClickListener(actionClickListener);

        dialogRef[0] = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .show();
    }

    private void showWordEditorDialog(
            String selectedWord,
            String wordKey,
            boolean preloadMeanings,
            boolean appendEmptyMeaning
    ) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_word, null);

        TextView selectedWordText = dialogView.findViewById(R.id.selectedWord);
        MaterialButton stateNewButton = dialogView.findViewById(R.id.stateNewButton);
        MaterialButton stateLearningButton = dialogView.findViewById(R.id.stateLearningButton);
        MaterialButton stateLearnedButton = dialogView.findViewById(R.id.stateLearnedButton);
        LinearLayout meaningInputsContainer = dialogView.findViewById(R.id.meaningInputsContainer);
        MaterialButton addMeaningRowButton = dialogView.findViewById(R.id.addMeaningRowButton);
        MaterialButton deleteWordButton = dialogView.findViewById(R.id.deleteWordButton);
        MaterialButton cancelWordButton = dialogView.findViewById(R.id.cancelWordButton);
        MaterialButton saveWordButton = dialogView.findViewById(R.id.saveWordButton);

        selectedWordText.setText(selectedWord);

        Word.State currentState = savedWordsState.get(wordKey);
        List<String> meanings = parseMeanings(savedWordsMeaning.get(wordKey));
        boolean wordExists = currentState != null;
        List<String> editorMeanings = new ArrayList<>();
        Word.State initialState = currentState != null ? currentState : Word.State.NEW;
        final Word.State[] selectedState = new Word.State[] { initialState };

        bindStateSelector(
                selectedState[0],
                stateNewButton,
                stateLearningButton,
                stateLearnedButton
        );
        stateNewButton.setOnClickListener(v -> {
            selectedState[0] = Word.State.NEW;
            bindStateSelector(selectedState[0], stateNewButton, stateLearningButton, stateLearnedButton);
        });
        stateLearningButton.setOnClickListener(v -> {
            selectedState[0] = Word.State.LEARNING;
            bindStateSelector(selectedState[0], stateNewButton, stateLearningButton, stateLearnedButton);
        });
        stateLearnedButton.setOnClickListener(v -> {
            selectedState[0] = Word.State.LEARNED;
            bindStateSelector(selectedState[0], stateNewButton, stateLearningButton, stateLearnedButton);
        });

        if (preloadMeanings) {
            editorMeanings.addAll(meanings);
        }
        if (appendEmptyMeaning || editorMeanings.isEmpty()) {
            editorMeanings.add("");
        }

        showMeaningEditor(meaningInputsContainer, editorMeanings);
        addMeaningRowButton.setOnClickListener(v -> addMeaningInput(meaningInputsContainer, ""));

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(resolveEditorTitle(wordExists, appendEmptyMeaning))
                .setView(dialogView)
                .show();

        deleteWordButton.setVisibility(wordExists ? View.VISIBLE : View.GONE);
        cancelWordButton.setOnClickListener(v -> dialog.dismiss());
        saveWordButton.setOnClickListener(v -> {
            saveWordToDatabase(
                    wordKey,
                    serializeMeanings(collectMeanings(meaningInputsContainer)),
                    selectedState[0]
            );
            dialog.dismiss();
        });
        deleteWordButton.setOnClickListener(v -> {
            deleteWordFromDatabase(wordKey);
            dialog.dismiss();
        });
    }

    private String resolveEditorTitle(boolean wordExists, boolean appendEmptyMeaning) {
        if (!wordExists) {
            return "Agregar palabra";
        }
        if (appendEmptyMeaning) {
            return "Agregar significado";
        }
        return "Editar palabra";
    }

    private void bindStateSelector(
            Word.State selectedState,
            MaterialButton stateNewButton,
            MaterialButton stateLearningButton,
            MaterialButton stateLearnedButton
    ) {
        styleStateButton(stateNewButton, Word.State.NEW, selectedState == Word.State.NEW);
        styleStateButton(stateLearningButton, Word.State.LEARNING, selectedState == Word.State.LEARNING);
        styleStateButton(stateLearnedButton, Word.State.LEARNED, selectedState == Word.State.LEARNED);
    }

    private void styleStateButton(MaterialButton button, Word.State state, boolean selected) {
        int color = getStateColor(state);
        button.setBackgroundTintList(ColorStateList.valueOf(color));
        button.setStrokeColor(ColorStateList.valueOf(selected ? color : resolveThemeColor(com.google.android.material.R.attr.colorOutline)));
        button.setStrokeWidth(dpToPx(selected ? 3 : 1));
        button.setAlpha(selected ? 1f : 0.45f);
        button.setContentDescription(getStateLabel(state));
    }

    private int getStateColor(Word.State state) {
        if (state == Word.State.LEARNING) {
            return getColor(R.color.word_learning);
        }
        if (state == Word.State.LEARNED) {
            return getColor(R.color.word_learned);
        }
        return getColor(R.color.word_new);
    }

    private void bindPreviewMeanings(
            LinearLayout previewContainer,
            boolean wordExists,
            List<String> meanings
    ) {
        previewContainer.removeAllViews();

        if (!wordExists) {
            return;
        }

        if (meanings.isEmpty()) {
            return;
        }

        for (String meaning : meanings) {
            addPreviewMeaningRow(previewContainer, meaning);
        }
    }

    private void addPreviewMeaningRow(LinearLayout container, String text) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dpToPx(8);
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setLineSpacing(0f, 1.15f);
        container.addView(textView);
    }

    private void showMeaningEditor(
            LinearLayout inputsContainer,
            List<String> meanings
    ) {
        inputsContainer.removeAllViews();
        List<String> safeMeanings = meanings.isEmpty() ? Collections.singletonList("") : meanings;
        for (String meaning : safeMeanings) {
            addMeaningInput(inputsContainer, meaning);
        }
    }

    private void addMeaningInput(LinearLayout container, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dpToPx(10);
        row.setLayoutParams(rowParams);

        TextInputLayout inputLayout = new TextInputLayout(this);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputLayout.setLayoutParams(inputParams);
        inputLayout.setHint("Significado");

        TextInputEditText input = new TextInputEditText(inputLayout.getContext());
        input.setText(value);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(3);
        inputLayout.addView(input);

        MaterialButton removeButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        removeParams.leftMargin = dpToPx(8);
        removeButton.setLayoutParams(removeParams);
        removeButton.setText("Quitar");
        removeButton.setAllCaps(false);
        removeButton.setOnClickListener(v -> {
            if (container.getChildCount() > 1) {
                container.removeView(row);
            } else {
                input.setText("");
            }
        });

        row.addView(inputLayout);
        row.addView(removeButton);
        container.addView(row);
    }

    private List<String> collectMeanings(LinearLayout container) {
        List<String> meanings = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            if (!(row instanceof LinearLayout)) {
                continue;
            }
            LinearLayout rowLayout = (LinearLayout) row;
            if (rowLayout.getChildCount() == 0 || !(rowLayout.getChildAt(0) instanceof TextInputLayout)) {
                continue;
            }
            TextInputLayout inputLayout = (TextInputLayout) rowLayout.getChildAt(0);
            CharSequence text = inputLayout.getEditText() != null ? inputLayout.getEditText().getText() : null;
            String value = text != null ? text.toString().trim() : "";
            if (!value.isEmpty()) {
                meanings.add(value);
            }
        }
        return meanings;
    }

    private List<String> parseMeanings(String rawMeaning) {
        List<String> meanings = new ArrayList<>();
        if (rawMeaning == null || rawMeaning.trim().isEmpty()) {
            return meanings;
        }

        String[] parts = rawMeaning.split("\\n+");
        for (String part : parts) {
            String clean = part.replaceFirst("^[-\\s]+", "").trim();
            if (!clean.isEmpty()) {
                meanings.add(clean);
            }
        }
        if (meanings.isEmpty()) {
            meanings.add(rawMeaning.trim());
        }
        return meanings;
    }

    private String serializeMeanings(List<String> meanings) {
        if (meanings.isEmpty()) {
            return "";
        }
        return String.join("\n", meanings);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private int resolveThemeColor(int attrRes) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }

    private String getStateLabel(Word.State state) {
        if (state == Word.State.LEARNING) {
            return "Aprendiendo";
        }
        if (state == Word.State.LEARNED) {
            return "Aprendida";
        }
        return "Nueva";
    }

    private void saveWordToDatabase(String wordKey, String meaning, Word.State state) {
        isSavingWord = true;
        boolean wasAlreadySaved = savedWordsState.containsKey(wordKey);

        savedWordsState.put(wordKey, state);
        savedWordsMeaning.put(wordKey, meaning);
        if (adapter != null) {
            adapter.updateWords(new HashMap<>(savedWordsState));
        }

        Word word = new Word();
        word.setWord(wordKey);
        word.setMeaning(meaning);
        word.setState(state);

        if (!wasAlreadySaved) {
            wordRepository.insertWord(word, (success, id) -> isSavingWord = false);
            return;
        }

        observeOnce(wordRepository.getWordByText(wordKey), existingWord -> {
            if (existingWord != null && existingWord.getId() != null) {
                existingWord.setMeaning(meaning);
                existingWord.setState(state);
                wordRepository.updateWord(existingWord, (success, id) -> isSavingWord = false);
            } else {
                wordRepository.insertWord(word, (success, id) -> isSavingWord = false);
            }
        });
    }

    private void deleteWordFromDatabase(String wordKey) {
        isSavingWord = true;
        savedWordsState.remove(wordKey);
        savedWordsMeaning.remove(wordKey);

        if (adapter != null) {
            adapter.updateWords(new HashMap<>(savedWordsState));
        }

        observeOnce(wordRepository.getWordByText(wordKey), word -> {
            if (word != null && word.getId() != null) {
                wordRepository.deleteWord(word.getId(), (success, id) -> isSavingWord = false);
            } else {
                isSavingWord = false;
            }
        });
    }

    private void persistReadingProgress(int pageIndex) {
        if (textId == -1 || pages.isEmpty()) {
            return;
        }

        int safePage = Math.max(0, Math.min(pageIndex, pages.size() - 1));
        int totalPages = pages.size();
        if (safePage == lastSavedPage && totalPages == lastSavedTotalPages) {
            return;
        }

        persistedPage = safePage;
        lastSavedPage = safePage;
        lastSavedTotalPages = totalPages;
        textRepository.updateReadingProgress(textId, safePage, totalPages, (success, id) -> { });
    }

    private <T> void observeOnce(LiveData<T> liveData, Observer<T> observer) {
        final Observer<T>[] observerRef = new Observer[1];
        observerRef[0] = value -> {
            liveData.removeObserver(observerRef[0]);
            observer.onChanged(value);
        };
        liveData.observe(this, observerRef[0]);
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

        private final List<String> pages;
        private final ReadingActivity activity;
        private final Map<String, Word.State> wordStates = new HashMap<>();
        private final Map<Integer, CharSequence> pageCache = new HashMap<>();
        private float textSize;

        PageAdapter(List<String> pages, float textSize, Map<String, Word.State> wordStates, ReadingActivity activity) {
            this.pages = pages;
            this.textSize = textSize;
            this.activity = activity;
            updateWords(wordStates);
        }

        public void setTextSize(float textSize) {
            this.textSize = textSize;
        }

        public void updateWords(Map<String, Word.State> wordStates) {
            this.wordStates.clear();
            this.wordStates.putAll(wordStates);
            pageCache.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.bind(getOrCreatePageSpan(position), textSize);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        private CharSequence getOrCreatePageSpan(int position) {
            CharSequence cached = pageCache.get(position);
            if (cached != null) {
                return cached;
            }

            CharSequence rendered = buildPageSpan(pages.get(position), wordStates, activity);
            pageCache.put(position, rendered);
            return rendered;
        }

        private static CharSequence buildPageSpan(String text, Map<String, Word.State> wordStates, ReadingActivity activity) {
            SpannableString spannable = new SpannableString(text);
            int textLength = text.length();
            int i = 0;

            while (i < textLength) {
                char c = text.charAt(i);
                if (!Character.isLetter(c)) {
                    i++;
                    continue;
                }

                int start = i;
                StringBuilder wordBuilder = new StringBuilder();
                while (i < textLength && Character.isLetter(text.charAt(i))) {
                    wordBuilder.append(text.charAt(i));
                    i++;
                }

                String originalWord = wordBuilder.toString();
                String cleanWord = WordTextUtils.normalizeWord(originalWord);
                if (cleanWord.isEmpty()) {
                    continue;
                }

                Word.State state = wordStates.get(cleanWord);
                int color = activity.getColor(R.color.word_new);
                if (state == Word.State.LEARNING) {
                    color = activity.getColor(R.color.word_learning);
                } else if (state == Word.State.LEARNED) {
                    color = activity.getColor(R.color.word_learned);
                }

                int finalColor = color;
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        activity.showWordDialog(originalWord, cleanWord);
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
            }

            return spannable;
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {

            private final TextView pageText;

            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                pageText = itemView.findViewById(R.id.pageText);
            }

            void bind(CharSequence text, float textSize) {
                pageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                pageText.setText(text);
                pageText.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
}
