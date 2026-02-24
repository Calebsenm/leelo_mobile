package com.app.leelo.ui;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.app.leelo.R;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.util.ReadingPreferences;
import com.app.leelo.utils.TextPaginationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import java.util.ArrayList;
import java.util.List;

public class ReadingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView pageIndicator;
    private TextView textTitle;
    private List<String> pages = new ArrayList<>();
    private PageAdapter adapter;
    private TextRepository textRepository;
    private long textId;
    private String title;
    private boolean isDataLoaded = false;
    private float currentTextSize = 16f;
    private TextView previewText;
    private ReadingPreferences readingPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        textRepository = TextRepository.RepositoryProvider.getInstance(this);
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
        setupViewPager();
        updatePageIndicator(0);
        isDataLoaded = true;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
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
            previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
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

    private void setupViewPager() {
        adapter = new PageAdapter(pages, currentTextSize);
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
    }

    private void updatePageIndicator(int position) {
        int currentPage = position + 1;
        int totalPages = pages.size();
        
        pageIndicator.setText("Page " + currentPage + "/" + totalPages);
        
        if (totalPages > 0) {
            int progress = (currentPage * 100) / totalPages;
            progressBar.setProgress(progress);
        }
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

        private final List<String> pages;
        private float textSize;

        public PageAdapter(List<String> pages, float textSize) {
            this.pages = pages;
            this.textSize = textSize;
        }

        public void setTextSize(float textSize) {
            this.textSize = textSize;
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
            holder.bind(pages.get(position), textSize);
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

            public void bind(String text, float textSize) {
                pageText.setText(text);
                pageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            }
        }
    }
}
