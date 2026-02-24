package com.app.leelo.ui;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.app.leelo.R;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.model.Text;
import com.google.android.material.button.MaterialButton;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.time.LocalDate;

public class ImportPdfTextFragment extends Fragment {

    private MaterialButton saveButton;
    private MaterialButton selectButton;
    private MaterialButton btnPdfListo;
    private TextView extractedTextView;
    private View loadingLayout;

    private Uri selectedPdfUri = null;
    private String extractedText = "";

    private TextRepository textRepository;
    private static boolean pdfBoxInitialized = false;

    private final ActivityResultLauncher<String[]> pickPdfLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            selectedPdfUri = uri;
                            extractTextFromPdf(uri);
                        }
                    });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(requireContext().getApplicationContext());
            pdfBoxInitialized = true;
            Log.d("PDFBOX", "PDFBox inicializado correctamente");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        if (getContext() != null) {
            textRepository = TextRepository.RepositoryProvider.getInstance(getContext());
        }

        View view = inflater.inflate(
                R.layout.fragment_import_pdf_text, container, false);

        selectButton = view.findViewById(R.id.btnSelectPdf);
        saveButton = view.findViewById(R.id.SaveTextButtonTextPdf);
        btnPdfListo = view.findViewById(R.id.btnPdfListo);
        extractedTextView = view.findViewById(R.id.textPdfExtracted);
        loadingLayout = view.findViewById(R.id.loadingLayout);


        selectButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.GONE);
        btnPdfListo.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.GONE);
        extractedTextView.setVisibility(View.GONE);


        selectButton.setOnClickListener(v ->
                pickPdfLauncher.launch(new String[]{"application/pdf"})
        );


        btnPdfListo.setOnClickListener(v -> {
            if (!extractedText.isEmpty()) {
                extractedTextView.setText(extractedText);
                extractedTextView.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Texto disponible", Toast.LENGTH_SHORT).show();
            }
        });

        saveButton.setOnClickListener(v -> {
            if (!extractedText.isEmpty() && textRepository != null) {
                saveTextToDatabase();
            } else {
                Toast.makeText(requireContext(), "No hay texto para guardar", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void extractTextFromPdf(Uri uri) {
        selectButton.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.VISIBLE);

        new Thread(() -> {
            InputStream inputStream = null;
            try {
                if (getContext() == null) throw new Exception("Contexto no disponible");

                inputStream = requireContext()
                        .getContentResolver().openInputStream(uri);

                if (inputStream == null) throw new Exception("No se puede leer el archivo PDF");

                String text = extractRealTextFromPdf(inputStream);

                if (text.trim().isEmpty()) throw new Exception("Este PDF no contiene texto legible");

                extractedText = text;

                requireActivity().runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    btnPdfListo.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "PDF procesado", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("EXTRACT_ERROR", e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    selectButton.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(),
                            e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception e) {
                    Log.e("STREAM_CLOSE_ERROR", e.getMessage());
                }
            }
        }).start();
    }

    private String extractRealTextFromPdf(InputStream is) {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (Exception e) {
            Log.e("PDF_EXTRACTION_ERROR", e.getMessage());
            return "";
        }
    }

    private void saveTextToDatabase() {
        String fileName = generateSafeFileName();
        Text text = new Text();
        text.setTitle(fileName);
        text.setContent(extractedText);
        text.setCreationDate(LocalDate.now());

        textRepository.insertText(text,
                (success, id) -> requireActivity().runOnUiThread(() -> {
                    String msg = success ?
                            "✅ Texto guardado exitosamente" :
                            "❌ Error al guardar texto";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                }));
    }

    private String generateSafeFileName() {
        String name = (selectedPdfUri != null ?
                selectedPdfUri.getLastPathSegment() : "PDF Extraído");
        if (name != null && name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (name == null) name = "PDF Extraído";
        name = name.replaceAll("[^a-zA-Z0-9\\s\\-_]", "").trim();
        return name.isEmpty() ? "PDF Extraído" : name;
    }
}
