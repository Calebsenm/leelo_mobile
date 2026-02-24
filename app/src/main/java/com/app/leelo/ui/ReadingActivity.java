package com.app.leelo.ui;

import android.os.Bundle;
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
import com.app.leelo.utils.TextPaginationUtils;
import com.google.android.material.appbar.MaterialToolbar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        textRepository = TextRepository.RepositoryProvider.getInstance(this);

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
        // Ensure progress bar is visible for reading progress
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        // Keep progress bar visible after loading for reading progress
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
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // Remove title from toolbar
        }
        
        // Set title in the dedicated TextView
        if (textTitle != null && title != null) {
            textTitle.setText(title);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        adapter = new PageAdapter(pages);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageIndicator(position);
                // Hide title after first page
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

    // PageAdapter for ViewPager2
    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

        private final List<String> pages;

        public PageAdapter(List<String> pages) {
            this.pages = pages;
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
            holder.bind(pages.get(position));
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

            public void bind(String text) {
                pageText.setText(text);
            }
        }
    }
}