package com.app.leelo.presentation.ui;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.app.leelo.R;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.domain.model.Text;
import com.google.android.material.button.MaterialButton;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.app.leelo.presentation.ui.MainActivity;
import com.app.leelo.presentation.ui.TextFragment;

public class ImportPdfTextFragment extends Fragment {

    private LinearLayout selectLayout;
    private LinearLayout loadingLayout;
    private LinearLayout readyLayout;
    private MaterialButton selectButton;
    private MaterialButton saveButton;
    private Uri selectedPdfUri;
    private String extractedText;
    private TextRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static boolean pdfBoxInitialized = false;

    private final ActivityResultLauncher<String[]> pickPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedPdfUri = uri;
                    processPdf(uri);
                }
            }
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPdfBox();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import_pdf_text, container, false);
        initViews(view);
        initRepository();
        setupListeners();
        return view;
    }

    private void initPdfBox() {
        if (!pdfBoxInitialized && getContext() != null) {
            PDFBoxResourceLoader.init(requireContext().getApplicationContext());
            pdfBoxInitialized = true;
        }
    }

    private void initViews(View view) {
        selectLayout = view.findViewById(R.id.selectLayout);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        readyLayout = view.findViewById(R.id.readyLayout);
        selectButton = view.findViewById(R.id.btnSelectPdf);
        saveButton = view.findViewById(R.id.saveButton);
    }

    private void initRepository() {
        if (getContext() != null) {
            repository = TextRepository.RepositoryProvider.getInstance(getContext());
        }
    }

    private void setupListeners() {
        selectButton.setOnClickListener(v -> openPdfPicker());
        saveButton.setOnClickListener(v -> saveText());
    }

    private void openPdfPicker() {
        pickPdfLauncher.launch(new String[]{"application/pdf"});
    }

    private void processPdf(Uri uri) {
        showState(State.PROCESSING);

        executor.execute(() -> {
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    throw new Exception("No se puede leer el archivo");
                }

                extractedText = extractTextFromPdf(inputStream);

                if (extractedText == null || extractedText.trim().isEmpty()) {
                    throw new Exception("El PDF no contiene texto legible");
                }

                requireActivity().runOnUiThread(this::onPdfProcessed);

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> onError(e.getMessage()));
            }
        });
    }

    private String extractTextFromPdf(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private void onPdfProcessed() {
        showState(State.READY);
    }

    private void onError(String message) {
        showState(State.SELECT);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void saveText() {
        if (extractedText == null || extractedText.isEmpty()) {
            Toast.makeText(requireContext(), "No hay texto para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Text text = new Text();
        text.setTitle(generateFileName());
        text.setContent(extractedText);
        text.setCreationDate(LocalDate.now());

        repository.insertText(text, (success, id) -> requireActivity().runOnUiThread(() -> {
            if (success) {
                Toast.makeText(requireContext(), "Texto guardado", Toast.LENGTH_SHORT).show();
                navigateToHome();
            } else {
                Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private String generateFileName() {
        String name = selectedPdfUri != null ? selectedPdfUri.getLastPathSegment() : "PDF Extraído";
        if (name != null && name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (name == null) name = "PDF Extraído";
        return name.replaceAll("[^a-zA-Z0-9\\s\\-_]", "").trim().isEmpty() 
                ? "PDF Extraído" 
                : name.replaceAll("[^a-zA-Z0-9\\s\\-_]", "").trim();
    }

    private void navigateToHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).replaceFragment(new TextFragment());
        }
    }

    private void showState(State state) {
        selectLayout.setVisibility(state == State.SELECT ? View.VISIBLE : View.GONE);
        loadingLayout.setVisibility(state == State.PROCESSING ? View.VISIBLE : View.GONE);
        readyLayout.setVisibility(state == State.READY ? View.VISIBLE : View.GONE);
    }

    private enum State {
        SELECT, PROCESSING, READY
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
