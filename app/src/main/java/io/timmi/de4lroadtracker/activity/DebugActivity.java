package io.timmi.de4lroadtracker.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import io.timmi.de4lroadtracker.HistoryRecyclerAdapter;
import io.timmi.de4lroadtracker.R;

public class DebugActivity extends AppCompatActivity {

    private static final String TAG = "DE4LDebugActivity";
    public static final String HISTORY_MESSAGE_BROADCAST = "io.timmi.de4lroadtracker.historymessagebroadcast";
    @Nullable
    private HistoryRecyclerAdapter historyRecyclerAdapter = null;

    private class HistoryBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (historyRecyclerAdapter == null) return;
            String historyMessage = intent.getStringExtra("historyMessage");
            if (historyMessage == null) {
                return;
            }
            Log.i(TAG, "[messageBroadcastReceiver.onReceive]");
            historyRecyclerAdapter.add(historyMessage);
            //RecyclerView rView = (RecyclerView) MainActivity.this.findViewById(R.id.mqttHistory);

        }
    }

    final private BroadcastReceiver receiver = new HistoryBroadcastReceiver();

    private void registerBroadcastReceiver(String broadcastMessageId) {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(broadcastMessageId);
            registerReceiver(receiver, intentFilter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        ArrayList<String> preList = new ArrayList<>();
        historyRecyclerAdapter = new HistoryRecyclerAdapter(preList);
        RecyclerView rView = (RecyclerView) DebugActivity.this.findViewById(R.id.mqttHistory);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rView.setLayoutManager(llm);
        rView.setAdapter(historyRecyclerAdapter);
        registerBroadcastReceiver(HISTORY_MESSAGE_BROADCAST);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}