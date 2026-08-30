/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Display;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

public final class DisplayUtils {

    private static final String TAG = "DisplayUtils";

    private static final int DC_MODE_ID = 20;
    private static final int DC_ENABLE_VALUE = 1;
    private static final int DC_DISABLE_VALUE = 0;
    private static final int DC_COOKIE = 255;

    private static final String DC_PROP = "persist.vendor.dc_backlight.enable";

    private static final String HBM_PREVIOUS_BRIGHTNESS_KEY =
            "hbm_previous_brightness_percentage";
    private static final String HBM_ENABLE_CMD = "0x10000";
    private static final String HBM_DISABLE_CMD = "0xF0000";
    private static final float MAX_BRIGHTNESS_PERCENTAGE = 100f;

    private DisplayUtils() {
    }

    public static boolean isDcSupported() {
        return !getDcDimmingProperty().isEmpty();
    }

    public static boolean setDcDimming(boolean enabled) {
        if (!isDcSupported()) {
            Log.w(TAG, "DC Backlight property is not available");
            return false;
        }

        final int value = enabled ? DC_ENABLE_VALUE : DC_DISABLE_VALUE;
        final boolean success = DfWrapper.setDisplayFeature(
                new DfWrapper.DfParams(DC_MODE_ID, value, DC_COOKIE));
        if (success) {
            return true;
        }

        final String node = DisplayNodes.getDcDimmingNode();
        if (FileUtils.fileExists(node)) {
            final boolean fallback = FileUtils.writeLine(node, enabled ? "1" : "0");
            Log.d(TAG, "DC via exposure fallback: enabled=" + enabled
                    + " result=" + fallback);
            return fallback;
        }

        Log.e(TAG, "Failed to set DC Dimming through DisplayFeature");
        return false;
    }

    public static boolean restoreDcDimming(boolean enabled) {
        return setDcDimming(enabled);
    }

    public static String getDcDimmingProperty() {
        return SystemProperties.get(DC_PROP, "");
    }

    public static boolean setHbmEnabled(Context context, boolean enabled) {
        if (!FileUtils.fileExists(DisplayNodes.getHbmNode())) {
            return false;
        }

        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        if (enabled) {
            return enableHbm(context, preferences);
        }

        if (!FileUtils.writeLine(DisplayNodes.getHbmNode(), HBM_DISABLE_CMD)) {
            return false;
        }
        if (!preferences.contains(HBM_PREVIOUS_BRIGHTNESS_KEY)) {
            return true;
        }

        final float previousBrightness = preferences.getFloat(
                HBM_PREVIOUS_BRIGHTNESS_KEY, MAX_BRIGHTNESS_PERCENTAGE);
        if (!isValidBrightness(previousBrightness)
                || !isValidBrightness(getBrightness(context))
                || !setBrightness(context, previousBrightness)) {
            return true;
        }
        preferences.edit().remove(HBM_PREVIOUS_BRIGHTNESS_KEY).apply();
        return true;
    }

    private static boolean enableHbm(Context context, SharedPreferences preferences) {
        final boolean hasSavedBrightness =
                preferences.contains(HBM_PREVIOUS_BRIGHTNESS_KEY);
        final float brightness = getBrightness(context);
        if (!isValidBrightness(brightness)) {
            return false;
        }
        if (!hasSavedBrightness) {
            if (!preferences.edit().putFloat(
                    HBM_PREVIOUS_BRIGHTNESS_KEY, brightness).commit()) {
                return false;
            }
        }

        if (!setBrightness(context, MAX_BRIGHTNESS_PERCENTAGE)) {
            final boolean restored = setBrightness(context, preferences.getFloat(
                    HBM_PREVIOUS_BRIGHTNESS_KEY, brightness));
            if (!hasSavedBrightness && restored) {
                preferences.edit().remove(HBM_PREVIOUS_BRIGHTNESS_KEY).apply();
            }
            return false;
        }
        if (FileUtils.writeLine(DisplayNodes.getHbmNode(), HBM_ENABLE_CMD)) {
            return true;
        }

        final boolean restored = setBrightness(context, preferences.getFloat(
                HBM_PREVIOUS_BRIGHTNESS_KEY, MAX_BRIGHTNESS_PERCENTAGE));
        if (!hasSavedBrightness && restored) {
            preferences.edit().remove(HBM_PREVIOUS_BRIGHTNESS_KEY).apply();
        }
        return false;
    }

    private static float getBrightness(Context context) {
        final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        try {
            return displayManager.getBrightness(
                    Display.DEFAULT_DISPLAY, DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to read display brightness", e);
            return -1f;
        }
    }

    private static boolean setBrightness(Context context, float brightness) {
        final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        try {
            displayManager.setBrightness(
                    Display.DEFAULT_DISPLAY,
                    brightness,
                    DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE);
            final float appliedBrightness = displayManager.getBrightness(
                    Display.DEFAULT_DISPLAY, DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE);
            return Math.abs(appliedBrightness - brightness) < 0.1f;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to set display brightness", e);
            return false;
        }
    }

    private static boolean isValidBrightness(float brightness) {
        return !Float.isNaN(brightness)
                && brightness >= 0f
                && brightness <= MAX_BRIGHTNESS_PERCENTAGE;
    }
}
