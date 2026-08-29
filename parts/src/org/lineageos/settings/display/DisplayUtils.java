/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display;

import android.os.SystemProperties;
import android.util.Log;

public final class DisplayUtils {

    private static final String TAG = "DisplayUtils";

    private static final int DC_MODE_ID = 20;
    private static final int DC_ENABLE_VALUE = 1;
    private static final int DC_DISABLE_VALUE = 0;
    private static final int DC_COOKIE = 255;

    private static final String DC_PROP = "persist.vendor.dc_backlight.enable";

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
}
