package com.example.keyboardapp;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.text.NumberFormat;

import static com.example.keyboardapp.Constants.HOST;
import static com.example.keyboardapp.Constants.PORT;
import static com.example.keyboardapp.Constants.TIME_CONSTANT;

public class MyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {

    }

    public void drawGesture(String[] data) {
        Log.i("Accesability", "Start Drawing . . .");

        if(data == null || data.length < 2) {
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
        new Thread(() ->
        {
            while (HOST == null)
                Broadcast.recieveBroadcast();
            while (true) {
                if (MainActivity.mConnect == null) {
                    MainActivity.mConnect = new Connection(HOST, PORT);
                    MainActivity.mConnect.openConnection();
                }

                if (MainActivity.mConnect == null)
                    continue;

                try {
                    drawGesture(MainActivity.mConnect.receiveData());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        ).start();
    }
}
