package com.app.leelo.presentation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.app.leelo.R;
import com.app.leelo.domain.repository.WordRepository;
import com.app.leelo.domain.model.Word;
import com.app.leelo.presentation.viewmodel.WordViewModel;
import com.app.leelo.presentation.viewmodel.WordViewModelFactory;
import com.google.android.material.tabs.TabLayout;
import java.util.List;

public class WordsFragment extends Fragment {

    private WordViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView totalCount, learningCount, learnedCount;
    private TabLayout tabLayout;
    private WordAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_words, container, false);

        initViews(view);
        initViewModel();
        setupTabs();
        setupRecyclerView();
        observeData();

        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.wordsRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        totalCount = view.findViewById(R.id.totalCount);
        learningCount = view.findViewById(R.id.learningCount);
        learnedCount = view.findViewById(R.id.learnedCount);
    }

    private void initViewModel() {
        WordRepository repo = WordRepository.RepositoryProvider.getInstance(requireContext());
        viewModel = new ViewModelProvider(this, new WordViewModelFactory(repo))
                .get(WordViewModel.class);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Todas"));
        tabLayout.addTab(tabLayout.newTab().setText("Aprendiendo"));
        tabLayout.addTab(tabLayout.newTab().setText("Aprendidas"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        viewModel.setFilter(WordViewModel.FilterType.ALL);
                        break;
                    case 1:
                        viewModel.setFilter(WordViewModel.FilterType.LEARNING);
                        break;
                    case 2:
                        viewModel.setFilter(WordViewModel.FilterType.LEARNED);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new WordAdapter(word -> {
            showWordOptionsDialog(word);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getFilteredWords().observe(getViewLifecycleOwner(), words -> {
            adapter.submitList(words);
            if (words == null || words.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        });

        viewModel.totalCount.observe(getViewLifecycleOwner(), count -> {
            totalCount.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.learningCount.observe(getViewLifecycleOwner(), count -> {
            learningCount.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.learnedCount.observe(getViewLifecycleOwner(), count -> {
            learnedCount.setText(String.valueOf(count != null ? count : 0));
        });
    }

    private void showWordOptionsDialog(Word word) {
        String[] options = {"Cambiar a Aprendiendo", "Cambiar a Aprendida", "Eliminar"};
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(word.getWord())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            word.setState(Word.State.LEARNING);
                            viewModel.updateWord(word);
                            break;
                        case 1:
                            word.setState(Word.State.LEARNED);
                            viewModel.updateWord(word);
                            break;
                        case 2:
                            viewModel.deleteWord(word.getId());
                            break;
                    }
                })
                .show();
    }

    private static class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {

        private List<Word> words;
        private final OnWordClickListener listener;

        interface OnWordClickListener {
            void onWordClick(Word word);
        }

        WordAdapter(OnWordClickListener listener) {
            this.listener = listener;
        }

        void submitList(List<Word> words) {
            this.words = words;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_word, parent, false);
            return new WordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
            if (words != null) {
                holder.bind(words.get(position), listener);
            }
        }

        @Override
        public int getItemCount() {
            return words != null ? words.size() : 0;
        }

        static class WordViewHolder extends RecyclerView.ViewHolder {
            private final View stateIndicator;
            private final TextView wordText;
            private final TextView meaningText;
            private final TextView stateText;
            private final View deleteButton;

            WordViewHolder(View itemView) {
                super(itemView);
                stateIndicator = itemView.findViewById(R.id.stateIndicator);
                wordText = itemView.findViewById(R.id.wordText);
                meaningText = itemView.findViewById(R.id.meaningText);
                stateText = itemView.findViewById(R.id.stateText);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }

            void bind(Word word, OnWordClickListener listener) {
                wordText.setText(word.getWord() != null ? word.getWord() : "");
                meaningText.setText(word.getMeaning() != null ? word.getMeaning() : "");

                int stateColor;
                String stateLabel;

                Word.State state = word.getState();
                
                switch (state) {
                    case NEW:
                        stateColor = itemView.getContext().getColor(R.color.word_new);
                        stateLabel = "Nueva";
                        break;
                    case LEARNING:
                        stateColor = itemView.getContext().getColor(R.color.word_learning);
                        stateLabel = "Aprendiendo";
                        break;
                    case LEARNED:
                    default:
                        stateColor = android.graphics.Color.TRANSPARENT;
                        stateLabel = "Aprendida";
                        break;
                }

                stateIndicator.setBackgroundColor(stateColor);
                stateText.setText(stateLabel);
                if (state == Word.State.LEARNED) {
                    stateText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.word_learned));
                } else {
                    stateText.setTextColor(stateColor);
                }

                itemView.setOnClickListener(v -> listener.onWordClick(word));
                deleteButton.setOnClickListener(v -> listener.onWordClick(word));
            }
        }
    }
}
