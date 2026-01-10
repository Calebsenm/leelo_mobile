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

public class AddTextFragment extends Fragment {

    private EditText titleEditText;
    private EditText contentEditText;
    private boolean isEditMode = false;
    private long editId = -1;

    public AddTextFragment() {
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

        if (isEditMode) {
            updateTextInList(title, content, editId);
        } else {
            addNewTextToList(title, content);
        }

        Toast.makeText(getContext(), "Texto guardado exitosamente", Toast.LENGTH_SHORT).show();
        
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.replaceFragment(new TextFragment());
        }
    }

    private void updateTextInList(String title, String content, long id) {
        // Store updated text data to be retrieved by TextFragment
        MainActivity.textUpdateData.putLong("update_id", id);
        MainActivity.textUpdateData.putString("update_title", title);
        MainActivity.textUpdateData.putString("update_content", content);
    }

    private void addNewTextToList(String title, String content) {
        // Store new text data to be retrieved by TextFragment
        MainActivity.textUpdateData.putString("new_title", title);
        MainActivity.textUpdateData.putString("new_content", content);
        MainActivity.textUpdateData.putLong("new_id", System.currentTimeMillis());
    }
}