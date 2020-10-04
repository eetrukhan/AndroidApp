package com.example.keyboardapp;


import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Debug;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Button;
import android.widget.EditText;


import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity implements KeyboardHeightObserver {
    private final static String LOG_TAG = "Main Activity";

    private KeyboardHeightProvider keyboardHeightProvider;
    private WordPredictions wordPredictions = new WordPredictions();

    private boolean isKeyboardOpened = false;
    MyAccessibilityService service;

    int tc_counter = 0;
    ArrayList<String> words = new ArrayList<String>();

    List<String> predictions = Collections.synchronizedList(new ArrayList<>());
    long start;

    Thread sendThread;
    boolean isPredictionValid;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardHeightProvider.close();
        Connection.getInstance().Disconnect();
        ConnectionSwypeHelper.getInstance().Disconnect();
        MyAccessibilityService.DisableService();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        predictions.add("");
        predictions.add("");
        predictions.add("");

        keyboardHeightProvider = new KeyboardHeightProvider(this);

        View view = findViewById(R.id.activitylayout);
        view.post(() -> keyboardHeightProvider.start());

        MyAccessibilityService.mainActivity = this;
        Connection.getInstance().loopConnection();
        ConnectionSwypeHelper.getInstance().loopConnection();
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

    public void clearEditText() {
        runOnUiThread(() -> {
            ((EditText) findViewById(R.id.editText)).setText("");
            int curr = service.forceWaitStopCounter.get();
            service.WaitDrawEnd = false;
            Log.i("Gesture", String.format("%d done", curr));

            Log.i("service", "WaitDrawEnt = FALSE");
        });
    }

    public void sendScreenshot() {
        if (Connection.getInstance().isConnected() && isKeyboardOpened) {
            WordPredictions.verifyStoragePermissions(MainActivity.this);
            File screenshot = wordPredictions.takeScreenshot(MainActivity.this);
            new Thread(() -> {
                Connection.getInstance().sendFile(screenshot);
            }).start();
        } else {
            Toast.makeText(MainActivity.this, "Warning. No connection with server" +
                            "Screenshot",
                    Toast.LENGTH_SHORT).show();
        }
    }

    void addTextEntryListener() {
        EditText textEdit = findViewById(R.id.editText);
        textEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                new Thread(() -> {Connection.getInstance().sendData(s.toString() + "\r\n");}).start();
            }
        });
    }

    public void sendPredictions() {
        if (!Connection.getInstance().isConnected() || !isKeyboardOpened)
            return;

        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        String temp = predictions.get(0) + ";" + predictions.get(1) + ";" + predictions.get(2);



        predictions.set(0, "");
        predictions.set(1, "");
        predictions.set(2, "");

        runOnUiThread(() -> {

            TextView tw = findViewById(R.id.textView);
            tw.setText(isPredictionValid ? temp : "ОШИБКА;ОШИБКА;ОШИБКА");
        });

        if (Connection.getInstance().isConnected() && isKeyboardOpened) {
            sendThread = new Thread(() -> {
                Connection.getInstance().sendData(isPredictionValid ? temp : "ОШИБКА;ОШИБКА;ОШИБКА");
                clearEditText();
            });
            sendThread.start();
        } else
            Toast.makeText(MainActivity.this,
                    "Warning. No connection with server or reopen keyboard view",
                    Toast.LENGTH_SHORT).show();
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
    public void onKeyboardSizeChanged(int height_keyboard, int height, int width) {
        Log.i(LOG_TAG, "onKeyboardHeightChanged in pixels: " + height);

        TextView tv = findViewById(R.id.height_text);
        tv.setText(String.format(Locale.ENGLISH, "%d %d : %d", height, width, height_keyboard));
        if (Connection.getInstance().isConnected() && !isKeyboardOpened) {
            new Thread(() -> Connection.getInstance()
                    .sendData(String.format(Locale.ENGLISH, "%d %d %d", height, width, height_keyboard)))
                    .start();
            isKeyboardOpened = true;
        }
    }
}
