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

import io.timmi.de4lroadtracker.helper.Md5Builder;
import io.timmi.de4lroadtracker.helper.RawResourceLoader;
import us.feras.mdv.MarkdownView;

public class PrivacyAgreementActivity extends AppCompatActivity {

    private String agreementText = null;

    private boolean didAgree() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.contains("agreedPrivacyMD5") && agreementText != null) {
            try {
                return Md5Builder.md5(agreementText).equals(sharedPreferences.getString("agreedPrivacyMD5", ""));
            } catch (Exception e) { }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        agreementText = RawResourceLoader.loadText(R.raw.privacy, this);
        setContentView(R.layout.activity_privacy_agreement);
        MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdownView);
        markdownView.loadMarkdown(agreementText);
        CheckBox agreePrivacyCheckbox = (CheckBox) findViewById(R.id.agreeCheckbox);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(didAgree()) {
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
        String md5 = Md5Builder.md5(agreementText);
        sharedPreferencesEditor.putBoolean("hasPrivacyAgreement", ((CheckBox)checkBox).isChecked());
        sharedPreferencesEditor.putString("agreedPrivacyMD5", md5);
        sharedPreferencesEditor.apply();

    }
}