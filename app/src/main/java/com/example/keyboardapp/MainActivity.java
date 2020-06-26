package com.example.keyboardapp;


import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;


public class MainActivity extends Activity implements KeyboardHeightObserver {
    private final static String LOG_TAG = "Main Activity";

    private KeyboardHeightProvider keyboardHeightProvider;

    private boolean isKeyboardOpened = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardHeightProvider.close();
        Connection.getInstance().Disconnect();
        MyAccessibilityService.DisableService();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        keyboardHeightProvider = new KeyboardHeightProvider(this);

        View view = findViewById(R.id.activitylayout);
        view.post(() -> keyboardHeightProvider.start());

        Connection.getInstance().loopConnection();
        MyAccessibilityService.isLooping = true;

        addTextEntryListener();

        Button button = findViewById(R.id.button);
        button.setOnClickListener((e) -> {
            e.setClickable(false);
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            e.setClickable(true);
        });

    }

    void addTextEntryListener() {
        EditText text = findViewById(R.id.editText);
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Connection.getInstance().isConnected() && isKeyboardOpened)
                    new Thread(() -> Connection.getInstance().sendData(text.getText().toString() + '\0')).start();
                else
                    Toast.makeText(MainActivity.this, "Warning. No connection with server" +
                            " or can't determine keyboard size(reopen keyboard view for retry.).",
                            Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
        keyboardHeightProvider.setKeyboardHeightObserver(null);
    }


    @Override
    public void onResume() {
        super.onResume();
        keyboardHeightProvider.setKeyboardHeightObserver(this);
    }


    @Override
    public void onKeyboardSizeChanged(int height, int width) {
        Log.i(LOG_TAG, "onKeyboardHeightChanged in pixels: " + height);

        TextView tv = findViewById(R.id.height_text);
        tv.setText(String.format(Locale.ENGLISH,"%d %d",height, width));
        if(Connection.getInstance().isConnected() && !isKeyboardOpened)
        {
            new Thread(() ->Connection.getInstance()
                    .sendData(String.format(Locale.ENGLISH,"%d %d",height, width)))
                    .start();
            isKeyboardOpened = true;
        }
    }
}
