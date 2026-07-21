package com.app.leelo.presentation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.app.leelo.R;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.domain.model.Text;
import com.app.leelo.presentation.viewmodel.TextViewModel;
import com.app.leelo.presentation.viewmodel.ViewModelFactory;
import com.app.leelo.presentation.ui.TextFragment;

public class AddTextFragment extends Fragment {

    private EditText titleEditText, contentEditText;
    private Button saveButton;
    private boolean isEditMode = false;
    private long editId = -1;
    private TextViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextRepository repo = TextRepository.RepositoryProvider.getInstance(requireContext());
        viewModel = new ViewModelProvider(this, new ViewModelFactory(repo))
                .get(TextViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_text, container, false);

        titleEditText = view.findViewById(R.id.inputTittle);
        contentEditText = view.findViewById(R.id.inputText);
        saveButton = view.findViewById(R.id.save_text_button);

        Bundle args = getArguments();
        if (args != null && args.containsKey("id")) {
            isEditMode = true;
            editId = args.getLong("id", -1);
            loadTextForEditing(editId);
        }

        saveButton.setOnClickListener(v -> saveText());
        return view;
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
            ((MainActivity) getActivity()).replaceFragment(new TextFragment());
        }
    }
}