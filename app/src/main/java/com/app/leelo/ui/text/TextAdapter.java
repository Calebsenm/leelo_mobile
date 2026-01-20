package com.app.leelo.ui.text;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.app.leelo.R;
import com.app.leelo.model.TextInfo;

import java.util.ArrayList;
import java.util.List;

public class TextAdapter extends RecyclerView.Adapter<TextAdapter.TextViewHolder> {

    private List<TextInfo> items = new ArrayList<>();
    private OnItemClickListener listener;
    private OnMenuClickListener menuListener;

    public interface OnItemClickListener {
        void onItemClick(TextInfo textInfo);
    }

    public interface OnMenuClickListener {
        void onMenuClick(TextInfo textInfo);
    }

    public TextAdapter(OnItemClickListener listener, OnMenuClickListener menuListener) {
        this.listener = listener;
        this.menuListener = menuListener;
    }

    public void setItems(List<TextInfo> texts) {
        this.items = texts;
        notifyDataSetChanged();
    }

    @Override
    public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_text, parent, false);
        return new TextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TextViewHolder holder, int position) {
        TextInfo info = items.get(position);
        holder.bind(info, listener, menuListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        ImageButton menuButton;
        ProgressBar readingProgress;

        TextViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.textTitle);
            menuButton = itemView.findViewById(R.id.menuButton);
            readingProgress = itemView.findViewById(R.id.readingProgress);
        }

        void bind(TextInfo info, OnItemClickListener listener, OnMenuClickListener menuListener) {
            titleView.setText(info.titulo);
            itemView.setOnClickListener(v -> listener.onItemClick(info));
            menuButton.setOnClickListener(v -> menuListener.onMenuClick(info));
            readingProgress.setProgress(0);
        }
    }
}
