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
import java.util.Date;
import java.util.Locale;


public class MainActivity extends Activity implements KeyboardHeightObserver {
    private final static String LOG_TAG = "Main Activity";

    private KeyboardHeightProvider keyboardHeightProvider;
    private WordPredictions wordPredictions = new WordPredictions();

    private boolean isKeyboardOpened = false;

    ArrayList<String> words = new ArrayList<String>();

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

        MyAccessibilityService.mainActivity = this;
        Connection.getInstance().loopConnection();
        MyAccessibilityService.isLooping = true;

        addTextEntryListener();

        Button button = findViewById(R.id.button);
        Button buttonScreen = findViewById(R.id.buttonScreen);
        button.setOnClickListener((e) -> {
            e.setClickable(false);
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            e.setClickable(true);
        });
        buttonScreen.setOnClickListener(v -> {
            Date now = new Date();
            android.text.format.DateFormat.format("yyyy-MM-dd_hh-mm-ss", now);
            String mPath = Environment.getExternalStorageDirectory().toString() + "/screen" + now.getTime() + ".jpg";

            try {
                Process proc = Runtime.getRuntime().exec("screencap -p " + mPath);
                proc.waitFor();
                InputStream error = proc.getErrorStream();
                for (int i = 0; i < error.available(); i++) {
                    System.out.println("" + error.read());
                }

                File f = new File(mPath);
                byte[] bytes = new byte[(int) f.length()];

                FileInputStream fis = new FileInputStream(f);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(bytes, 0, bytes.length);
                for(byte b: bytes)
                    Log.i("FILE", Byte.toString(b));
                f.delete();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void clearEditText() {
        runOnUiThread(() -> ((EditText) findViewById(R.id.editText)).setText(""));
    }

    public void sendScreenshot()
    {
        if (Connection.getInstance().isConnected() && isKeyboardOpened) {
            WordPredictions.verifyStoragePermissions(MainActivity.this);
            File screenshot = wordPredictions.takeScreenshot(MainActivity.this);
            new Thread(() -> {
                Connection.getInstance().sendFile(screenshot);
            }).start();
        }
        else
        {
            Toast.makeText(MainActivity.this, "Warning. No connection with server" +
                            "Screenshot",
                    Toast.LENGTH_SHORT).show();
        }
    }

    void addTextEntryListener() {
        EditText text = findViewById(R.id.editText);
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Connection.getInstance().isConnected() && isKeyboardOpened) {
                    new Thread(() -> {
                        Connection.getInstance().sendData(text.getText().toString());
                    }).start();
                } else
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
