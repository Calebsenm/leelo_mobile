package com.app.leelo;



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
import android.widget.Button;

import com.app.leelo.model.Text;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.Dialog;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class TextFragment extends Fragment {

    Text text;
    List<Text> texts = new ArrayList<>();

    public TextFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View thisFragmentView = inflater.inflate(R.layout.fragment_text, container, false);

        // Check for updates from AddTextFragment immediately
        checkForUpdates();

        FloatingActionButton practiceButton = thisFragmentView.findViewById(R.id.practice_button);
        FloatingActionButton addTextButton = thisFragmentView.findViewById(R.id.add_text_button);


        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.replaceFragment(new PracticeFragment());
                }

            }
        });
        addTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });




        //Creation text example:
        text = new Text();
        text.setIdText(10000L);
        text.setTittle("Mango is a fruit");
        text.setText("Mango is a fruir from america. mango is yellow ");

        Text text1 = new Text();
        text1.setIdText(20000L);
        text1.setTittle("Mango is a fruit");
        text1.setText("Mango is a fruit from America. Mango is yellow.");

        Text text2 = new Text();
        text2.setIdText(30000L);
        text2.setTittle("Apple is a fruit");
        text2.setText("Apple is red or green.");

        texts.add(text1);
        texts.add(text2);

        // Post to ensure view is fully created
        thisFragmentView.post(() -> {
            checkForUpdates();
            renderTexts();
        });

        return thisFragmentView;
    }

    private View createTextItem(Text t) {
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
            openReadingScreen(t);
        });

        LinearLayout textLayout = new LinearLayout(requireContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textLayout.setLayoutParams(textParams);
        textLayout.setPadding(dp(16), 0, dp(16), 0);

        TextView title = new TextView(requireContext());
        title.setText(t.getTittle());
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
            Text book = (Text) v.getTag();
            showEditDialog(book);
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
                
                for (Text t : texts) {
                    containerID.addView(createTextItem(t));
                }
            }
        }
    }

    private void deleteText(Text book) {
        texts.remove(book);
        renderTexts();
        Toast.makeText(requireContext(),
                "Texto eliminado: " + book.getTittle(),
                Toast.LENGTH_SHORT).show();
    }

    private void openReadingScreen(Text book) {
        Intent intent = new Intent(requireContext(), ReadingActivity.class);
        intent.putExtra("title", book.getTittle());
        intent.putExtra("text", book.getText());
        startActivity(intent);
    }

    private void navigateToEditFragment(Text book) {
        AddTextFragment editFragment = new AddTextFragment();
        
        Bundle args = new Bundle();
        args.putString("title", book.getTittle());
        args.putString("content", book.getText());
        args.putLong("id", book.getIdText());
        editFragment.setArguments(args);
        
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.replaceFragment(editFragment);
        }
        
        Toast.makeText(requireContext(),
                "Editando: " + book.getTittle(),
                Toast.LENGTH_SHORT).show();
    }

    private void checkForUpdates() {
        // Check for updated text
        if (MainActivity.textUpdateData.containsKey("update_id")) {
            long updateId = MainActivity.textUpdateData.getLong("update_id", -1);
            String updateTitle = MainActivity.textUpdateData.getString("update_title");
            String updateContent = MainActivity.textUpdateData.getString("update_content");
            
            for (Text text : texts) {
                if (text.getIdText() == updateId) {
                    text.setTittle(updateTitle);
                    text.setText(updateContent);
                    Toast.makeText(requireContext(), "Texto actualizado", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            
            MainActivity.textUpdateData.remove("update_id");
            MainActivity.textUpdateData.remove("update_title");
            MainActivity.textUpdateData.remove("update_content");
        }
        
        // Check for new text
        if (MainActivity.textUpdateData.containsKey("new_id")) {
            Text newText = new Text();
            newText.setIdText(MainActivity.textUpdateData.getLong("new_id", -1));
            newText.setTittle(MainActivity.textUpdateData.getString("new_title"));
            newText.setText(MainActivity.textUpdateData.getString("new_content"));
            texts.add(newText);
            Toast.makeText(requireContext(), "Texto agregado", Toast.LENGTH_SHORT).show();
            
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