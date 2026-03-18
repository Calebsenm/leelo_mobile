package com.app.leelo.util;

import androidx.fragment.app.Fragment;

import com.app.leelo.ui.MainActivity;
import com.app.leelo.ui.TextFragment;

public final class FragmentUiUtils {

    private FragmentUiUtils() {
    }

    public static void postToUiIfAdded(Fragment fragment, Runnable action) {
        if (!fragment.isAdded()) {
            return;
        }

        fragment.requireActivity().runOnUiThread(() -> {
            if (fragment.isAdded()) {
                action.run();
            }
        });
    }

    public static void navigateToTexts(Fragment fragment) {
        if (fragment.getActivity() instanceof MainActivity) {
            ((MainActivity) fragment.getActivity()).replaceFragment(new TextFragment());
        }
    }
}
