package com.app.leelo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.app.leelo.R;
import com.app.leelo.model.TextInfo;
import com.app.leelo.viewmodel.TextViewModel;
import com.app.leelo.viewmodel.ViewModelFactory;
import com.app.leelo.data.repository.TextRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;

public class TextFragment extends Fragment {

    private TextViewModel viewModel;
    private TextAdapter adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_text, container, false);

        RecyclerView recycler = view.findViewById(R.id.recyclerText);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TextAdapter();
        recycler.setAdapter(adapter);

        TextRepository repo = TextRepository.getInstance(requireContext());
        ViewModelFactory factory = new ViewModelFactory(repo);

        viewModel = new ViewModelProvider(this, factory).get(TextViewModel.class);
        viewModel.getTexts().observe(getViewLifecycleOwner(), adapter::setItems);
        viewModel.loadTexts();

        view.findViewById(R.id.add_text_button).setOnClickListener(v -> showAddTextOptions());

        return view;
    }


    private class TextAdapter extends RecyclerView.Adapter<TextAdapter.VH> {

        private final List<TextInfo> items = new ArrayList<>();
        void setItems(List<TextInfo> texts) {
            items.clear();
            items.addAll(texts);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_text, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            TextInfo info = items.get(pos);
            h.title.setText(info.titulo);
            h.progress.setProgress(0);

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), ReadingActivity.class);
                i.putExtra("text_id", info.id);
                i.putExtra("title", info.titulo);
                startActivity(i);
            });

            h.menu.setOnClickListener(v -> showOptionsMenu(info));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title;
            ImageButton menu;
            ProgressBar progress;

            VH(View v) {
                super(v);
                title = v.findViewById(R.id.textTitle);
                menu = v.findViewById(R.id.menuButton);
                progress = v.findViewById(R.id.readingProgress);
            }
        }
    }

    private void showOptionsMenu(TextInfo info) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(
                R.layout.bottomssheet_edit_text, null
        );
        dialog.setContentView(view);

        View edit = view.findViewById(R.id.layout_edit_text);
        View delete = view.findViewById(R.id.layout_delete_text);

        edit.setOnClickListener(v -> {
            dialog.dismiss();
            editText(info.id);
        });

        delete.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.deleteText(info.id);
        });

        dialog.show();
    }

    private void editText(long id) {
        AddTextFragment f = new AddTextFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        f.setArguments(b);
        ((MainActivity) requireActivity()).replaceFragment(f);
    }

    private void showAddTextOptions() {
        BottomSheetDialog d = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.bottomsheet, null);
        d.setContentView(v);

        v.findViewById(R.id.layout_add_text)
                .setOnClickListener(x -> {
                    d.dismiss();
                    ((MainActivity) requireActivity())
                            .replaceFragment(new AddTextFragment());
                });

        v.findViewById(R.id.layout_add_text_url)
                .setOnClickListener(x -> {
                    d.dismiss();
                    ((MainActivity) requireActivity())
                            .replaceFragment(new ImportUrlTextFragment());
                });

        v.findViewById(R.id.layout_add_text_pdf)
                .setOnClickListener(x -> {
                    d.dismiss();
                    ((MainActivity) requireActivity())
                            .replaceFragment(new ImportPdfTextFragment());
                });

        d.show();
    }
}
