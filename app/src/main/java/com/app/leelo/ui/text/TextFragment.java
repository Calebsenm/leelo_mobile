package com.app.leelo.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.app.leelo.R;
import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.model.Text;
import com.app.leelo.model.TextInfo;
import com.app.leelo.data.repository.TextRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class TextFragment extends Fragment {

    List<TextInfo> texts = new ArrayList<>();
    private TextRepository textRepository;

    public TextFragment() {
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (textRepository != null) {
            refreshTexts();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View thisFragmentView = inflater.inflate(R.layout.fragment_text, container, false);

        textRepository = TextRepository.getInstance(requireContext());

        loadTextsFromDatabase();

        checkForUpdates();


        FloatingActionButton addTextButton = thisFragmentView.findViewById(R.id.add_text_button);


        addTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        thisFragmentView.post(() -> {
            checkForUpdates();
            renderTexts();
        });

        return thisFragmentView;
    }

    private void loadTextsFromDatabase() {
        textRepository.getAllTextInfoAsync(new TextRepository.OnGetAllTextInfoCallback() {
            @Override
            public void onGetAllTextInfoComplete(List<TextInfo> entities) {
                texts.clear();
                texts.addAll(entities);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> renderTexts());
                }
            }
        });
    }


    private void refreshTexts() {
        loadTextsFromDatabase();
    }

    private View createTextItem(TextInfo t) {
        LinearLayout parent = new LinearLayout(requireContext());
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setPadding(dp(16), dp(16), dp(16), dp(16));
        parent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        FloatingActionButton fabBook = new FloatingActionButton(requireContext());
        fabBook.setImageResource(R.drawable.baseline_book_24);
        fabBook.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.gold)
        );
        fabBook.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), android.R.color.black)
        );
        fabBook.setOnClickListener(v -> {
            openReadingScreen(t.id);
        });

        LinearLayout textLayout = new LinearLayout(requireContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textLayout.setLayoutParams(textParams);
        textLayout.setPadding(dp(16), 0, dp(16), 0);

        TextView title = new TextView(requireContext());
        title.setText(t.titulo);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);

        ProgressBar progressBar = new ProgressBar(
                requireContext(), null,
                android.R.attr.progressBarStyleHorizontal
        );
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        FloatingActionButton fabMore = new FloatingActionButton(requireContext());
        fabMore.setImageResource(R.drawable.more_vert);
        fabMore.setTag(t);
        fabMore.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.white)
        );
        fabMore.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), android.R.color.black)
        );

        fabMore.setOnClickListener(v -> {
            TextInfo textInfo = (TextInfo) v.getTag();
            textRepository.getTextById(textInfo.id, new TextRepository.OnGetTextCallback() {
                @Override
                public void onGetTextComplete(Text book) {
                    if (book != null) {
                        showEditDialog(book);
                    }
                }
            });
        });

        textLayout.addView(title);
        textLayout.addView(progressBar);

        row.addView(fabBook);
        row.addView(textLayout);
        row.addView(fabMore);

        parent.addView(row);

        return parent;
    }

    private int dp(int pixels) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (pixels * scale + 0.5f);
    }

    private void renderTexts() {
        if (getContext() == null) return;
        
        View rootView = getView();
        if (rootView != null) {
            LinearLayout containerID = rootView.findViewById(R.id.container);
            if (containerID != null) {
                containerID.removeAllViews();
                
                for (TextInfo t : texts) {
                    containerID.addView(createTextItem(t));
                }
            }
        }
    }

    private void deleteText(Text book) {
        if (book.getIdText() == null) {
            Toast.makeText(requireContext(),
                "Error: Texto sin ID no se puede eliminar",
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        textRepository.deleteText(book.getIdText(), new TextRepository.OnDeleteCallback() {
            @Override
            public void onDeleteComplete(boolean success) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(requireContext(),
                                "Texto eliminado: " + book.getTitle(),
                                Toast.LENGTH_SHORT).show();
                            refreshTexts(); // Recargar desde BD
                        } else {
                            Toast.makeText(requireContext(),
                                "Error al eliminar texto",
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void openReadingScreen(long textId) {
        Intent intent = new Intent(requireContext(), ReadingActivity.class);
        intent.putExtra("text_id", textId);
        startActivity(intent);
    }

    private void navigateToEditFragment(Text book) {
        AddTextFragment editFragment = new AddTextFragment();
        
        Bundle args = new Bundle();
        args.putString("title", book.getTitle());
        args.putString("content", book.getText());
        args.putLong("id", book.getIdText());
        editFragment.setArguments(args);
        
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.replaceFragment(editFragment);
        }
        
        Toast.makeText(requireContext(),
                "Editando: " + book.getTitle(),
                Toast.LENGTH_SHORT).show();
    }

    private void checkForUpdates() {
        // Check for updated text (legacy support)
        if (MainActivity.textUpdateData.containsKey("update_id")) {
            long updateId = MainActivity.textUpdateData.getLong("update_id", -1);
            String updateTitle = MainActivity.textUpdateData.getString("update_title");
            String updateContent = MainActivity.textUpdateData.getString("update_content");
            
            // Update in database
            Text updateText = new Text();
            updateText.setIdText(updateId);
            updateText.setTitle(updateTitle);
            updateText.setText(updateContent);
            
            textRepository.updateText(updateText, new TextRepository.OnUpdateCallback() {
                @Override
                public void onUpdateComplete(boolean success) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(requireContext(), "Texto actualizado", Toast.LENGTH_SHORT).show();
                                refreshTexts(); // Reload from database
                            } else {
                                Toast.makeText(requireContext(), "Error al actualizar texto", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            
            MainActivity.textUpdateData.remove("update_id");
            MainActivity.textUpdateData.remove("update_title");
            MainActivity.textUpdateData.remove("update_content");
        }
        
        // Check for new text (legacy support)
        if (MainActivity.textUpdateData.containsKey("new_id")) {
            String newTitle = MainActivity.textUpdateData.getString("new_title");
            String newContent = MainActivity.textUpdateData.getString("new_content");
            
            Text newText = new Text();
            newText.setTitle(newTitle);
            newText.setText(newContent);
            
            textRepository.insertText(newText, new TextRepository.OnInsertCallback() {
                @Override
                public void onInsertComplete(boolean success, long id) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(requireContext(), "Texto agregado", Toast.LENGTH_SHORT).show();
                                refreshTexts(); // Reload from database
                            } else {
                                Toast.makeText(requireContext(), "Error al guardar texto", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            
            MainActivity.textUpdateData.remove("new_id");
            MainActivity.textUpdateData.remove("new_title");
            MainActivity.textUpdateData.remove("new_content");
        }
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheet);

        LinearLayout addTextLayout = dialog.findViewById(R.id.layout_add_text);
        LinearLayout addTextUrlLayout = dialog.findViewById(R.id.layout_add_text_url);
        LinearLayout  addTextPdfLayout = dialog.findViewById(R.id.layout_add_text_pdf);

        addTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                   activity.replaceFragment(new AddTextFragment());
                }
            }
        });

        addTextUrlLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.replaceFragment(new ImportUrlTextFragment());
                }
            }
        });

        addTextPdfLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.replaceFragment(new ImportPdfTextFragment());
                }
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void showEditDialog(Text book) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomssheet_edit_text);

        LinearLayout edit = dialog.findViewById(R.id.layout_edit_text);
        LinearLayout delete = dialog.findViewById(R.id.layout_delete_text);

        edit.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToEditFragment(book);
        });

        delete.setOnClickListener(v -> {
            dialog.dismiss();
            deleteText(book);
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }
}