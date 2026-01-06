package com.app.leelo;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.Dialog;
import android.widget.LinearLayout;

public class TextFragment extends Fragment {

    public TextFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View thisFragmentView = inflater.inflate(R.layout.fragment_text, container, false);

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

        return thisFragmentView;
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

}