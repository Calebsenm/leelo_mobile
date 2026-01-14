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
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;

public class ImportPdfTextFragment extends Fragment {

    private MaterialButton saveButton;
    private MaterialButton selectButton;
    private MaterialButton btnPdfListo;
    private TextView extractedTextView;
    private View loadingLayout;
    private Uri selectedPdfUri = null;
    private String extractedText = "";

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
                        if (!extractedText.isEmpty()) {
                            // Aquí iría el guardado real
                            Toast.makeText(getContext(), "Texto guardado", Toast.LENGTH_SHORT).show();
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

                    // Simulación - reemplazar con extracción real de PDF
                    Thread.sleep(1500); // Simular procesamiento
                    
                    extractedText = "Texto extraído del PDF: " + uri.getLastPathSegment() + "\n\n" +
                                   "Aquí aparecerá el texto real del PDF cuando se implemente " +
                                   "la extracción con PDFBox correctamente.";
                    
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
}