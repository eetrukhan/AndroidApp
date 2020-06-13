package com.example.keyboardapp;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import static com.example.keyboardapp.Constants.HOST;
import static com.example.keyboardapp.Constants.PORT;
import static com.example.keyboardapp.Constants.TIME_CONSTANT;

public class MyAccessibilityService extends AccessibilityService {
    public static volatile Connection mConnect = null;
    public static Thread execThread = null;
    private volatile boolean stopLoop = false;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {

    }

    public void drawGesture(String[] data) {
        Log.i("Accessibility", "Start Drawing . . .");

        if (data == null || data.length < 2) {
            Log.i("Draw gesture", "Empty coords");
            return;
        }

        Path clickPath = new Path();

        clickPath.moveTo(
                Float.parseFloat(data[0]),
                Float.parseFloat(data[1]));

        for (int i = 2; i < data.length; i += 2) {
            clickPath.lineTo(
                    Float.parseFloat(data[i]),
                    Float.parseFloat(data[i + 1]));
        }
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, data.length * TIME_CONSTANT));
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //TODO КОСТЫЛЬ НЕ ЗНАЮ КАК ПРАВИЛЬНО ВЫКЛЮЧАТЬ ACCESSIBILITY
        if (execThread != null) {
            while(mConnect != null)
                stopLoop = true;
        }

        execThread = new Thread(() ->
        {
            while (!stopLoop) {
                if (mConnect == null) {
                    Log.i("Accessibility", "NO CONNECTION");

                    try {
                        Log.i("Connection", "Search for server ...");
                        while (HOST == null)
                            Broadcast.recieveBroadcast();

                        Log.i("Connection", "Connecting ...");
                        mConnect = null;
                        mConnect = new Connection(HOST, PORT);
                        mConnect.openConnection();
                        Log.i("Connection", "Server Connected!");

                    } catch (Exception e) {
                        Log.i("Connection", e.getMessage() == null ? "exception" : e.getMessage());
                    }

                    continue;
                }

                try {
                    drawGesture(mConnect.receiveData());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            execThread = null;
            mConnect.closeConnection();
            mConnect = null;
        }
        );
        execThread.start();
    }

}
