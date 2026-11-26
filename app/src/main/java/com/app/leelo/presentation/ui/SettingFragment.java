package com.app.leelo.presentation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.app.leelo.R;
import com.app.leelo.util.AiDictionaryClient;
import com.app.leelo.util.AiPreferences;
import com.app.leelo.util.AiProviders;
import com.app.leelo.util.AppExecutors;
import com.app.leelo.util.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends Fragment {

    private ThemeManager themeManager;
    private AiPreferences aiPrefs;
    private MaterialSwitch darkModeSwitch;
    private ImageView themeIcon;
    private RadioGroup modeRadioGroup;
    private View aiSettingsContainer;
    private Spinner providerSpinner;
    private Spinner modelSpinner;
    private View endpointInputLayout;
    private TextInputEditText endpointInput;
    private TextInputEditText modelInput;
    private TextInputEditText apiKeyInput;
    private TextView modelHintText;
    private TextView aiStatusText;
    private MaterialButton testConnectionButton;
    private MaterialButton clearApiKeyButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        themeManager = ThemeManager.getInstance(requireContext());
        aiPrefs = AiPreferences.getInstance(requireContext());
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        themeIcon = view.findViewById(R.id.themeIcon);
        modeRadioGroup = view.findViewById(R.id.modeRadioGroup);
        aiSettingsContainer = view.findViewById(R.id.aiSettingsContainer);
        providerSpinner = view.findViewById(R.id.providerSpinner);
        modelSpinner = view.findViewById(R.id.modelSpinner);
        endpointInputLayout = view.findViewById(R.id.endpointInputLayout);
        endpointInput = view.findViewById(R.id.endpointInput);
        modelInput = view.findViewById(R.id.modelInput);
        apiKeyInput = view.findViewById(R.id.apiKeyInput);
        modelHintText = view.findViewById(R.id.modelHintText);
        aiStatusText = view.findViewById(R.id.aiStatusText);
        testConnectionButton = view.findViewById(R.id.testConnectionButton);
        clearApiKeyButton = view.findViewById(R.id.clearApiKeyButton);

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

        List<String> labels = new ArrayList<>();
        for (AiProviders.Preset preset : AiProviders.PRESETS) {
            labels.add(preset.label);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, labels);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        providerSpinner.setAdapter(adapter);
        providerSpinner.setSelection(AiProviders.indexForEndpoint(aiPrefs.getEndpoint()));
        applyPreset(providerSpinner.getSelectedItemPosition(), text(modelInput));

        providerSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View itemView, int position, long id) {
                applyPreset(position, AiProviders.PRESETS[position].model);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        view.<MaterialButton>findViewById(R.id.saveAiSettingsButton)
                .setOnClickListener(v -> saveAiSettings());
        testConnectionButton.setOnClickListener(v -> testConnection());
        clearApiKeyButton.setOnClickListener(v -> {
            aiPrefs.clearApiKey();
            apiKeyInput.setText("");
            updateAiStatus();
        });

        updateAiStatus();
        return view;
    }

    /** Known providers fill in endpoint and model automatically; only Personalizado asks for them. */
    private void applyPreset(int position, String preferredModel) {
        AiProviders.Preset preset = AiProviders.PRESETS[position];
        boolean custom = position == AiProviders.CUSTOM_INDEX;
        endpointInputLayout.setVisibility(custom ? View.VISIBLE : View.GONE);
        if (!custom) {
            endpointInput.setText(preset.endpoint);
            modelInput.setText(preferredModel);
        }
        modelHintText.setText(preset.hint);
        setupModelSpinner(preset, preferredModel);
    }

    private void setupModelSpinner(AiProviders.Preset preset, String selectedModel) {
        List<String> models = new ArrayList<>();
        for (String model : preset.models) {
            models.add(model);
        }
        models.add("Otro (escribir ID)");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, models);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        modelSpinner.setOnItemSelectedListener(null);
        modelSpinner.setAdapter(adapter);

        int selection = models.size() - 1;
        for (int i = 0; i < preset.models.length; i++) {
            if (preset.models[i].equals(selectedModel)) {
                selection = i;
                break;
            }
        }
        modelSpinner.setSelection(selection);

        modelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View itemView, int position, long id) {
                if (position < preset.models.length) {
                    modelInput.setText(preset.models[position]);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private String selectedEndpoint() {
        if (providerSpinner.getSelectedItemPosition() == AiProviders.CUSTOM_INDEX) {
            return text(endpointInput);
        }
        return AiProviders.PRESETS[providerSpinner.getSelectedItemPosition()].endpoint;
    }

    private void saveAiSettings() {
        String endpoint = selectedEndpoint();
        String model = text(modelInput);
        String key = text(apiKeyInput);

        if (endpoint.isEmpty()) {
            endpointInput.setError("Indica el endpoint");
            return;
        }
        if (model.isEmpty()) {
            modelInput.setError("Indica un modelo");
            return;
        }

        aiPrefs.setEndpoint(endpoint);
        aiPrefs.setModel(model);
        aiPrefs.setApiKey(key);
        updateAiStatus();
    }

    private void testConnection() {
        String key = text(apiKeyInput);
        if (key.isEmpty()) {
            aiStatusText.setText("Pega tu clave API antes de probar.");
            return;
        }
        String model = text(modelInput);
        if (model.isEmpty()) {
            modelInput.setError("Indica un modelo");
            return;
        }

        String endpoint = selectedEndpoint();
        if (endpoint.isEmpty()) {
            endpointInput.setError("Indica el endpoint");
            return;
        }

        aiStatusText.setText("Probando conexión…");
        testConnectionButton.setEnabled(false);
        AppExecutors.getInstance().diskIO().execute(() ->
                AiDictionaryClient.test(key, model, endpoint, (ok, message) -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            aiStatusText.setText(message);
                            testConnectionButton.setEnabled(true);
                        });
                    }
                }));
    }

    private void updateAiStatus() {
        if (aiPrefs.hasApiKey()) {
            aiStatusText.setText("Clave guardada y cifrada en este dispositivo.");
        } else if (aiPrefs.isAiMode()) {
            aiStatusText.setText("Modo IA activo. Pega tu clave y pulsa Guardar.");
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
