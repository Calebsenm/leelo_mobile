package com.app.leelo.presentation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.app.leelo.R;
import com.app.leelo.util.AiPreferences;
import com.app.leelo.util.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

public class SettingFragment extends Fragment {

    private ThemeManager themeManager;
    private AiPreferences aiPrefs;
    private MaterialSwitch darkModeSwitch;
    private ImageView themeIcon;
    private RadioGroup modeRadioGroup;
    private View aiSettingsContainer;
    private TextInputEditText endpointInput;
    private TextInputEditText modelInput;
    private TextInputEditText apiKeyInput;
    private MaterialButton clearApiKeyButton;
    private TextView aiStatusText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        themeManager = ThemeManager.getInstance(requireContext());
        aiPrefs = AiPreferences.getInstance(requireContext());
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        themeIcon = view.findViewById(R.id.themeIcon);
        modeRadioGroup = view.findViewById(R.id.modeRadioGroup);
        aiSettingsContainer = view.findViewById(R.id.aiSettingsContainer);
        endpointInput = view.findViewById(R.id.endpointInput);
        modelInput = view.findViewById(R.id.modelInput);
        apiKeyInput = view.findViewById(R.id.apiKeyInput);
        clearApiKeyButton = view.findViewById(R.id.clearApiKeyButton);
        aiStatusText = view.findViewById(R.id.aiStatusText);

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
            updateAiStatus();
        });

        endpointInput.setText(aiPrefs.getEndpoint());
        modelInput.setText(aiPrefs.getModel());
        apiKeyInput.setText(aiPrefs.getApiKey());

        view.<MaterialButton>findViewById(R.id.saveAiSettingsButton)
                .setOnClickListener(v -> saveAiSettings());
        clearApiKeyButton.setOnClickListener(v -> {
            aiPrefs.clearApiKey();
            apiKeyInput.setText("");
            updateAiStatus();
        });

        updateAiStatus();
        return view;
    }

    private void saveAiSettings() {
        String model = text(modelInput);
        if (model.isEmpty()) {
            modelInput.setError("Indica un modelo");
            return;
        }
        aiPrefs.setEndpoint(text(endpointInput));
        aiPrefs.setModel(model);
        aiPrefs.setApiKey(text(apiKeyInput));
        updateAiStatus();
    }

    private void updateAiStatus() {
        if (aiPrefs.hasApiKey()) {
            aiStatusText.setText("Clave guardada y cifrada en este dispositivo.");
        } else if (aiPrefs.isAiMode()) {
            aiStatusText.setText("Modo IA activo. Añade tu clave y pulsa Guardar.");
        } else {
            aiStatusText.setText("Sin clave guardada. Puedes añadirla cuando actives el modo IA.");
        }
        clearApiKeyButton.setEnabled(aiPrefs.hasApiKey());
    }

    private static String text(TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }

    private void updateIcon(boolean isDark) {
        themeIcon.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
    }
}
