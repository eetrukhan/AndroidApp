package com.example.keyboardapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Connection {
    private Socket mSocket = null;
    private String mHost = null;
    private int mPort = 0;
    private static BufferedReader in;
    public static float x;
    public static float y;
    public static String[] data;

    public static final String LOG_TAG = "SOCKET";

    public Connection() {
    }

    public Connection(final String host, final int port) {
        this.mHost = host;
        this.mPort = port;
    }

    // Метод открытия сокета
    public void openConnection() throws Exception {
        // Если сокет уже открыт, то он закрывается
        closeConnection();
        try {
            // Создание сокета
            mSocket = new Socket(mHost, mPort);
            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            Log.d("SOCKET", "Открыт");
        } catch (IOException e) {
            Log.e("socket", e.getMessage());
            throw new Exception("Невозможно создать сокет: "
                    + e.getMessage());
        }
    }

    /**
     * Метод закрытия сокета
     */
    public void closeConnection() {
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Ошибка при закрытии сокета :"
                        + e.getMessage());
            } finally {
                mSocket = null;
            }
        }
        mSocket = null;
    }

    /**
     * Метод отправки данных
     */
    public void sendData(byte[] data) throws Exception {
        // Проверка открытия сокета
        if (mSocket == null || mSocket.isClosed()) {
            throw new Exception("Ошибка отправки данных. " +
                    "Сокет не создан или закрыт");
        }
        // Отправка данных
        try {
            mSocket.getOutputStream().write(data);
            mSocket.getOutputStream().flush();
        } catch (IOException e) {
            throw new Exception("Ошибка отправки данных : "
                    + e.getMessage());
        }
    }

    public void receiveData() {
        try {
            Log.d(Connection.LOG_TAG, "Вошли");
            Log.d(Connection.LOG_TAG, "Читаем");
            data = in.readLine().split(";");
            Log.d(Connection.LOG_TAG, "Считали");
            for (int i = 0; i < data.length; i++)
                Log.d(Connection.LOG_TAG, data[i]);
            Log.d(Connection.LOG_TAG, "Размер массива: "+data.length);
            Log.d(Connection.LOG_TAG, "Вывели");
        } catch (Exception ex) {
            Log.d(Connection.LOG_TAG, "Упало");
        }

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeConnection();
    }
}