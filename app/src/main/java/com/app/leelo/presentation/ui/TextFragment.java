package com.app.leelo.presentation.viewmodel.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.app.leelo.R;
import com.app.leelo.domain.repository.model.TextInfo;
import com.app.leelo.presentation.viewmodel.TextViewModel;
import com.app.leelo.presentation.viewmodel.ViewModelFactory;
import com.app.leelo.domain.repository.TextRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class TextFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView emptySearchView;
    private SearchView searchView;
    private TextAdapter adapter;
    private TextViewModel viewModel;
    private boolean isSearching = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_text, container, false);

        recyclerView = view.findViewById(R.id.recyclerText);
        emptyView = view.findViewById(R.id.emptyView);
        emptySearchView = view.findViewById(R.id.emptySearchView);
        searchView = view.findViewById(R.id.searchView);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TextAdapter(new TextAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TextInfo textInfo) {
                Intent i = new Intent(requireContext(), ReadingActivity.class);
                i.putExtra("text_id", textInfo.getId());
                i.putExtra("title", textInfo.getTitle());
                startActivity(i);
            }

            @Override
            public void onMenuClick(TextInfo textInfo) {
                showOptionsMenu(textInfo);
            }
        }, recyclerView, emptyView, emptySearchView);

        recyclerView.setAdapter(adapter);

        TextRepository repo = TextRepository.RepositoryProvider.getInstance(requireContext());
        viewModel = new ViewModelProvider(this, new ViewModelFactory(repo))
                .get(TextViewModel.class);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                isSearching = !newText.trim().isEmpty();
                viewModel.search(newText);
                return true;
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), texts -> {
            adapter.submitList(texts, isSearching);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            // Manejar estado de carga si es necesario
        });

        view.findViewById(R.id.add_text_button).setOnClickListener(v -> showAddTextOptions());

        return view;
    }

    private static class TextAdapter extends RecyclerView.Adapter<TextAdapter.VH> {

        private List<TextInfo> texts;
        private final OnItemClickListener listener;
        private final RecyclerView recyclerView;
        private final TextView emptyView;
        private final TextView emptySearchView;

        public TextAdapter(OnItemClickListener listener, RecyclerView recyclerView, TextView emptyView, TextView emptySearchView) {
            this.listener = listener;
            this.texts = null;
            this.recyclerView = recyclerView;
            this.emptyView = emptyView;
            this.emptySearchView = emptySearchView;
        }

        public void submitList(List<TextInfo> texts, boolean isSearching) {
            this.texts = texts;
            notifyDataSetChanged();
            
            if (texts == null || texts.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                if (isSearching) {
                    emptyView.setVisibility(View.GONE);
                    emptySearchView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.VISIBLE);
                    emptySearchView.setVisibility(View.GONE);
                }
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                emptySearchView.setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (texts != null) {

                TextInfo info = texts.get(pos);
                h.title.setText(info.getTitle());

                int totalPages = info.getTotalPages();
                int currentPage = info.getCurrentPage();
                int progress = totalPages > 0 ? Math.min(100, Math.max(0, (currentPage * 100) / totalPages)) : 0;


                h.progress.setProgress(progress);
                h.itemView.setOnClickListener(v -> listener.onItemClick(info));
                h.menu.setOnClickListener(v -> listener.onMenuClick(info));
            }
        }

        @Override
        public int getItemCount() {
            return texts != null ? texts.size() : 0;
        }

        static class VH extends RecyclerView.ViewHolder {
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

        interface OnItemClickListener {
            void onItemClick(TextInfo textInfo);
            void onMenuClick(TextInfo textInfo);
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
            editText(info.getId());
        });

        delete.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.deleteText(info.getId());
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
