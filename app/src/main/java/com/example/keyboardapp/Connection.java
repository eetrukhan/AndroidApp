package com.example.keyboardapp;

import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

class Connection {
    private final String LOG_TAG = "Connection";
    private static Connection Instance;


    private Socket mSocket = null;
    private int mPort = 1488;
    private Broadcast broadcast = new Broadcast(9876);

    private volatile BufferedReader in;
    private volatile OutputStreamWriter out;

    private volatile boolean isConnected;

    private volatile boolean doConnect = true;

    private Connection(final int port) {
        this.mPort = port;
    }

    boolean isConnected() {
        return isConnected;
    }

    void Disconnect() {
        doConnect = false;
    }

    static Connection getInstance() {
        if (Instance == null)
            Instance = new Connection(1488);
        return Instance;
    }

    void loopConnection() {
        new Thread(() -> {
            while (doConnect)
                openConnection();
            closeConnection();
        }).start();
    }

    private void openConnection() {
        if (isConnected)
            return;

        isConnected = false;

        try {
            Log.i(LOG_TAG, "Search for server ...");
            while (!broadcast.isPacketReceived())
                broadcast.recieveBroadcast();

            Log.i(LOG_TAG, "Connecting to " + broadcast.getHost());
            mSocket = new Socket(broadcast.getHost(), mPort);
            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            out = new OutputStreamWriter(mSocket.getOutputStream());

            Log.i(LOG_TAG, "Server Connected.");
            isConnected = true;
        } catch (Exception e) {
            Log.i(LOG_TAG, e.getMessage() == null ? "Error" : e.getMessage());
        }
    }


    private void closeConnection() {
        if (!isConnected)
            return;

        try {
            sendData("\0");
            mSocket.close();
            isConnected = false;
            mSocket = null;
            in = null;
            out = null;
            Instance = null;
        } catch (IOException e) {
            Log.i(LOG_TAG, e.getMessage() == null ? "Connection not closed." : e.getMessage());
        }
        Log.i(LOG_TAG, "Connection Closed.");
    }

    void sendData(String data) {
        if (!isConnected)
            return;

        try {

            String[] words = data.split(" ");
            if(words[words.length - 1].length() == 1)
                data += ' ';

            out.write(data);
            out.flush();

            Log.i(LOG_TAG, "Sent: " + data);
        } catch (IOException e) {
            isConnected = false;
            Log.i(LOG_TAG, "Can't send: " + data);
            Log.i(LOG_TAG, e.getMessage() == null ? "Can't send: " + data : e.getMessage());
        }
    }

    String[] receiveData() {
        if (!isConnected)
            return null;

        Log.i(LOG_TAG, "Waiting for gestures . . .");

        try {
            String data = in.readLine();

            if (data == null)
                throw new IOException("Disconnected from server");

            Log.d(LOG_TAG, data);
            return data.split(";");
        } catch (IOException e) {
            isConnected = false;
            Log.i(LOG_TAG, e.getMessage() == null ? "Can't receive data from server." : e.getMessage());
            return null;
        }
    }

}