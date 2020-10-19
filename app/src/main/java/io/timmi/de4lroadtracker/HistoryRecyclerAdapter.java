package io.timmi.de4lroadtracker;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HistoryRecyclerAdapter extends RecyclerView.Adapter<HistoryRecyclerAdapter.ItemViewHolder> {

    ArrayList<String> historyMessageList;

    public HistoryRecyclerAdapter(ArrayList<String> _historyMessageList) {
        super();
        historyMessageList = _historyMessageList;
    }

    public void add(String message) {
        historyMessageList.add(message);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryRecyclerAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.i("HistoryRecyclerAdapder", "[onCreateViewHolder]");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_items, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Log.i("HistoryRecyclerAdapter", "[onBindViewHolder]");
        holder.itemView.setText(historyMessageList.get(position));
    }

    @Override
    public int getItemCount() {
        Log.i("HistoryRecyclerAdapter", "[getItemCount]");
        return historyMessageList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView itemView;
        public ItemViewHolder(@NonNull View _view) {
            super(_view);
            itemView = (TextView) _view.findViewById(R.id.row_text);
        }
    }
}
