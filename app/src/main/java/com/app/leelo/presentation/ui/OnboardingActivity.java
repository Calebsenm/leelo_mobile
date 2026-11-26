package com.app.leelo.presentation.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.app.leelo.R;
import com.app.leelo.util.OnboardingPreferences;
import com.app.leelo.util.ThemeManager;
import com.google.android.material.button.MaterialButton;

/** First-run introduction: three quiet screens with short, restrained transitions. */
public class OnboardingActivity extends AppCompatActivity {

    private static final int[] PAGES = {
            R.layout.onboarding_page_read,
            R.layout.onboarding_page_modes,
            R.layout.onboarding_page_generate
    };

    private FrameLayout pageContainer;
    private MaterialButton nextButton;
    private View[] dots;
    private int current = 0;
    private boolean animating = false;
    private long duration = 130L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);

        if (OnboardingPreferences.isDone(this)) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        duration = Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ? 0L : 130L;

        pageContainer = findViewById(R.id.pageContainer);
        nextButton = findViewById(R.id.nextButton);
        dots = new View[]{findViewById(R.id.dot1), findViewById(R.id.dot2), findViewById(R.id.dot3)};

        nextButton.setOnClickListener(v -> {
            if (current == PAGES.length - 1) {
                finishOnboarding();
            } else {
                showPage(current + 1, true);
            }
        });
        findViewById(R.id.skipButton).setOnClickListener(v -> finishOnboarding());

        showPage(0, false);
    }

    private void showPage(int index, boolean animate) {
        View page = LayoutInflater.from(this).inflate(PAGES[index], pageContainer, false);
        View oldPage = pageContainer.getChildAt(0);

        if (!animate || oldPage == null || duration == 0L) {
            pageContainer.removeAllViews();
            pageContainer.addView(page);
            animateVisual(page);
            updateDots(index, false);
            updateLabel(index);
            current = index;
            return;
        }

        if (animating) {
            return;
        }
        animating = true;
        page.setAlpha(0f);
        page.setTranslationX(24f);
        pageContainer.addView(page);
        animateVisual(page);

        oldPage.animate()
                .alpha(0f)
                .translationX(-24f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        pageContainer.removeView(oldPage);
                    }
                })
                .start();

        page.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animating = false;
                    }
                })
                .start();

        updateDots(index, true);
        updateLabel(index);
        current = index;
    }

    private void animateVisual(View page) {
        View visual = page.findViewById(R.id.pageVisual);
        if (visual == null || duration == 0L) {
            return;
        }
        visual.setAlpha(0f);
        visual.setTranslationY(8f);
        visual.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(40L)
                .setDuration(duration + 20L)
                .start();
    }

    private void updateDots(int index, boolean animate) {
        for (int i = 0; i < dots.length; i++) {
            View dot = dots[i];
            boolean selected = i == index;
            dot.setBackgroundResource(selected
                    ? R.drawable.onboarding_dot_selected : R.drawable.onboarding_dot);
            ViewGroup.LayoutParams params = dot.getLayoutParams();
            params.width = dp(selected ? 18 : 6);
            dot.setLayoutParams(params);
            if (animate && duration > 0L) {
                dot.setScaleX(0.8f);
                dot.animate().scaleX(1f).setDuration(duration).start();
            }
        }
    }

    private void updateLabel(int index) {
        nextButton.setText(index == PAGES.length - 1 ? "Empezar" : "Siguiente");
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private void finishOnboarding() {
        OnboardingPreferences.setDone(this);
        openMain();
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        if (duration > 0L) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public void onBackPressed() {
        if (current > 0 && !animating) {
            showPage(current - 1, true);
        } else {
            super.onBackPressed();
        }
    }
}
