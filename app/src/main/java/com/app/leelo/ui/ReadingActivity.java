package com.app.leelo.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        // Get text data from intent
        String fullText = getIntent().getStringExtra("text");
        String title = getIntent().getStringExtra("title");
        
        if (fullText == null) fullText = "This is sample text for reading. ".repeat(50);

        initViews();
        setupToolbar(title);
        divideTextIntoPages(fullText);
        setupViewPager();
        updatePageIndicator(0);
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

    private void divideTextIntoPages(String fullText) {
        // Calculate page dimensions (subtracting padding)
        int pageWidth = getResources().getDisplayMetrics().widthPixels - 
                       (int) (32 * getResources().getDisplayMetrics().density); // 16dp each side
        int pageHeight = getResources().getDisplayMetrics().heightPixels - 
                        (int) (120 * getResources().getDisplayMetrics().density); // toolbar + padding

        TextPaint paint = new TextPaint();
        paint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity); // 16sp
        paint.setTypeface(Typeface.DEFAULT);

        int availableHeight = pageHeight;

        // Use StaticLayout to measure text
        StaticLayout layout = StaticLayout.Builder
                .obtain(fullText, 0, fullText.length(), paint, pageWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, 1.2f) // 1.2 line spacing
                .build();

        int start = 0;
        int totalLines = layout.getLineCount();

        while (start < totalLines) {
            int endLine = start;
            int currentHeight = 0;

            // Find how many lines fit in this page
            while (endLine < totalLines && currentHeight < availableHeight) {
                currentHeight += layout.getLineBottom(endLine) - layout.getLineTop(endLine);
                if (currentHeight >= availableHeight) {
                    endLine--; // Remove the line that overflowed
                    break;
                }
                endLine++;
            }

            if (endLine >= totalLines) {
                endLine = totalLines - 1;
            }

            int pageStart = layout.getLineStart(start);
            int pageEnd = layout.getLineEnd(endLine);

            String pageText = fullText.substring(pageStart, pageEnd).trim();
            pages.add(pageText);

            start = endLine + 1;
        }

        if (pages.isEmpty()) {
            pages.add(fullText);
        }
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