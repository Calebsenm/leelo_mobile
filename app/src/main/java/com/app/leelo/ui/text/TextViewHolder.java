package com.app.leelo.ui.text;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.leelo.R;

public class TextViewHolder extends RecyclerView.ViewHolder {

    TextView titleView;

    public TextViewHolder(@NonNull View itemView) {
        super(itemView);
        titleView = itemView.findViewById(R.id.textTitle);
    }
}
