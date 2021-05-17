package io.timmi.de4lroadtracker;

import android.app.ActionBar;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.textfield.TextInputLayout;

public class PasswordPreference extends Preference {

    @Nullable
    private EditText passw = null;

    public PasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        passw = holder.itemView.findViewById(R.id.editMqttPW);
        passw.setText(getPersistedString(""));
    }

    @Override
    public void onDetached() {
        String password = passw.getText().toString();
        persistString(password);
    }
}
