package com.example.keyboardapp;


import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;


import android.os.Bundle;
import android.widget.Toast;

import static com.example.keyboardapp.MyAccessibilityService.mConnect;

public class MainActivity extends Activity {

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addTextEntryListener();
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
                if (mConnect != null)
                    mConnect.sendData(text.getText().toString() + '\0');
                else
                    Toast.makeText(MainActivity.this, "Turn On Accessibility Service!", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
