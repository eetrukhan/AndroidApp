package com.example.keyboardapp;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Debug;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Button;
import android.widget.EditText;


import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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


public class MainActivity extends AppCompatActivity implements KeyboardHeightObserver {
    private final static String LOG_TAG = "Main Activity";

    private KeyboardHeightProvider keyboardHeightProvider;
    private WordPredictions wordPredictions = new WordPredictions();

    private boolean isKeyboardOpened = false;
    MyAccessibilityService service = new MyAccessibilityService();

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
        MyAccessibilityService.isLooping = true;

        Connection.getInstance().loopConnection();
        ConnectionSwypeHelper.getInstance().loopConnection();

        addTextEntryListener();

        service.onServiceConnected();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.ABCD:
                Log.d("MENU", "selected switch ABCD");
//                new Thread(() -> {
//                    Connection.getInstance().sendData("switchGET#");
//                }).start();
                return true;
            case R.id.scale_A:
                Log.d("MENU", "selected A");
                new Thread(() -> {
                    Connection.getInstance().sendData("switchA#");
                }).start();
                return true;
            case R.id.scale_B:
                Log.d("MENU", "selected B");
                new Thread(() -> {
                    Connection.getInstance().sendData("switchB#");
                }).start();
                return true;
            case R.id.scale_C:
                Log.d("MENU", "selected C");
                new Thread(() -> {
                    Connection.getInstance().sendData("switchC#");
                }).start();
                return true;
            case R.id.scale_D:
                Log.d("MENU", "selected D");
                new Thread(() -> {
                    Connection.getInstance().sendData("switchD#");
                }).start();
                return true;
            case R.id.scale_Train:
                Log.d("MENU", "selected Train");
                new Thread(() -> {
                    Connection.getInstance().sendData("switchTrain#");
                }).start();
                return true;
            case R.id.Re_entry:
                Log.d("MENU", "selected re-entry");
                AlertAndSendToServer("Re-entry sentence",
                        "Do you really want to re-entry attempt?",
                        "restart#");
                return true;
            case R.id.Re_generate:
                Log.d("MENU", "selected re-generate");
                AlertAndSendToServer("Re-generate sentences",
                        "Do you really want to re-generate dictionary?",
                        "regenerate#");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void AlertAndSendToServer(String Title, String Message, String Data)
    {
        new AlertDialog.Builder(this)
                .setTitle(Title)
                .setMessage(Message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new Thread(() -> {
                            Connection.getInstance().sendData(Data);
                        }).start();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
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

    public void backspace() {
        runOnUiThread(() -> {
            EditText et = findViewById(R.id.editText);
            Editable res;

            int i = et.getText().length() - 1;

            if (i == -1)
                return;
            if (et.getText().charAt(i) == ' ')
                --i;

            while (i >= 0 && et.getText().charAt(i) != ' ')
                --i;
            res = et.getText().delete(i + 1, et.getText().length());

            et.setText(res.toString());

            et.setSelection(et.getText().length());

            new Thread(() -> {
                Connection.getInstance().sendData(et.getText().toString() + "#");
            }).start();

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
                new Thread(() -> {
                    Connection.getInstance().sendData(s.toString() + "#");
                }).start();
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
