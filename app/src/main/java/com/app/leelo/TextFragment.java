package com.app.leelo;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TextFragment extends Fragment {

    public TextFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View thisFragmentView = inflater.inflate(R.layout.fragment_text, container, false);

        FloatingActionButton button = thisFragmentView.findViewById(R.id.practice_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.replaceFragment(new PracticeFragment());
                }

            }
        });

        return thisFragmentView;
    }
}