package com.app.leelo.presentation.viewmodel.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import com.app.leelo.R;
import com.app.leelo.util.ThemeManager;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingFragment extends Fragment {

    private ThemeManager themeManager;
    private MaterialSwitch darkModeSwitch;
    private ImageView themeIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        
        themeManager = ThemeManager.getInstance(requireContext());
        
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        themeIcon = view.findViewById(R.id.themeIcon);
        
        darkModeSwitch.setChecked(themeManager.isDarkMode());
        updateIcon(themeManager.isDarkMode());
        
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setDarkMode(isChecked);
            updateIcon(isChecked);
        });
        
        return view;
    }

    private void updateIcon(boolean isDark) {
        themeIcon.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
    }
}
