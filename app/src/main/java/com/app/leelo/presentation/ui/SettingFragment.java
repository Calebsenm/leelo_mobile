package com.app.leelo.presentation.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.fragment.app.Fragment;

import com.app.leelo.R;
import com.app.leelo.util.AiPreferences;
import com.app.leelo.util.ThemeManager;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.function.Consumer;

public class SettingFragment extends Fragment {

    private ThemeManager themeManager;
    private AiPreferences aiPrefs;
    private MaterialSwitch darkModeSwitch;
    private ImageView themeIcon;
    private RadioGroup modeRadioGroup;
    private View aiSettingsContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        themeManager = ThemeManager.getInstance(requireContext());
        aiPrefs = AiPreferences.getInstance(requireContext());
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        themeIcon = view.findViewById(R.id.themeIcon);
        modeRadioGroup = view.findViewById(R.id.modeRadioGroup);
        aiSettingsContainer = view.findViewById(R.id.aiSettingsContainer);

        darkModeSwitch.setChecked(themeManager.isDarkMode());
        updateIcon(themeManager.isDarkMode());
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setDarkMode(isChecked);
            updateIcon(isChecked);
        });

        boolean aiMode = aiPrefs.isAiMode();
        RadioButton selected = view.findViewById(aiMode ? R.id.radioAiMode : R.id.radioManualMode);
        selected.setChecked(true);
        aiSettingsContainer.setVisibility(aiMode ? View.VISIBLE : View.GONE);

        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isAi = checkedId == R.id.radioAiMode;
            aiPrefs.setAiMode(isAi);
            aiSettingsContainer.setVisibility(isAi ? View.VISIBLE : View.GONE);
        });

        TextInputEditText apiKeyInput = view.findViewById(R.id.apiKeyInput);
        TextInputEditText modelInput = view.findViewById(R.id.modelInput);
        apiKeyInput.setText(aiPrefs.getApiKey());
        modelInput.setText(aiPrefs.getModel());
        autoSave(apiKeyInput, aiPrefs::setApiKey);
        autoSave(modelInput, aiPrefs::setModel);

        return view;
    }

    private void autoSave(EditText input, Consumer<String> saver) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                saver.accept(s.toString());
            }
        });
    }

    private void updateIcon(boolean isDark) {
        themeIcon.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
    }
}
