package io.timmi.de4lroadtracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import io.timmi.de4lroadtracker.helper.RawResourceLoader;
import us.feras.mdv.MarkdownView;

public class PrivacyAgreementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_agreement);
        MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdownView);
        markdownView.loadMarkdown(RawResourceLoader.loadText(R.raw.privacy, this));
        CheckBox agreePrivacyCheckbox = (CheckBox) findViewById(R.id.agreeCheckbox);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean("hasPrivacyAgreement", false)) {
           agreePrivacyCheckbox.setChecked(true);
        }
    }

    public void closeActivity(MenuItem item) {
        finish();
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.privacy_agreeement_activity, menu);
        return true;
    }

    public void handleClickAgreeCheckbox(View checkBox) {
        //Log.d("Privacy", String.valueOf(((CheckBox)checkBox).isChecked()));
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        sharedPreferencesEditor.putBoolean("hasPrivacyAgreement", ((CheckBox)checkBox).isChecked());
        sharedPreferencesEditor.apply();

    }
}