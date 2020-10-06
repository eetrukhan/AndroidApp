package com.example.keyboardapp;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MyAccessibilityService extends AccessibilityService {
    public static MainActivity mainActivity;

    public static volatile boolean isLooping = true;

    private final int TIME_CONSTANT = 5; //коэффицент отвечающий за время отрисовки жеста

    public static void DisableService() {
        isLooping = false;
    }

    Executor executor = Executors.newSingleThreadExecutor();

    long predictionsClickedTime;

    volatile boolean WaitDrawEnd = false;
    AtomicInteger forceWaitStopCounter = new AtomicInteger(0);

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        mainActivity.service = this;
        loopReceiving();
        loopReceivingDoubleClick();
    }

    public void predictionsDoubleClick() {
        int x1 = (int) (KeyboardHeightProvider.width / 4);
        int x2 = (int) (KeyboardHeightProvider.width * 3 / 4);
        int y = (int) (KeyboardHeightProvider.height - KeyboardHeightProvider.keyboard_height + 60);
        Log.i("Double click", "Enter Method");
        Path clickPath1 = new Path();
        clickPath1.moveTo(x1, y);
        GestureDescription.StrokeDescription clickStroke1 = new GestureDescription.StrokeDescription(clickPath1, 50, 50);


        Path clickPath3 = new Path();
        clickPath3.moveTo(x2, y);
        GestureDescription.StrokeDescription clickStroke3 = new GestureDescription.StrokeDescription(clickPath3, 50, 50);
        GestureDescription.Builder clickBuilder3 = new GestureDescription.Builder();
        clickBuilder3.addStroke(clickStroke3);
        clickBuilder3.addStroke(clickStroke1);

        dispatchGesture(clickBuilder3.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                new Thread(() -> mainActivity.sendPredictions()).start();
                Log.i("Double click", "Completed Gesture");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.i("Double click", "Canceled Gesture");
                WaitDrawEnd = false;
            }
        }, null);


    }


    void loopReceiving() {
        executor.execute(() ->
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
                        } else if (receivedData[0].equals("u")) {
                            Log.i("Accessibility", "draw case enter . . .");
                            int current = forceWaitStopCounter.get();
                            Log.i("Gesture", String.format("%d starts", current));
                            //new Thread(() -> drawGesture(receivedData)).start();
                        }
                    }
                }
            }
            disableSelf();
        });
    }

    void loopReceivingDoubleClick() {
        new Thread(() ->
        {
            String[] gesture;
            Log.i("Thread 1", "Started");
            while (true) {
                ConnectionSwypeHelper.getInstance().receiveData();
            }
        }).start();
    }

    void windowViewTree() {
        AccessibilityWindowInfo wInfo = AccessibilityWindowInfo.obtain();

        wInfo.describeContents();


    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }


    public void drawGesture(String[] data) {
        Log.i("Accessibility", "Start Drawing ...");

        if (data == null || data.length < 2) {
            Log.i("Accessibility", "Empty (x,y) array.");
            WaitDrawEnd = false;
            return;
        }
        ArrayList<Float> gestureData;

        gestureData = fixGesture(data);

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

            int gestureMicros = Math.max(gestureData.size() * TIME_CONSTANT, 150);

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, gestureMicros));
            dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    predictionsDoubleClick();
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    WaitDrawEnd = false;
                }
            }, null);
        } else {
            WaitDrawEnd = false;
        }
        Log.i("Accessibility", "Successful drawn.");
    }

    public ArrayList<Float> fixGesture(String[] data) {
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
                   // (fixed_data.get(i + 1) > (KeyboardHeightProvider.height - KeyboardHeightProvider.keyboard_height * 0.20875) && (fixed_data.get(i) < KeyboardHeightProvider.width * 0.31 || fixed_data.get(i) > KeyboardHeightProvider.width * 0.745))) {
                    (fixed_data.get(i + 1) > (KeyboardHeightProvider.height - KeyboardHeightProvider.keyboard_height * 0.20875))||
                    ((fixed_data.get(i)<KeyboardHeightProvider.width*0.1||fixed_data.get(i)>KeyboardHeightProvider.width*0.9) && fixed_data.get(i+1)>KeyboardHeightProvider.height - KeyboardHeightProvider.keyboard_height*0.4170 )) { // Backspace
                    fixed_data.remove(i + 1);
                fixed_data.remove(i);
            } else
                i += 2;

        }


        for (int l = 0; l < fixed_data.size(); l++)
            Log.i("fixed point ", fixed_data.get(l).toString());


        return fixed_data;

    }

}
