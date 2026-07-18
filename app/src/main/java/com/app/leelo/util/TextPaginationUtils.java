package com.app.leelo.util;

import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.List;

public class TextPaginationUtils {

    private static final float DEFAULT_TEXT_SIZE_SP = 16;
    private static final float DEFAULT_LINE_SPACING = 1.4f;

    public static class PageMetrics {
        public final int pageWidth;
        public final int pageHeight;
        public final TextPaint paint;

        public PageMetrics(int pageWidth, int pageHeight, TextPaint paint) {
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
            this.paint = paint;
        }
    }

    public static PageMetrics calculatePageMetrics(DisplayMetrics displayMetrics) {
        return calculatePageMetrics(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.scaledDensity,
                DEFAULT_TEXT_SIZE_SP
        );
    }

    public static PageMetrics calculatePageMetrics(int pageWidth, int pageHeight, float scaledDensity, float textSizeSp) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSizeSp * scaledDensity);
        paint.setTypeface(Typeface.DEFAULT);

        return new PageMetrics(
                Math.max(pageWidth, 1),
                Math.max(pageHeight, 1),
                paint
        );
    }

    public static List<String> paginateText(String text, PageMetrics metrics) {
        List<String> pages = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            pages.add("");
            return pages;
        }

        StaticLayout layout = StaticLayout.Builder
                .obtain(text, 0, text.length(), metrics.paint, metrics.pageWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, DEFAULT_LINE_SPACING)
                .build();

        int startLine = 0;
        int totalLines = layout.getLineCount();

        while (startLine < totalLines) {
            int endLine = findLastLineForPage(layout, startLine, metrics.pageHeight);
            
            int pageStart = layout.getLineStart(startLine);
            int pageEnd = layout.getLineEnd(endLine);
            
            String pageText = text.substring(pageStart, pageEnd).trim();
            if (!pageText.isEmpty()) {
                pages.add(pageText);
            }
            
            startLine = endLine + 1;
        }

        if (pages.isEmpty()) {
            pages.add(text);
        }

        return pages;
    }

    private static int findLastLineForPage(StaticLayout layout, int startLine, int availableHeight) {
        int currentHeight = 0;
        int endLine = startLine;
        int totalLines = layout.getLineCount();

        while (endLine < totalLines && currentHeight < availableHeight) {
            int lineHeight = layout.getLineBottom(endLine) - layout.getLineTop(endLine);
            
            if (currentHeight + lineHeight > availableHeight) {
                break;
            }
            
            currentHeight += lineHeight;
            endLine++;
        }

        return Math.min(endLine, totalLines - 1);
    }

    public static int estimatePageCount(String text, PageMetrics metrics) {
        if (text == null || text.trim().isEmpty()) {
            return 1;
        }

        StaticLayout layout = StaticLayout.Builder
                .obtain(text, 0, text.length(), metrics.paint, metrics.pageWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, DEFAULT_LINE_SPACING)
                .build();

        int totalLines = layout.getLineCount();
        int linesPerPage = estimateLinesPerPage(layout, metrics.pageHeight);
        
        return (int) Math.ceil((double) totalLines / linesPerPage);
    }

    private static int estimateLinesPerPage(StaticLayout layout, int availableHeight) {
        int currentHeight = 0;
        int lineCount = 0;
        
        for (int i = 0; i < layout.getLineCount(); i++) {
            int lineHeight = layout.getLineBottom(i) - layout.getLineTop(i);
            
            if (currentHeight + lineHeight > availableHeight) {
                break;
            }
            
            currentHeight += lineHeight;
            lineCount++;
        }
        
        return Math.max(lineCount, 1);
    }
}
