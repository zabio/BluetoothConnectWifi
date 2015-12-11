package com.hy.bluetoothconnectwifi.wifi;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by henry  15/12/3.
 */
public class UIUtils {
    public static int getWindowWidth(Context context) {
        int width;
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        try {
            if (Build.VERSION.SDK_INT < 13) {
                DisplayMetrics displaymetrics = new DisplayMetrics();
                display.getMetrics(displaymetrics);
                width = displaymetrics.widthPixels;
            } else {
                Point size = new Point();
                display.getSize(size);
                width = size.x;
            }
        } catch (NoSuchMethodError e) {
            try {
                width = display.getWidth();
            } catch (Exception ex) {
                width = 200;
            }
        }
        return width;
    }

    public static int getWindowHeight(Context context) {
        int height;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        context.getApplicationContext().getResources().getDisplayMetrics();
        Display display = windowManager.getDefaultDisplay();
        try {
            if (Build.VERSION.SDK_INT < 13) {
                DisplayMetrics displaymetrics = new DisplayMetrics();
                display.getMetrics(displaymetrics);
                height = displaymetrics.heightPixels;
            } else {
                Point size = new Point();
                display.getSize(size);
                height = size.y;
            }
        } catch (NoSuchMethodError e) {
            try {
                height = display.getHeight();
            } catch (Exception ex) {
                height = 200;
            }
        }
        return height;
    }

}
