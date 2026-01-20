package com.app.leelo.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.leelo.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class PracticeFragment extends Fragment {

    public PracticeFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_practice, container, false);

        FloatingActionButton practiceButton = view.findViewById(R.id.practice_button);
        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.replaceFragment(new PracticeFragment());
                }
            }
        });
        return view;
    }
}