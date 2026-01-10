package com.app.leelo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.app.leelo.model.Text;
import com.app.leelo.data.repository.TextRepository;
import com.app.leelo.data.repository.TextRepository;

public class AddTextFragment extends Fragment {

    private EditText titleEditText;
    private EditText contentEditText;
    private boolean isEditMode = false;
    private long editId = -1;
    private TextRepository textRepository;

    public AddTextFragment() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textRepository = TextRepository.getInstance(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_text, container, false);

        titleEditText = view.findViewById(R.id.inputTittle);
        contentEditText = view.findViewById(R.id.inputText);
        Button saveButton = view.findViewById(R.id.SaveTextButtonText);

        Bundle args = getArguments();
        if (args != null) {
            isEditMode = true;
            editId = args.getLong("id", -1);
            titleEditText.setText(args.getString("title", ""));
            contentEditText.setText(args.getString("content", ""));
        }

        saveButton.setOnClickListener(v -> saveText());

        return view;
    }

private void saveText() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(getContext(), "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Text text = new Text();
        text.setTittle(title);
        text.setText(content);
        
        if (isEditMode) {
            text.setIdText(editId);
            updateText(text);
        } else {
            insertText(text);
        }
    }

    private void updateText(Text text) {
        textRepository.updateText(text, new TextRepository.OnUpdateCallback() {
            @Override
            public void onUpdateComplete(boolean success) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "Texto actualizado exitosamente", Toast.LENGTH_SHORT).show();
                            navigateBack();
                        } else {
                            Toast.makeText(getContext(), "Error al actualizar texto", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void insertText(Text text) {
        textRepository.insertText(text, new TextRepository.OnInsertCallback() {
            @Override
            public void onInsertComplete(boolean success, long id) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (id != -1) {
                            Toast.makeText(getContext(), "Texto guardado exitosamente", Toast.LENGTH_SHORT).show();
                            navigateBack();
                        } else {
                            Toast.makeText(getContext(), "Error al guardar texto", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
    
    private void navigateBack() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.replaceFragment(new TextFragment());
        }
    }
}