package com.example.keyboardapp;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.net.SocketException;

public class MyAccessibilityService extends AccessibilityService {
    public static volatile boolean isLooping = true;

    private final int TIME_CONSTANT = 7; //коэффицент отвечающий за время отрисовки жеста

    public static void DisableService() {
        isLooping = false;
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        loopReceiving();
    }

    synchronized void loopReceiving() {
        new Thread(() ->
        {
            while (isLooping) {
                if (Connection.getInstance().isConnected())
                    drawGesture(Connection.getInstance().receiveData());
            }
            disableSelf();
        }).start();
    }

    public void drawGesture(String[] data) {
        Log.i("Accessibility", "Start Drawing ...");

        if (data == null || data.length < 2) {
            Log.i("Accessibility", "Empty (x,y) array.");
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

        Log.i("Accessibility", "Successful drawn.");
    }

}
