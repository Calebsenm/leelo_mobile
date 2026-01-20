package com.app.leelo.ui.text;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.app.leelo.ui.ImportPdfTextFragment;
import com.app.leelo.ui.ImportUrlTextFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.leelo.R;
import com.app.leelo.data.repository.TextRepository;
import com.app.leelo.model.TextInfo;
import com.app.leelo.ui.AddTextFragment;
import com.app.leelo.ui.MainActivity;
import com.app.leelo.ui.ReadingActivity;
import com.app.leelo.ui.text.TextAdapter;
import com.app.leelo.viewmodel.TextViewModel;
import com.app.leelo.viewmodel.ViewModelFactory;

public class TextFragment extends Fragment {

    private TextViewModel viewModel;
    private TextAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_text, container, false);

        RecyclerView recycler = view.findViewById(R.id.recyclerText);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TextAdapter(info -> {
            Intent intent = new Intent(requireContext(), ReadingActivity.class);
            intent.putExtra("text_id", info.id);
            intent.putExtra("title", info.titulo);
            startActivity(intent);
        }, info -> {
            showOptionsMenu(info);
        });
        recycler.setAdapter(adapter);

        TextRepository repo = TextRepository.getInstance(requireContext());
        ViewModelFactory factory = new ViewModelFactory(repo);

        viewModel = new ViewModelProvider(this, factory).get(TextViewModel.class);
        viewModel.getTexts().observe(getViewLifecycleOwner(), adapter::setItems);
        viewModel.loadTexts();

        View addTextButton = view.findViewById(R.id.add_text_button);
        addTextButton.setOnClickListener(v -> {
            showAddTextOptions();
        });

        return view;
    }

    private void showOptionsMenu(TextInfo info) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottomssheet_edit_text, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        View editLayout = bottomSheetView.findViewById(R.id.layout_edit_text);
        View deleteLayout = bottomSheetView.findViewById(R.id.layout_delete_text);

        editLayout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            ((MainActivity) requireActivity()).editText(info.id);
        });

        deleteLayout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            deleteText(info);
        });

        bottomSheetDialog.show();
    }

    private void deleteText(TextInfo info) {
        TextRepository repo = TextRepository.getInstance(requireContext());
        repo.deleteText(info.id, new TextRepository.OnDeleteCallback() {
            @Override
            public void onDeleteComplete(boolean success) {
                if (success) {
                    viewModel.loadTexts();
                } else {
                    
                }
            }
        });
    }

    private void showAddTextOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottomsheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        View addTextLayout = bottomSheetView.findViewById(R.id.layout_add_text);
        View addUrlLayout = bottomSheetView.findViewById(R.id.layout_add_text_url);
        View addPdfLayout = bottomSheetView.findViewById(R.id.layout_add_text_pdf);

        addTextLayout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            ((MainActivity) requireActivity()).replaceFragment(new AddTextFragment());
        });

        addUrlLayout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            ((MainActivity) requireActivity()).replaceFragment(new ImportUrlTextFragment());
        });

        addPdfLayout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            ((MainActivity) requireActivity()).replaceFragment(new ImportPdfTextFragment());
        });

        bottomSheetDialog.show();
    }
}
