package com.example.keyboardapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Connection {
    private Socket mSocket = null;
    private String mHost = null;
    private int mPort = 0;

    private static BufferedReader in;
    private static OutputStreamWriter out;

    private final String LOG_TAG = "SOCKET";

    public Connection() {
    }


    Connection(final String host, final int port) {
        this.mHost = host;
        this.mPort = port;
    }

    // Метод открытия сокета
    void openConnection() {
        try {
            // Создание сокета
            mSocket = new Socket(mHost, mPort);
            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            out = new OutputStreamWriter(mSocket.getOutputStream());
            Log.d(LOG_TAG, "socket opened");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод закрытия сокета
     */
    void closeConnection() {
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                sendData("\0");
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mSocket = null;
                in = null;
                out = null;
            }
            Log.i("CLOSE CONNECTION", "Connection Closed!");
        }
    }

    /**
     * Метод отправки данных
     */
    void sendData(String data) {
        if(out == null) {
            Log.i("SENT DATA", "null stream");
            return;
        }

        new Thread(() -> {
            try {
                out.write(data);
                out.flush();

                Log.i("SENT DATA", data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    String[] receiveData() {
        if(in == null) {
            Log.i("RECIEVED DATA", "null stream");
            return null;
        }
        try {
            Log.i("RECIEVED DATA", "Waiting for data . . .");
            String data = in.readLine();
            Log.d("RECIEVED DATA", data);
            return data.split(";");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}