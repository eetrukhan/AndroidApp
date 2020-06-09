package com.example.keyboardapp;


import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private String HOST = "192.168.0.18";
    private int PORT = 8080;
    private Connection mConnect = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText text = (EditText) findViewById(R.id.editText);
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

                doSomething(s);
                String str = s.toString();

            }
        });
        mConnect = new Connection(HOST, PORT);
        // Открытие сокета в отдельном потоке
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(Connection.LOG_TAG, "Соединение");
                    mConnect.openConnection();
                    Log.d(Connection.LOG_TAG, "Соединение установлено");
                    Log.d(Connection.LOG_TAG, "(mConnect != null) = " + (mConnect != null));
                } catch (Exception e) {
                    Log.e(Connection.LOG_TAG, e.getMessage());
                    mConnect = null;
                }

                while (true)
                {
                    Log.d(Connection.LOG_TAG, "Попытка1");
                    assert mConnect != null;
                    Log.d(Connection.LOG_TAG, "Попытка2");
                    try {
                        mConnect.receiveData();
                    }catch (Exception ex)
                    {
                        Log.d("GG", ex.getMessage());
                        Log.d("GG", "NO");
                    }
                    Log.d(Connection.LOG_TAG, "Попытка3");
                    Log.d(Connection.LOG_TAG, "Попытка4");
                }
            }
        }).start();


    }


    public void doSomething(final CharSequence s) {
        final String str = s.toString();
        byte[] data = str.getBytes();
        if (mConnect == null) {
            Log.d(Connection.LOG_TAG, "Соединение не установлено");
        } else {
            Log.d(Connection.LOG_TAG, "Отправка сообщения");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String text = str;
                        if (text.trim().length() == 0)
                            text = "";

                        mConnect.sendData(text.getBytes());
                    } catch (Exception e) {
                        Log.e(Connection.LOG_TAG, e.getMessage());
                    }
                }
            }).start();
            TextView print = findViewById(R.id.textView);
            print.setText(s);

        }
    }

}
