package com.app.leelo.presentation.ui;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.text.TextUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.app.leelo.R;
import com.app.leelo.domain.model.Word;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.domain.repository.WordRepository;
import com.app.leelo.util.ReadingPreferences;
import com.app.leelo.util.AppExecutors;
import com.app.leelo.util.TextPaginationUtils;
import com.app.leelo.util.ThemeManager;
import com.app.leelo.util.AiDictionaryClient;
import com.app.leelo.util.AiPreferences;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadingActivity extends AppCompatActivity {

    private static final int PAGE_CONTENT_PADDING_DP = 40;
    private ViewPager2 viewPager;
    private MaterialToolbar toolbar;
    private TextView pageIndicator;
    private TextView textTitle;
    private boolean isFocusMode = false;
    private boolean titleVisibleOnPage = false;
    private boolean useSerif = true;
    private long lastWordClickAt = 0L;
    private ThemeManager themeManager;
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
    private AiPreferences aiPrefs;
    private final Map<String, Word.State> savedWordsState = new HashMap<>();
    private final Map<String, String> savedWordsMeaning = new HashMap<>();
    private boolean isSavingWord = false;
    private String loadedContent;
    private boolean hasRestoredSavedPage = false;
    private int savedCurrentPage = 1;
    private int lastSavedPage = -1;
    private int lastSavedTotalPages = -1;
    private int lastPageWidth = -1;
    private int lastPageHeight = -1;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final AtomicInteger paginationGeneration = new AtomicInteger();
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        textRepository = TextRepository.RepositoryProvider.getInstance(this);
        wordRepository = WordRepository.RepositoryProvider.getInstance(this);
        readingPrefs = ReadingPreferences.getInstance(this);
        aiPrefs = AiPreferences.getInstance(this);
        themeManager = ThemeManager.getInstance(this);
        useSerif = readingPrefs.isSerif();
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
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
                getResources().getDisplayMetrics().density,
                currentTextSize,
                useSerif ? Typeface.SERIF : Typeface.SANS_SERIF
        );
        final int generation = paginationGeneration.incrementAndGet();

        // StaticLayout over a complete imported book is expensive. Keep it off the UI
        // thread so swipes and the word tooltip remain responsive.
        executors.diskIO().execute(() -> {
            List<String> paginatedPages = TextPaginationUtils.paginateText(fullText, metrics);
            runOnUiThread(() -> {
                if (generation != paginationGeneration.get() || !fullText.equals(loadedContent)) {
                    return;
                }

                pages.clear();
                pages.addAll(paginatedPages);

                if (adapter == null) {
                    adapter = new PageAdapter(pages, currentTextSize, useSerif, savedWordsState, this);
                    viewPager.setAdapter(adapter);

                    viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int position) {
                            updatePageIndicator(position);
                            updateReadingTitleLayout(position == 0);
                            persistReadingProgress(position);
                        }
                    });
                } else {
                    adapter.refreshContent(currentTextSize, useSerif);
                }

                restoreSavedPageIfNeeded();
                isDataLoaded = true;
            });
        });
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
        updateReadingTitleLayout(restoredPosition == 0);
        persistReadingProgress(restoredPosition);
    }

    private void updateReadingTitleLayout(boolean showTitle) {
        titleVisibleOnPage = showTitle;

        ConstraintLayout.LayoutParams params =
                (ConstraintLayout.LayoutParams) viewPager.getLayoutParams();
        if (showTitle) {
            params.topToTop = ConstraintLayout.LayoutParams.UNSET;
            params.topToBottom = R.id.textTitle;
            params.topMargin = dpToPx(12);
        } else {
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
            params.topMargin = 0;
        }
        viewPager.setLayoutParams(params);
        applyChromeVisibility();
    }

    private void updatePageIndicator(int position) {
        int currentPage = position + 1;
        int totalPages = pages.size();

        if (pageIndicator != null) {
            pageIndicator.setText(getString(R.string.page_indicator, currentPage, totalPages));
        }
    }

    /** A single tap in the middle of the page is the only focus-mode gesture. */
    private void handleReaderTap(float x, float y, View source) {
        if (System.currentTimeMillis() - lastWordClickAt < 500L) {
            return;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean inCenter = x > width * 0.25f && x < width * 0.75f
                && y > height * 0.25f && y < height * 0.75f;
        if (!inCenter) {
            return;
        }

        source.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        setFocusMode(!isFocusMode);
    }

    private void setFocusMode(boolean enabled) {
        if (isFocusMode == enabled) {
            return;
        }
        isFocusMode = enabled;

        int duration = shouldReduceMotion() ? 0 : 120;
        fadeView(toolbar, !enabled, duration);
        fadeView(pageIndicator, !enabled, duration);
        if (titleVisibleOnPage) {
            fadeView(textTitle, !enabled, duration);
        }
    }

    private void applyChromeVisibility() {
        boolean show = !isFocusMode;
        setViewVisibility(toolbar, show);
        setViewVisibility(pageIndicator, show);
        setViewVisibility(textTitle, show && titleVisibleOnPage);
    }

    private void setViewVisibility(View view, boolean visible) {
        if (view != null) {
            view.animate().cancel();
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
            view.setAlpha(1f);
        }
    }

    private void fadeView(View view, boolean visible, int duration) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        if (duration == 0) {
            setViewVisibility(view, visible);
            return;
        }
        if (visible) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1f).setDuration(duration).start();
        } else {
            view.animate().alpha(0f).setDuration(duration)
                    .withEndAction(() -> view.setVisibility(View.GONE)).start();
        }
    }

    private boolean shouldReduceMotion() {
        return Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
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
        // Keep only the adjacent page alive; each page contains many clickable spans.
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnLayoutChangeListener((view, left, top, right, bottom,
                                              oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            int height = bottom - top;
            if (width <= 0 || height <= 0 || (width == lastPageWidth && height == lastPageHeight)) {
                return;
            }
            lastPageWidth = width;
            lastPageHeight = height;
            if (loadedContent != null && isDataLoaded) {
                savedCurrentPage = viewPager.getCurrentItem() + 1;
                hasRestoredSavedPage = false;
                processTextInChunks(loadedContent);
            }
        });
        toolbar = findViewById(R.id.toolbar);
        pageIndicator = findViewById(R.id.pageIndicator);
        textTitle = findViewById(R.id.textTitle);
    }

    private void setupToolbar(String title) {
        if (toolbar.getMenu().size() == 0) {
            toolbar.inflateMenu(R.menu.reading_menu);
        }
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
        MaterialButton fontSerif = dialogView.findViewById(R.id.buttonFontSerif);
        MaterialButton fontSans = dialogView.findViewById(R.id.buttonFontSans);
        MaterialButton themeLight = dialogView.findViewById(R.id.buttonThemeLight);
        MaterialButton themeDark = dialogView.findViewById(R.id.buttonThemeDark);

        final float[] pendingTextSize = {currentTextSize};
        final boolean[] pendingSerif = {useSerif};
        final boolean[] pendingDark = {themeManager.isDarkMode()};

        fontSizeSlider.setValue(pendingTextSize[0]);
        updatePreview(previewText, pendingTextSize[0], pendingSerif[0]);
        markSelection(fontSerif, fontSans, pendingSerif[0]);
        markSelection(themeLight, themeDark, !pendingDark[0]);

        fontSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            pendingTextSize[0] = value;
            previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
        });
        fontSerif.setOnClickListener(v -> {
            pendingSerif[0] = true;
            markSelection(fontSerif, fontSans, true);
            updatePreview(previewText, pendingTextSize[0], true);
        });
        fontSans.setOnClickListener(v -> {
            pendingSerif[0] = false;
            markSelection(fontSerif, fontSans, false);
            updatePreview(previewText, pendingTextSize[0], false);
        });
        themeLight.setOnClickListener(v -> {
            pendingDark[0] = false;
            markSelection(themeLight, themeDark, true);
        });
        themeDark.setOnClickListener(v -> {
            pendingDark[0] = true;
            markSelection(themeLight, themeDark, false);
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Ajustes de lectura")
                .setView(dialogView)
                .setPositiveButton("Aplicar", (dialog, which) ->
                        applyReadingPreferences(pendingTextSize[0], pendingSerif[0], pendingDark[0]))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updatePreview(TextView preview, float textSize, boolean serif) {
        if (preview == null) {
            return;
        }
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        preview.setTypeface(serif ? Typeface.SERIF : Typeface.SANS_SERIF);
    }

    private void markSelection(MaterialButton first, MaterialButton second, boolean selectFirst) {
        MaterialButton selected = selectFirst ? first : second;
        MaterialButton other = selectFirst ? second : first;
        int accent = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary);
        int outline = resolveThemeColor(com.google.android.material.R.attr.colorOutline);
        int onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface);

        selected.setStrokeColor(ColorStateList.valueOf(accent));
        selected.setTextColor(accent);
        other.setStrokeColor(ColorStateList.valueOf(outline));
        other.setTextColor(onSurface);
    }

    private void applyReadingPreferences(float textSize, boolean serif, boolean dark) {
        currentTextSize = textSize;
        useSerif = serif;
        readingPrefs.setTextSize(textSize);
        readingPrefs.setSerif(serif);

        boolean themeChanged = themeManager.isDarkMode() != dark;
        if (themeChanged) {
            themeManager.setDarkMode(dark);
            recreate();
            return;
        }

        if (loadedContent != null) {
            hasRestoredSavedPage = false;
            processTextInChunks(loadedContent);
        } else if (adapter != null) {
            adapter.refreshContent(currentTextSize, useSerif);
        }
    }

    public void showWordDialog(String selectedWord, View anchorView) {
        showWordDialog(selectedWord, anchorView, null);
    }

    private void showWordDialog(String selectedWord, View anchorView, android.text.style.ClickableSpan clickedSpan) {
        String wordKey = normalizeWordKey(selectedWord);
        Word.State currentState = savedWordsState.get(wordKey);
        String currentMeaning = savedWordsMeaning.get(wordKey);
        boolean wordExists = currentState != null || !parseMeanings(currentMeaning).isEmpty();

        int[] anchorLocation = getWordAnchor(anchorView, clickedSpan);
        showWordPreviewPopup(
                selectedWord,
                wordKey,
                currentMeaning,
                currentState,
                wordExists,
                anchorLocation[0],
                anchorLocation[1],
                anchorLocation[2],
                anchorLocation[3]
        );
    }

    private int[] getWordAnchor(View view, android.text.style.ClickableSpan clickedSpan) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int width = view.getWidth();
        int height = view.getHeight();

        if (clickedSpan instanceof PageAdapter.WordClickableSpan && view instanceof TextView) {
            TextView textView = (TextView) view;
            android.text.Layout layout = textView.getLayout();
            android.text.Spannable text = (android.text.Spannable) textView.getText();
            int start = text.getSpanStart(clickedSpan);
            int end = text.getSpanEnd(clickedSpan);
            if (layout != null && start >= 0 && end > start) {
                int line = layout.getLineForOffset(start);
                int left = Math.round(Math.min(layout.getPrimaryHorizontal(start), layout.getPrimaryHorizontal(end)));
                int right = Math.round(Math.max(layout.getPrimaryHorizontal(start), layout.getPrimaryHorizontal(end)));
                location[0] += left;
                location[1] += layout.getLineTop(line);
                width = Math.max(1, right - left);
                height = layout.getLineBottom(line) - layout.getLineTop(line);
            }
        }
        return new int[]{location[0], location[1], width, height};
    }

    private void showWordPreviewPopup(
            String selectedWord,
            String wordKey,
            String currentMeaning,
            Word.State currentState,
            boolean wordExists,
            int anchorX,
            int anchorY,
            int anchorWidth,
            int anchorHeight
    ) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_word_preview, null);
        TextView previewWordText = popupView.findViewById(R.id.previewWordText);
        TextView previewEmptyText = popupView.findViewById(R.id.previewEmptyText);
        LinearLayout previewMeaningsContainer = popupView.findViewById(R.id.previewMeaningsContainer);
        LinearLayout wordStateActions = popupView.findViewById(R.id.wordStateActions);
        MaterialButton actionRemove = popupView.findViewById(R.id.actionRemove);
        MaterialButton actionQuickAdd = popupView.findViewById(R.id.actionQuickAdd);
        MaterialButton actionSpeak = popupView.findViewById(R.id.actionSpeak);
        MaterialButton actionLearning = popupView.findViewById(R.id.actionLearning);
        MaterialButton actionLearned = popupView.findViewById(R.id.actionLearned);
        MaterialButton previewActionButton = popupView.findViewById(R.id.previewActionButton);
        TextView previewDictionaryLink = popupView.findViewById(R.id.previewDictionaryLink);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                Math.min(dpToPx(320), getResources().getDisplayMetrics().widthPixels - dpToPx(32)),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setElevation(0f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        previewWordText.setText(selectedWord);
        previewDictionaryLink.setVisibility(aiPrefs.isAiMode() ? View.GONE : View.VISIBLE);

        List<String> meanings = parseMeanings(currentMeaning);
        List<String> resolvedMeanings = new ArrayList<>(meanings);
        if (wordExists && !meanings.isEmpty()) {
            previewEmptyText.setVisibility(View.GONE);
            previewMeaningsContainer.setVisibility(View.VISIBLE);
            bindMeaningsPreview(previewMeaningsContainer, meanings);
            previewActionButton.setText("Editar");
        } else {
            previewEmptyText.setVisibility(View.VISIBLE);
            previewMeaningsContainer.setVisibility(View.GONE);
            previewActionButton.setText("Agregar significado");
            if (aiPrefs.isAiMode()) {
                previewEmptyText.setText(aiPrefs.getApiKey().isEmpty()
                        ? "Añade tu clave IA en Ajustes."
                        : "Buscando con IA…");
            } else {
                previewEmptyText.setText("Sin significado guardado.");
            }
        }

        actionQuickAdd.setVisibility(wordExists ? View.GONE : View.VISIBLE);
        wordStateActions.setVisibility(wordExists ? View.VISIBLE : View.GONE);
        actionSpeak.setOnClickListener(v -> speakWord(selectedWord));
        actionQuickAdd.setOnClickListener(v -> {
            saveWordToDatabase(wordKey, TextUtils.join("\n", resolvedMeanings), Word.State.NEW);
            popupWindow.dismiss();
        });

        actionRemove.setOnClickListener(v -> {
            deleteWordFromDatabase(wordKey);
            popupWindow.dismiss();
        });

        actionLearning.setOnClickListener(v -> {
            markButtonSelected(actionLearning, R.color.accent);
            markButtonDeselected(actionRemove);
            markButtonDeselected(actionLearned);
            saveWordToDatabase(wordKey, currentMeaning, Word.State.LEARNING);
        });

        actionLearned.setOnClickListener(v -> {
            markButtonSelected(actionLearned, R.color.state_learned);
            markButtonDeselected(actionRemove);
            markButtonDeselected(actionLearning);
            saveWordToDatabase(wordKey, currentMeaning, Word.State.LEARNED);
        });

        if (currentState == Word.State.LEARNING) {
            markButtonSelected(actionLearning, R.color.accent);
        } else if (currentState == Word.State.LEARNED) {
            markButtonSelected(actionLearned, R.color.state_learned);
        }

        previewActionButton.setOnClickListener(v -> {
            popupWindow.dismiss();
            Word.State latestState = savedWordsState.get(wordKey);
            String latestMeaning = savedWordsMeaning.get(wordKey);
            if ((latestMeaning == null || latestMeaning.trim().isEmpty()) && !resolvedMeanings.isEmpty()) {
                latestMeaning = TextUtils.join("\n", resolvedMeanings);
            }
            boolean isWordSaved = savedWordsState.containsKey(wordKey)
                    || !parseMeanings(savedWordsMeaning.get(wordKey)).isEmpty();
            showWordEditorSheet(selectedWord, wordKey, latestMeaning, latestState, isWordSaved);
        });

        previewDictionaryLink.setOnClickListener(v -> {
            popupWindow.dismiss();
            openDictionary(selectedWord);
        });

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(popupWindow.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        View rootView = getWindow().getDecorView();
        Rect visibleFrame = new Rect();
        rootView.getWindowVisibleDisplayFrame(visibleFrame);
        int screenWidth = visibleFrame.width();
        int screenHeight = visibleFrame.height();
        int popupWidth = popupWindow.getWidth();
        int popupHeight = popupView.getMeasuredHeight();
        int screenMargin = dpToPx(16);

        int x = anchorX + (anchorWidth - popupWidth) / 2;
        x = Math.max(screenMargin, Math.min(x, screenWidth - popupWidth - screenMargin));

        int preferredAboveY = anchorY - popupHeight - dpToPx(12);
        int fallbackBelowY = anchorY + anchorHeight + dpToPx(12);
        int y = preferredAboveY >= screenMargin
                ? preferredAboveY
                : Math.min(fallbackBelowY, screenHeight - popupHeight - screenMargin);

        popupWindow.showAtLocation(rootView, Gravity.TOP | Gravity.START,
                x + visibleFrame.left, y + visibleFrame.top);

        if (!wordExists && aiPrefs.isAiMode() && !aiPrefs.getApiKey().isEmpty()) {
            String apiKey = aiPrefs.getApiKey();
            String model = aiPrefs.getModel();
            executors.diskIO().execute(() -> AiDictionaryClient.fetch(
                    selectedWord, apiKey, model, aiPrefs.getEndpoint(), result ->
                    runOnUiThread(() -> {
                        if (!popupWindow.isShowing()) {
                            return;
                        }
                        if (result == null) {
                            previewEmptyText.setText("No se pudo consultar la IA. Puedes agregar el significado.");
                            return;
                        }
                        resolvedMeanings.clear();
                        resolvedMeanings.add(result.translationEs);
                        if (!result.context.trim().isEmpty()) {
                            resolvedMeanings.add(result.context);
                        }
                        previewEmptyText.setVisibility(View.GONE);
                        previewMeaningsContainer.setVisibility(View.VISIBLE);
                        bindAiPreview(previewMeaningsContainer, result);
                    })));
        }
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
        int color = ContextCompat.getColor(this, colorRes);
        button.setBackgroundTintList(ColorStateList.valueOf(
                android.graphics.Color.TRANSPARENT));
        button.setStrokeColor(ColorStateList.valueOf(color));
        button.setTextColor(color);
        button.setStrokeWidth(dpToPx(1));
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
        int visibleMeanings = Math.min(3, meanings.size());
        for (int index = 0; index < visibleMeanings; index++) {
            String meaning = meanings.get(index);
            TextView meaningView = new TextView(this);
            meaningView.setText(meaning);
            meaningView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            meaningView.setTextColor(resolveThemeColor());
            meaningView.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
            GradientDrawable meaningBackground = new GradientDrawable();
            meaningBackground.setColor(resolveThemeColor(com.google.android.material.R.attr.colorSurface));
            meaningBackground.setCornerRadius(dpToPx(6));
            meaningBackground.setStroke(dpToPx(1), resolveThemeColor(com.google.android.material.R.attr.colorOutline));
            meaningView.setBackground(meaningBackground);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dpToPx(3);
            meaningView.setLayoutParams(params);
            container.addView(meaningView);
        }
    }

    /** Only reachable from Manual mode: opens a real dictionary without leaving the app feel. */
    private void openDictionary(String word) {
        String url = "https://www.wordreference.com/enes/" + Uri.encode(word);
        CustomTabsIntent tabs = new CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .setToolbarColor(resolveThemeColor(com.google.android.material.R.attr.colorSurface))
                .build();
        tabs.launchUrl(this, Uri.parse(url));
    }

    private void bindAiPreview(LinearLayout container, AiDictionaryClient.Result result) {
        container.removeAllViews();
        addAiLine(container, "En inglés", result.meaningEn);
        addAiLine(container, "En español", result.translationEs);
        addAiLine(container, "Contexto", result.context);
    }

    private void addAiLine(LinearLayout container, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        String text = label + "\n" + value;
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),
                0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView view = new TextView(this);
        view.setText(spannable);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setTextColor(resolveThemeColor());
        view.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        GradientDrawable background = new GradientDrawable();
        background.setColor(resolveThemeColor(com.google.android.material.R.attr.colorSurface));
        background.setCornerRadius(dpToPx(6));
        background.setStroke(dpToPx(1), resolveThemeColor(com.google.android.material.R.attr.colorOutline));
        view.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dpToPx(3);
        view.setLayoutParams(params);
        container.addView(view);
    }

    private int resolveThemeColor() {
        return resolveThemeColor(com.google.android.material.R.attr.colorOnSurface);
    }

    private int resolveThemeColor(int attribute) {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
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

    private void speakWord(String word) {
        if (textToSpeech != null && word != null && !word.trim().isEmpty()) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, "leelo-word");
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

        private final List<String> pages;
        private final Map<String, Word.State> wordStates = new HashMap<>();
        private final ReadingActivity activity;
        private final LruCache<Integer, CharSequence> styledPageCache;
        private float textSize;
        private boolean useSerif;

        public PageAdapter(List<String> pages, float textSize, boolean useSerif,
                           Map<String, Word.State> wordStates, ReadingActivity activity) {
            this.pages = pages;
            this.textSize = textSize;
            this.useSerif = useSerif;
            this.activity = activity;
            this.wordStates.putAll(wordStates);
            this.styledPageCache = new LruCache<>(Math.max(6, Math.min(pages.size(), 30)));
            setHasStableIds(true);
        }

        public void setTextSize(float textSize) {
            this.textSize = textSize;
            notifyVisiblePagesChanged("text_size");
        }

        public void refreshContent(float textSize, boolean useSerif) {
            this.textSize = textSize;
            this.useSerif = useSerif;
            styledPageCache.evictAll();
            notifyDataSetChanged();
        }

        public void updateWords(Map<String, Word.State> wordStates) {
            this.wordStates.clear();
            this.wordStates.putAll(wordStates);
            styledPageCache.evictAll();
            notifyVisiblePagesChanged("word_state");
        }

        private void notifyVisiblePagesChanged(String payload) {
            int center = activity.viewPager.getCurrentItem();
            for (int position = Math.max(0, center - 1);
                    position <= Math.min(getItemCount() - 1, center + 1); position++) {
                notifyItemChanged(position, payload);
            }
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
            return new PageViewHolder(view, activity);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.bind(getStyledPage(position), textSize, useSerif);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() == 1 && "text_size".equals(payloads.get(0))) {
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
                spannable.setSpan(
                        new WordClickableSpan(activity, cleanWord, state),
                        start,
                        index,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            return spannable;
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            private final TextView pageText;
            private final ReadingActivity activity;

            public PageViewHolder(@NonNull View itemView, ReadingActivity activity) {
                super(itemView);
                this.activity = activity;
                pageText = itemView.findViewById(R.id.pageText);
                pageText.setMovementMethod(LinkMovementMethod.getInstance());

                GestureDetector focusDetector = new GestureDetector(itemView.getContext(),
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapConfirmed(MotionEvent event) {
                                activity.handleReaderTap(event.getX(), event.getY(), pageText);
                                return false;
                            }
                        });
                pageText.setOnTouchListener((view, event) -> {
                    focusDetector.onTouchEvent(event);
                    return false;
                });

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pageText.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
                }
            }

            public void bind(CharSequence text, float textSize, boolean useSerif) {
                pageText.setTypeface(useSerif ? Typeface.SERIF : Typeface.SANS_SERIF);
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
            private final Word.State state;

            private WordClickableSpan(ReadingActivity activity, String word, Word.State state) {
                this.activity = activity;
                this.word = word;
                this.state = state;
            }

            @Override
            public void onClick(@NonNull View widget) {
                activity.lastWordClickAt = System.currentTimeMillis();
                activity.showWordDialog(word, widget, this);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                android.util.TypedValue tv = new android.util.TypedValue();
                activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
                ds.setColor(tv.data);
                if (state == null || state == Word.State.LEARNING) {
                    ds.setUnderlineText(true);
                }
            }
        }
    }
}
