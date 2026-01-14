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
                InputStream inputStream = null;
                try {
                    // Validar contexto
                    if (getContext() == null) {
                        throw new Exception("Contexto no disponible");
                    }

                    // Validar URI
                    if (uri == null) {
                        throw new Exception("URI del archivo es nula");
                    }

                    Log.d("EXTRACT_START", "Extrayendo texto de: " + uri.toString());

                    inputStream = getContext().getContentResolver().openInputStream(uri);
                    if (inputStream == null) {
                        throw new Exception("No se puede leer el archivo PDF");
                    }

                    // Extraer texto real del PDF
                    extractedText = extractRealTextFromPdf(inputStream);
                    
                    // Validar resultado
                    if (extractedText == null || extractedText.trim().isEmpty()) {
                        throw new Exception("El PDF no contiene texto legible");
                    }
                    
                    Log.d("EXTRACT_SUCCESS", "Texto extraído: " + extractedText.length() + " caracteres");
                    
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
        com.tom_roush.pdfbox.pdmodel.PDDocument document = null;
        
        try {
            Log.d("PDF_EXTRACTION", "Iniciando extracción de PDF");
            
            // Verificar que el inputStream no sea nulo
            if (inputStream == null) {
                throw new Exception("InputStream es nulo");
            }
            
            // Cargar documento con manejo de errores
            document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream);
            
            if (document.getNumberOfPages() == 0) {
                throw new Exception("El PDF no tiene páginas");
            }
            
            // Extraer texto
            com.tom_roush.pdfbox.text.PDFTextStripper stripper = new com.tom_roush.pdfbox.text.PDFTextStripper();
            stripper.setSortByPosition(true); // Mejor ordenamiento del texto
            
            String text = stripper.getText(document);
            
            if (text == null || text.trim().isEmpty()) {
                Log.w("PDF_EXTRACTION", "PDF sin texto extraíble");
                return "Este PDF no contiene texto legible (puede ser solo imágenes)";
            }
            
            // Limitar tamaño para evitar problemas de memoria
            if (text.length() > 500000) { // 500KB límite
                text = text.substring(0, 500000) + "\n\n[Texto truncado por tamaño]";
                Log.w("PDF_EXTRACTION", "Texto truncado por tamaño excesivo");
            }
            
            Log.d("PDF_EXTRACTION", "Texto extraído exitosamente: " + text.length() + " caracteres");
            return text;
            
        } catch (OutOfMemoryError e) {
            Log.e("PDF_EXTRACT_ERROR", "Error de memoria procesando PDF: " + e.getMessage());
            return "PDF demasiado grande para procesar. Intenta con un archivo más pequeño.";
        } catch (Exception e) {
            Log.e("PDF_EXTRACT_ERROR", "Error extrayendo texto: " + e.getMessage());
            e.printStackTrace();
            
            // Dar mensajes más específicos según el error
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("encrypted") || errorMsg.contains("password")) {
                    return "Este PDF está protegido con contraseña y no puede ser procesado.";
                } else if (errorMsg.contains("corrupted") || errorMsg.contains("damaged")) {
                    return "El archivo PDF está dañado o corrupto.";
                } else if (errorMsg.contains("parse")) {
                    return "El formato del PDF no es válido o está corrupto.";
                }
            }
            
            return "Error al procesar PDF: " + (errorMsg != null && errorMsg.length() < 100 ? errorMsg : "Error desconocido");
        } finally {
            // Asegurar cerrar el documento siempre
            try {
                if (document != null) {
                    document.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e("PDF_CLOSE_ERROR", "Error cerrando recursos: " + e.getMessage());
            }
        }
    }

    private void saveTextToDatabase() {
        try {
            Log.d("SAVE_START", "Iniciando guardado de texto a base de datos");
            
            // Validaciones básicas
            if (textRepository == null) {
                throw new Exception("Repositorio no inicializado");
            }
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new Exception("No hay texto para guardar");
            }
            
            // Generar nombre de archivo seguro
            String fileName = generateSafeFileName();
            
            // Crear objeto Text con validaciones
            Text text = new Text();
            text.setTitle(fileName);
            text.setText(extractedText);
            text.setCreationDate(LocalDate.now());
            
            Log.d("SAVE_TEXT", "Guardando texto: " + fileName + " (longitud: " + extractedText.length() + ")");
            
            // Guardar en base de datos
            textRepository.insertText(text, new TextRepository.OnInsertCallback() {
                @Override
                public void onInsertComplete(boolean success, long id) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                if (success) {
                                    Toast.makeText(getContext(), "✅ Texto guardado exitosamente", Toast.LENGTH_LONG).show();
                                    Log.d("SAVE_SUCCESS", "Texto guardado con ID: " + id);
                                } else {
                                    Toast.makeText(getContext(), "❌ Error al guardar texto", Toast.LENGTH_LONG).show();
                                    Log.e("SAVE_ERROR", "No se pudo guardar el texto (callback false)");
                                }
                            } catch (Exception e) {
                                Log.e("SAVE_UI_ERROR", "Error actualizando UI después de guardar: " + e.getMessage());
                            }
                        });
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e("SAVE_INIT_ERROR", "Error iniciando guardado: " + e.getMessage());
            e.printStackTrace();
            
            try {
                Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception toastError) {
                Log.e("TOAST_ERROR", "Error mostrando toast: " + toastError.getMessage());
            }
        }
    }
    
    private String generateSafeFileName() {
        try {
            String fileName = selectedPdfUri != null ? selectedPdfUri.getLastPathSegment() : "PDF Extraído";
            
            if (fileName == null) {
                fileName = "PDF Extraído";
            }
            
            // Extraer solo el nombre del archivo (sin ruta)
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            
            // Remover extensión .pdf si existe
            if (fileName.toLowerCase().endsWith(".pdf")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            
            // Limpiar caracteres inválidos
            fileName = fileName.replaceAll("[^a-zA-Z0-9\\s\\-_]", "").trim();
            
            // Asegurar que no esté vacío
            if (fileName.trim().isEmpty()) {
                fileName = "PDF Extraído";
            }
            
            // Limitar longitud
            if (fileName.length() > 50) {
                fileName = fileName.substring(0, 50);
            }
            
            Log.d("FILENAME", "Nombre generado: " + fileName);
            return fileName;
            
        } catch (Exception e) {
            Log.e("FILENAME_ERROR", "Error generando nombre de archivo: " + e.getMessage());
            return "PDF Extraído";
        }
    }
}