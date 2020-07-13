package com.youdeyi.newdrugdevice.common.utils;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.youdeyi.newdrugdevice.common.app.MyApplication;


public class ToastUtils {

    public static void show(final int resId) {
        Toast.makeText(MyApplication.getInstance(), resId, Toast.LENGTH_SHORT).show();
    }

    public static void show(final String message) {
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(MyApplication.getInstance(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void show(final Activity activity, final int resId, final int duration) {
        if (activity == null)
            return;

        final Context context = activity.getApplication();
        activity.runOnUiThread(new Runnable() {
            public void run() {
              Toast.makeText(context, resId, duration).show();
            }
        });
    }

    public static void show(final Activity activity, final String message, final int duration) {
        if (activity == null)
            return;
        if (TextUtils.isEmpty(message))
            return;

        final Context context = activity.getApplication();
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, message, duration).show();
            }
        });
    }

    public static void show(final Activity activity, final String message, final int duration, final int gravity, final int xOffset, final int yOffset) {
        if (activity == null)
            return;
        if (TextUtils.isEmpty(message))
            return;

        final Context context = activity.getApplication();
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(context, message, duration);
                toast.setGravity(gravity, xOffset, yOffset);
                toast.show();
            }
        });
    }

    public static void showLong(final Activity activity, final int resId) {
        show(activity, resId, Toast.LENGTH_LONG);
    }

    public static void showShort(final Activity activity, final int resId) {
        show(activity, resId, Toast.LENGTH_SHORT);
    }

    public static void showLong(final Activity activity, final String message) {
        show(activity, message, Toast.LENGTH_LONG);
    }

    public static void showShort(final Activity activity, final String message) {
        show(activity, message, Toast.LENGTH_SHORT);
    }

    public static void showShort(final Activity activity, final String message, int gravity, int xOffset, int yOffset) {
        show(activity, message, Toast.LENGTH_SHORT, gravity, xOffset, yOffset);
    }

    public static void showShort(final Activity activity, int resId, final int gravity, final int xOffset, final int yOffset) {
        show(activity, activity.getString(resId), Toast.LENGTH_SHORT, gravity, xOffset, yOffset);
    }

}
