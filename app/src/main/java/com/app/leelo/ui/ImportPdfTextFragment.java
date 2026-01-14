package com.app.leelo.ui;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.app.leelo.R;
import com.app.leelo.data.repository.TextRepository;
import com.app.leelo.model.Text;
import com.google.android.material.button.MaterialButton;

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

    private final ActivityResultLauncher<String[]> pickPdfLauncher = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(), 
        uri -> {
            if (uri != null) {
                selectedPdfUri = uri;
                extractTextFromPdf(uri);
            }
        });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        
        // Inicializar repositorio
        if (getContext() != null) {
            textRepository = TextRepository.getInstance(getContext());
        }
        
        View view = inflater.inflate(R.layout.fragment_import_pdf_text, container, false);
        
        try {
            selectButton = view.findViewById(R.id.btnSelectPdf);
            saveButton = view.findViewById(R.id.SaveTextButtonTextPdf);
            btnPdfListo = view.findViewById(R.id.btnPdfListo);
            extractedTextView = view.findViewById(R.id.textPdfExtracted);
            loadingLayout = view.findViewById(R.id.loadingLayout);

            // Estado inicial: solo mostrar botón de seleccionar
            if (selectButton != null) {
                selectButton.setVisibility(View.VISIBLE);
            }
            if (saveButton != null) {
                saveButton.setVisibility(View.GONE);
            }
            if (btnPdfListo != null) {
                btnPdfListo.setVisibility(View.GONE);
            }
            if (loadingLayout != null) {
                loadingLayout.setVisibility(View.GONE);
            }
            if (extractedTextView != null) {
                extractedTextView.setVisibility(View.GONE);
            }

            // Listener para seleccionar PDF
            if (selectButton != null) {
                selectButton.setOnClickListener(v -> {
                    try {
                        pickPdfLauncher.launch(new String[]{"application/pdf"});
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al abrir selector", Toast.LENGTH_SHORT).show();
                        Log.e("SELECT_ERROR", e.getMessage());
                    }
                });
            }

            // Listener para botón PDF LISTO
            if (btnPdfListo != null) {
                btnPdfListo.setOnClickListener(v -> {
                    try {
                        if (!extractedText.isEmpty()) {
                            if (extractedTextView != null) {
                                extractedTextView.setText(extractedText);
                                extractedTextView.setVisibility(View.VISIBLE);
                            }
                            if (saveButton != null) {
                                saveButton.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(getContext(), "Texto mostrado", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al mostrar texto", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Listener para guardar
            if (saveButton != null) {
                saveButton.setOnClickListener(v -> {
                    try {
                        if (!extractedText.isEmpty() && textRepository != null) {
                            saveTextToDatabase();
                        } else {
                            Toast.makeText(getContext(), "No hay texto para guardar", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            Log.e("CREATE_VIEW_ERROR", "Error: " + e.getMessage());
            e.printStackTrace();
        }

        return view;
    }

    private void extractTextFromPdf(Uri uri) {
        try {
            // Mostrar estado de carga
            if (selectButton != null) selectButton.setVisibility(View.GONE);
            if (loadingLayout != null) loadingLayout.setVisibility(View.VISIBLE);

            // Extraer texto en segundo plano
            new Thread(() -> {
                try {
                    if (getContext() == null) {
                        throw new Exception("Contexto nulo");
                    }

                    InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                    if (inputStream == null) {
                        throw new Exception("No se puede leer el archivo");
                    }

                    // Extraer texto real del PDF
                    extractedText = extractRealTextFromPdf(inputStream);
                    if (extractedText.trim().isEmpty()) {
                        extractedText = "No se pudo extraer texto del PDF: " + uri.getLastPathSegment();
                    }
                    
                    inputStream.close();

                    // Actualizar UI en hilo principal
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
                                if (btnPdfListo != null) btnPdfListo.setVisibility(View.VISIBLE);
                                Toast.makeText(getContext(), "PDF procesado", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Log.e("UI_UPDATE_ERROR", e.getMessage());
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e("EXTRACT_ERROR", e.getMessage());
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
                                if (selectButton != null) selectButton.setVisibility(View.VISIBLE);
                                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            } catch (Exception ee) {
                                Log.e("ERROR_RECOVERY", ee.getMessage());
                            }
                        });
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e("EXTRACT_START_ERROR", e.getMessage());
            Toast.makeText(getContext(), "Error al iniciar extracción", Toast.LENGTH_SHORT).show();
        }
    }

    private String extractRealTextFromPdf(InputStream inputStream) {
        try {
            // Usar PDFBox para extraer texto real
            com.tom_roush.pdfbox.pdmodel.PDDocument document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream);
            com.tom_roush.pdfbox.text.PDFTextStripper stripper = new com.tom_roush.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            
            return text;
            
        } catch (Exception e) {
            Log.e("PDF_EXTRACT_ERROR", "Error extrayendo texto: " + e.getMessage());
            return "Error al extraer texto del PDF: " + e.getMessage();
        }
    }

    private void saveTextToDatabase() {
        String fileName = selectedPdfUri != null ? selectedPdfUri.getLastPathSegment() : "PDF Desconocido";
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "PDF Extraído";
        }
        
        // Remover extensión .pdf si existe
        if (fileName.toLowerCase().endsWith(".pdf")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        
        Text text = new Text();
        text.setTitle(fileName);
        text.setText(extractedText);
        text.setCreationDate(LocalDate.now());

        textRepository.insertText(text, new TextRepository.OnInsertCallback() {
            @Override
            public void onInsertComplete(boolean success, long id) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "✅ Texto guardado exitosamente", Toast.LENGTH_LONG).show();
                            Log.d("SAVE_SUCCESS", "Texto guardado con ID: " + id);
                        } else {
                            Toast.makeText(getContext(), "❌ Error al guardar texto", Toast.LENGTH_LONG).show();
                            Log.e("SAVE_ERROR", "No se pudo guardar el texto");
                        }
                    });
                }
            }
        });
    }
}