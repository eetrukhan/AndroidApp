package com.example.keyboardapp;


import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import android.os.Bundle;

import static com.example.keyboardapp.Constants.HOST;
import static com.example.keyboardapp.Constants.PORT;

public class MainActivity extends Activity {
    public static Connection mConnect = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnect.closeConnection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);

        new Thread(() -> {
            while (HOST == null)
                Broadcast.recieveBroadcast();
            addTextEntryListener();
        }).start();
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
                if (mConnect == null) {
                    new Thread(() -> {
                        if (mConnect == null) {
                            mConnect = new Connection(HOST, PORT);
                            mConnect.openConnection();
                        }
                    }).start();
                } else
                    mConnect.sendData(text.getText().toString() + '\0');
            }
        });
    }
}
