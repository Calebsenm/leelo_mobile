package com.app.leelo.presentation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.app.leelo.R;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.domain.model.Text;
import com.app.leelo.presentation.viewmodel.TextViewModel;
import com.app.leelo.presentation.viewmodel.ViewModelFactory;
import com.app.leelo.util.AiPreferences;
import com.app.leelo.util.AiTextGenerator;
import com.app.leelo.util.AppExecutors;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AddTextFragment extends Fragment {

    private static final String[] LEVELS = {
            "A1 - Principiante", "A2 - Elemental", "B1 - Intermedio",
            "B2 - Intermedio alto", "C1 - Avanzado"
    };
    private static final int[] WORD_COUNTS = {5, 8, 12};

    private EditText titleEditText, contentEditText, topicInput;
    private Button saveButton;
    private Spinner levelSpinner, wordCountSpinner;
    private MaterialSwitch examplesSwitch;
    private MaterialButton generateTextButton;
    private MaterialButton showGenerateButton;
    private TextView generationStatusText;
    private View aiGenerateContainer;
    private boolean isEditMode = false;
    private long editId = -1;
    private TextViewModel viewModel;
    private AiPreferences aiPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextRepository repo = TextRepository.RepositoryProvider.getInstance(requireContext());
        viewModel = new ViewModelProvider(this, new ViewModelFactory(repo))
                .get(TextViewModel.class);
        aiPrefs = AiPreferences.getInstance(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_text, container, false);

        titleEditText = view.findViewById(R.id.inputTittle);
        contentEditText = view.findViewById(R.id.inputText);
        saveButton = view.findViewById(R.id.save_text_button);
        aiGenerateContainer = view.findViewById(R.id.aiGenerateContainer);
        topicInput = view.findViewById(R.id.topicInput);
        levelSpinner = view.findViewById(R.id.levelSpinner);
        wordCountSpinner = view.findViewById(R.id.wordCountSpinner);
        examplesSwitch = view.findViewById(R.id.examplesSwitch);
        generateTextButton = view.findViewById(R.id.generateTextButton);
        showGenerateButton = view.findViewById(R.id.showGenerateButton);
        generationStatusText = view.findViewById(R.id.generationStatusText);

        Bundle args = getArguments();
        if (args != null && args.containsKey("id")) {
            isEditMode = true;
            editId = args.getLong("id", -1);
            loadTextForEditing(editId);
        } else if (args != null && args.getBoolean("generateAi", false)) {
            aiGenerateContainer.setVisibility(View.VISIBLE);
            setupGenerationControls();
        }

        if (isEditMode) {
            showGenerateButton.setVisibility(View.GONE);
        } else if (levelSpinner.getAdapter() == null) {
            setupGenerationControls();
        }
        showGenerateButton.setOnClickListener(v -> {
            aiGenerateContainer.setVisibility(View.VISIBLE);
            topicInput.requestFocus();
        });
        generateTextButton.setOnClickListener(v -> generateText());

        view.findViewById(R.id.backButton).setOnClickListener(v -> navigateBack());
        saveButton.setOnClickListener(v -> saveText());
        return view;
    }

    private void setupGenerationControls() {
        levelSpinner.setAdapter(spinnerAdapter(LEVELS));
        levelSpinner.setSelection(2);

        String[] counts = {"5 palabras", "8 palabras", "12 palabras"};
        wordCountSpinner.setAdapter(spinnerAdapter(counts));
        wordCountSpinner.setSelection(1);
    }

    private ArrayAdapter<String> spinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, values);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        return adapter;
    }

    private void generateText() {
        String topic = topicInput.getText().toString().trim();
        if (topic.isEmpty()) {
            topicInput.setError("Escribe un tema");
            return;
        }
        if (!aiPrefs.hasApiKey()) {
            generationStatusText.setText("Añade tu clave IA en Ajustes para generar textos.");
            return;
        }

        String level = LEVELS[levelSpinner.getSelectedItemPosition()].substring(0, 2);
        int wordCount = WORD_COUNTS[wordCountSpinner.getSelectedItemPosition()];
        boolean examples = examplesSwitch.isChecked();

        generationStatusText.setText("Generando texto…");
        generateTextButton.setEnabled(false);
        AppExecutors.getInstance().diskIO().execute(() ->
                AiTextGenerator.generate(
                        topic, level, wordCount, examples,
                        aiPrefs.getApiKey(), aiPrefs.getModel(), aiPrefs.getEndpoint(),
                        (result, error) -> {
                            if (!isAdded()) {
                                return;
                            }
                            requireActivity().runOnUiThread(() -> {
                                generateTextButton.setEnabled(true);
                                if (error != null) {
                                    generationStatusText.setText("No se pudo generar: " + error);
                                    return;
                                }
                                titleEditText.setText(result.title);
                                contentEditText.setText(result.content);
                                generationStatusText.setText("Texto generado. Revísalo y guarda.");
                            });
                        }));
    }

    private void saveText() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Text text = new Text();
        text.setTitle(title);
        text.setContent(content);

        if (isEditMode) {
            text.setId(editId);
            viewModel.updateText(text);
        } else {
            viewModel.insertText(text);
        }
    }

    private void loadTextForEditing(long id) {
        viewModel.getTextById(id).observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                titleEditText.setText(text.title);
                contentEditText.setText(text.content);
            } else {
                Toast.makeText(getContext(), "Texto no encontrado", Toast.LENGTH_SHORT).show();
                navigateBack();
            }
        });
    }

    private void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTexts();
        }
    }
}
