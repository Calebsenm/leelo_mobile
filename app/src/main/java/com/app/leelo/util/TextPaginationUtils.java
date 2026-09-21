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
    private static final float DEFAULT_LINE_SPACING = 1.6f;
    private static final float DEFAULT_LINE_SPACING_EXTRA_DP = 2f;
    private static final int MAX_LAYOUT_CHARS = 24000;
    private static final int WORDS_PER_READING_PAGE = 200;

    public static class PageMetrics {
        public final int pageWidth;
        public final int pageHeight;
        public final TextPaint paint;
        public final float lineSpacingExtraPx;

        public PageMetrics(int pageWidth, int pageHeight, TextPaint paint) {
            this(pageWidth, pageHeight, paint, 0f);
        }

        public PageMetrics(int pageWidth, int pageHeight, TextPaint paint, float lineSpacingExtraPx) {
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
            this.paint = paint;
            this.lineSpacingExtraPx = lineSpacingExtraPx;
        }
    }

    public static PageMetrics calculatePageMetrics(DisplayMetrics displayMetrics) {
        return calculatePageMetrics(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.scaledDensity,
                displayMetrics.density,
                DEFAULT_TEXT_SIZE_SP
        );
    }

    public static PageMetrics calculatePageMetrics(int pageWidth, int pageHeight, float scaledDensity, float textSizeSp) {
        return calculatePageMetrics(pageWidth, pageHeight, scaledDensity,
                scaledDensity, textSizeSp);
    }

    public static PageMetrics calculatePageMetrics(
            int pageWidth,
            int pageHeight,
            float scaledDensity,
            float density,
            float textSizeSp
    ) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSizeSp * scaledDensity);
        paint.setTypeface(Typeface.DEFAULT);

        return new PageMetrics(
                Math.max(pageWidth, 1),
                Math.max(pageHeight, 1),
                paint,
                DEFAULT_LINE_SPACING_EXTRA_DP * density
        );
    }

    public static List<String> paginateText(String text, PageMetrics metrics) {
        return paginateByWords(text);
    }

    /**
     * Keeps page navigation useful while letting each page reflow naturally in
     * a vertical scroll container. A sentence boundary is preferred near the
     * target word count, but no text is ever discarded to achieve it.
     */
    private static List<String> paginateByWords(String text) {
        List<String> pages = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            pages.add("");
            return pages;
        }

        int pageStart = 0;
        int wordCount = 0;
        boolean insideWord = false;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                insideWord = true;
                continue;
            }

            if (!insideWord) {
                continue;
            }

            insideWord = false;
            wordCount++;
            if (wordCount < WORDS_PER_READING_PAGE) {
                continue;
            }

            int cut = chooseWordPageBreak(text, pageStart, index);
            String page = text.substring(pageStart, cut).trim();
            if (!page.isEmpty()) {
                pages.add(page);
            }
            pageStart = cut;
            while (pageStart < text.length() && Character.isWhitespace(text.charAt(pageStart))) {
                pageStart++;
            }
            index = Math.max(pageStart - 1, 0);
            wordCount = 0;
        }

        if (pageStart < text.length()) {
            String lastPage = text.substring(pageStart).trim();
            if (!lastPage.isEmpty()) {
                pages.add(lastPage);
            }
        }
        if (pages.isEmpty()) {
            pages.add(text.trim());
        }
        return pages;
    }

    private static int chooseWordPageBreak(String text, int pageStart, int wordEnd) {
        int minimumBreak = pageStart + Math.max(1, (int) ((wordEnd - pageStart) * 0.65f));
        for (int index = wordEnd - 1; index >= minimumBreak; index--) {
            char current = text.charAt(index);
            boolean sentenceEnd = current == '.' || current == '!' || current == '?'
                    || current == '\u2026';
            boolean paragraphEnd = current == '\n' && index + 1 < text.length()
                    && text.charAt(index + 1) == '\n';
            if (!sentenceEnd && !paragraphEnd) {
                continue;
            }
            if (sentenceEnd && index + 1 < text.length()
                    && !Character.isWhitespace(text.charAt(index + 1))) {
                continue;
            }
            return paragraphEnd ? index + 2 : index + 1;
        }
        return wordEnd;
    }

    private static List<String> paginateByLines(String text, PageMetrics metrics) {
        List<String> pages = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            pages.add("");
            return pages;
        }

        // Avoid creating one very large StaticLayout for an entire book. Apart from
        // reducing peak memory, this lets long imports paginate in predictable chunks.
        if (text.length() > MAX_LAYOUT_CHARS) {
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(text.length(), start + MAX_LAYOUT_CHARS);
                if (end < text.length()) {
                    int boundary = text.lastIndexOf(' ', end);
                    if (boundary > start + (MAX_LAYOUT_CHARS / 2)) {
                        end = boundary;
                    }
                }
                String chunk = text.substring(start, end);
                if (!chunk.trim().isEmpty()) {
                    pages.addAll(paginateText(chunk, metrics));
                }
                start = end;
                while (start < text.length()
                        && (text.charAt(start) == ' '
                        || text.charAt(start) == '\t'
                        || text.charAt(start) == '\r')) {
                    start++;
                }
            }
            if (pages.isEmpty()) {
                pages.add("");
            }
            return pages;
        }

        StaticLayout layout = StaticLayout.Builder
                .obtain(text, 0, text.length(), metrics.paint, metrics.pageWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setLineSpacing(metrics.lineSpacingExtraPx, DEFAULT_LINE_SPACING)
                .build();

        int startOffset = 0;
        int totalLines = layout.getLineCount();

        while (startOffset < text.length() && totalLines > 0) {
            int startLine = layout.getLineForOffset(Math.min(startOffset, text.length() - 1));
            while (startLine < totalLines && layout.getLineEnd(startLine) <= startOffset) {
                startLine++;
            }
            if (startLine >= totalLines) {
                break;
            }

            int endLine = findLastLineForPage(layout, startLine, metrics.pageHeight);

            int pageEnd = Math.max(startOffset + 1, layout.getLineEnd(endLine));
            int balancedEnd = findNaturalBreak(
                    text, layout, startOffset, pageEnd, startLine, metrics.pageHeight
            );
            if (balancedEnd <= startOffset) {
                balancedEnd = pageEnd;
            }

            String pageText = text.substring(startOffset, balancedEnd);
            if (!pageText.trim().isEmpty()) {
                pages.add(pageText);
            }

            // Advance by the actual character boundary, not only by a line number.
            // This prevents a sentence moved to the next page from being skipped.
            startOffset = balancedEnd;
            while (startOffset < text.length()
                    && (text.charAt(startOffset) == ' '
                    || text.charAt(startOffset) == '\t'
                    || text.charAt(startOffset) == '\r')) {
                startOffset++;
            }
        }

        // Safety net: pagination must never discard a final fragment after a
        // sentence-aware adjustment or a line-break boundary.
        if (startOffset < text.length()) {
            String remaining = text.substring(startOffset);
            if (!remaining.trim().isEmpty()) {
                pages.add(remaining);
            }
        }

        if (pages.isEmpty()) {
            pages.add(text);
        }

        return pages;
    }

    private static int findNaturalBreak(
            String text,
            StaticLayout layout,
            int startOffset,
            int candidateEnd,
            int startLine,
            int availableHeight
    ) {
        if (candidateEnd >= text.length()) {
            return candidateEnd;
        }

        // Prefer a sentence/paragraph boundary in the lower part of the page.
        // The minimum line count prevents very short pages.
        for (int i = candidateEnd - 1; i >= startOffset; i--) {
            char current = text.charAt(i);
            boolean sentenceEnd = current == '.' || current == '!' || current == '?'
                    || current == '\u2026';
            if (sentenceEnd && i + 1 < text.length()
                    && !Character.isWhitespace(text.charAt(i + 1))) {
                sentenceEnd = false;
            }
            boolean paragraphEnd = current == '\n' && i + 1 < text.length()
                    && text.charAt(i + 1) == '\n';
            if (!sentenceEnd && !paragraphEnd) {
                continue;
            }

            int boundary = sentenceEnd ? i + 1 : i + 2;
            if (boundary >= candidateEnd || boundary <= startOffset) {
                continue;
            }

            int boundaryLine = layout.getLineForOffset(Math.min(boundary, text.length() - 1));
            int usedHeight = layout.getLineTop(boundaryLine) - layout.getLineTop(startLine);
            if (boundaryLine >= startLine + 2
                    && usedHeight >= Math.round(availableHeight * 0.62f)) {
                return boundary;
            }
        }

        return candidateEnd;
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
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setLineSpacing(metrics.lineSpacingExtraPx, DEFAULT_LINE_SPACING)
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
