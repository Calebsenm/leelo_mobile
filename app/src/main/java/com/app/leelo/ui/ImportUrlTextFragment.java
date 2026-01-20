package com.app.leelo.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.app.leelo.R;
import com.app.leelo.data.repository.TextRepository;
import com.app.leelo.model.Text;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportUrlTextFragment extends Fragment {

    private TextInputEditText inputTitle, inputUrl;
    private MaterialButton saveButton;
    private TextRepository textRepository;
    private ExecutorService executor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_import_url_text, container, false);

        inputTitle = view.findViewById(R.id.inputTittleUrl);
        inputUrl = view.findViewById(R.id.inputTextUrl);
        saveButton = view.findViewById(R.id.SaveTextButtonTextUrl);

        textRepository = TextRepository.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();

        saveButton.setOnClickListener(v -> validateAndExtract());

        return view;
    }

    private void validateAndExtract() {
        String title = inputTitle.getText().toString().trim();
        String url = inputUrl.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            inputTitle.setError("El título es requerido");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            inputUrl.setError("La URL es requerida");
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            inputUrl.setError("URL inválida");
            return;
        }

        extractTextAndSave(title, url);
    }

    private void extractTextAndSave(String title, String url) {

        saveButton.setEnabled(false);
        saveButton.setText("Extrayendo...");

        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(url)
                        .timeout(30000)
                        .userAgent("Mozilla/5.0")
                        .get();

                String content = extractText(doc);

                if (content.isEmpty()) {
                    showError("No se pudo extraer texto de la URL");
                    return;
                }

                Text text = new Text();
                text.setTitle(title);
                text.setText(content);
                text.setCreationDate(LocalDate.now());

                textRepository.insertText(text, (success, id) -> requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(), "Texto guardado", Toast.LENGTH_SHORT).show();
                        clearInputs();
                    } else {
                        Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                    }
                    resetButton();
                }));

            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        });
    }

    private String extractText(Document doc) {

        // Limpiar basura
        doc.select("script, style, nav, footer, header, aside").remove();

        Elements paragraphs = doc.select("p");
        StringBuilder result = new StringBuilder();

        for (var p : paragraphs) {
            String text = p.text().trim();
            if (text.length() > 30) {
                result.append(text).append("\n\n");
            }
        }

        // Fallback
        if (result.length() < 200) {
            result.append(doc.body().text());
        }

        return result.toString()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            resetButton();
        });
    }

    private void resetButton() {
        saveButton.setEnabled(true);
        saveButton.setText("Guardar");
    }

    private void clearInputs() {
        inputTitle.setText("");
        inputUrl.setText("");
        inputTitle.requestFocus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }
}
