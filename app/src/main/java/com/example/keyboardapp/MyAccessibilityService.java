package com.example.keyboardapp;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class MyAccessibilityService extends AccessibilityService {
    public static MainActivity mainActivity;

    public static volatile boolean isLooping = true;

    private final int TIME_CONSTANT = 5; //коэффицент отвечающий за время отрисовки жеста

    public static void DisableService() {
        isLooping = false;
    }

    long predictionsClickedTime;

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        mainActivity.service = this;
        loopReceiving();
    }

    void predictionsDoubleClick() {
        int x1 = (int) (KeyboardHeightProvider.width/4);
        int x2 = (int)(KeyboardHeightProvider.width*3/4);
        int y = (int)(KeyboardHeightProvider.height-KeyboardHeightProvider.keyboard_height+60);
       Log.i("Double click", "Enter Method");
        Path clickPath1 = new Path();
        clickPath1.moveTo(x1, y);
        GestureDescription.StrokeDescription clickStroke1 = new GestureDescription.StrokeDescription(clickPath1, 30, 50);


        Path clickPath3 = new Path();
        clickPath3.moveTo(x2, y);
        GestureDescription.StrokeDescription clickStroke3 = new GestureDescription.StrokeDescription(clickPath3, 30, 50);
        GestureDescription.Builder clickBuilder3 = new GestureDescription.Builder();
        clickBuilder3.addStroke(clickStroke3);
        clickBuilder3.addStroke(clickStroke1);

        dispatchGesture(clickBuilder3.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                new Thread(() -> mainActivity.sendPredictions()).start();
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }
        }, null);


    }


    void loopReceiving() {

        new Thread(() ->
        {
            String[] gesture;
            Log.i("Thread 1", "Started");
            while (isLooping) {
                if (Connection.getInstance().isConnected()) {
                    String[] receivedData = Connection.getInstance().receiveData();
                    if (mainActivity != null && receivedData != null &&
                            receivedData.length > 0) {
                        Log.i("Accessibility", "choose case enter . . .");

                        if (receivedData[0].equals("clear")) {
                            Log.i("Accessibility", "clear case enter . . .");
                            mainActivity.clearEditText();
                        } else if (receivedData[0].equals("screenshot")) {
                            Log.i("Accessibility word parse", "sent screenshot");
                            WordPredictions.verifyStoragePermissions(mainActivity);
                            mainActivity.sendScreenshot();
                        } else {
                            Log.i("Accessibility", "draw case enter . . .");
                            while(WaitDrawEnd);
                            drawGesture(receivedData);
                        }
                    }
                }
            }
            disableSelf();
        }).start();
    }

    void windowViewTree() {
        AccessibilityWindowInfo wInfo = AccessibilityWindowInfo.obtain();

        wInfo.describeContents();


    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i("Accessibility event", "Happened");

        AccessibilityNodeInfo mSource = event.getSource();
        int child = mSource.getChildCount();
        // iterate through all child of parent view
        for (int i = 0; i < child; i++) {
            AccessibilityNodeInfo childNodeView = mSource.getChild(i);
            Log.i("Accessibility Tree", childNodeView.getClassName().toString());
        }
    }

    boolean WaitDrawEnd = false;

    public void drawGesture(String[] data) {
        Log.i("Accessibility", "Start Drawing ...");

        if (data == null || data.length < 2) {
            Log.i("Accessibility", "Empty (x,y) array.");
            return;
        }
        ArrayList<Float> gestureData;

        gestureData = fixDesture(data);

        Path clickPath = new Path();

        if (gestureData.size() != 0) {
            clickPath.moveTo(
                    gestureData.get(0),
                    gestureData.get(1));

            for (int i = 2; i < gestureData.size() - 1; i += 2) {
                clickPath.lineTo(
                        gestureData.get(i),
                        gestureData.get(i + 1));
            }
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, gestureData.size() * TIME_CONSTANT));
            WaitDrawEnd = true;
            dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                }
            }, null);
        }
        Log.i("Accessibility", "Successful drawn.");
    }

    public ArrayList<Float> fixDesture(String[] data) {
        ArrayList<Float> fixed_data = new ArrayList<Float>();
        for (int i = 0; i < data.length; i++) {
            if (!((i + 1) % 6 == 0 || (i + 1) % 6 == 5)) {
                data[i] = data[i].replace(",", ".");
                fixed_data.add(Float.parseFloat(data[i]));
            }

        }


        int i = 0;
        while (i < fixed_data.size() - 1) {
            if (fixed_data.get(i) < 0 || fixed_data.get(i) > KeyboardHeightProvider.width || fixed_data.get(i + 1) < 0 || fixed_data.get(i + 1) > KeyboardHeightProvider.height ||
                    (fixed_data.get(i + 1) > (KeyboardHeightProvider.height - KeyboardHeightProvider.keyboard_height * 0.20875) && (fixed_data.get(i) < KeyboardHeightProvider.width * 0.31 || fixed_data.get(i) > KeyboardHeightProvider.width * 0.745))) {
                fixed_data.remove(i + 1);
                fixed_data.remove(i);
            } else
                i += 2;

        }


        for (int l = 0; l < fixed_data.size(); l++)
            Log.i(" i ", fixed_data.get(l).toString());


        return fixed_data;

    }

}
