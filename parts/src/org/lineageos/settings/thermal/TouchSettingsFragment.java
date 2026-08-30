/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.settings.thermal;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import com.android.settingslib.widget.SliderPreference;

import org.lineageos.settings.R;

public class TouchSettingsFragment extends SettingsBasePreferenceFragment
        implements OnCheckedChangeListener, Preference.OnPreferenceChangeListener {

    private SharedPreferences mSharedPrefs;
    private SliderPreference mTouchSensitivity;
    private SliderPreference mTouchResponse;
    private SliderPreference mTouchResistant;
    private MainSwitchPreference mGameMode;

    private String packageName = "";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.touch_settings, rootKey);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        Bundle bundle = getArguments();
        String appName = "";
        if (bundle != null) {
            appName = bundle.getString("appName", "");
            packageName = bundle.getString("packageName", "");
        }

        getActivity().setTitle(getResources().getString(R.string.touch_control_title));

        mGameMode = (MainSwitchPreference) findPreference(Constants.PREF_TOUCH_GAME_MODE);
        mGameMode.addOnSwitchChangeListener(this);
        mGameMode.setOnPreferenceChangeListener(this);

        mTouchResistant = (SliderPreference) findPreference(Constants.PREF_TOUCH_RESISTANT);
        mTouchResistant.setOnPreferenceChangeListener(this);
        mTouchResponse = (SliderPreference) findPreference(Constants.PREF_TOUCH_RESPONSE);
        mTouchResponse.setOnPreferenceChangeListener(this);
        mTouchSensitivity = (SliderPreference) findPreference(Constants.PREF_TOUCH_SENSITIVITY);
        mTouchSensitivity.setOnPreferenceChangeListener(this);
        updateDefaults();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mGameMode) {
            boolean enabled = (Boolean) newValue;
            mSharedPrefs.edit().putBoolean(Constants.PREF_TOUCH_GAME_MODE, enabled).apply();
            updateTouchModes(enabled ? 1 : 0, Constants.TOUCH_GAME_MODE);
            onCheckedChanged(null, enabled);
            return true;
        } else if (preference == mTouchResponse) {
            updateTouchModes((Integer) newValue, Constants.TOUCH_RESPONSE);
            return true;
        } else if (preference == mTouchSensitivity) {
            updateTouchModes((Integer) newValue, Constants.TOUCH_SENSITIVITY);
            return true;
        } else if (preference == mTouchResistant) {
            updateTouchModes((Integer) newValue, Constants.TOUCH_RESISTANT);
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mTouchSensitivity.setEnabled(isChecked);
        mTouchResponse.setEnabled(isChecked);
        mTouchResistant.setEnabled(isChecked);
    }

    private void updateDefaults() {
        String[] values = getTouchValues().split(",");
        boolean modeEnabled = Integer.parseInt(values[Constants.TOUCH_GAME_MODE]) == 1;
        mGameMode.setChecked(modeEnabled);

        mTouchSensitivity.setEnabled(modeEnabled);
        mTouchResponse.setEnabled(modeEnabled);
        mTouchResistant.setEnabled(modeEnabled);

        mTouchResponse.setValue(Integer.parseInt(values[Constants.TOUCH_RESPONSE]));
        mTouchSensitivity.setValue(Integer.parseInt(values[Constants.TOUCH_SENSITIVITY]));
        mTouchResistant.setValue(Integer.parseInt(values[Constants.TOUCH_RESISTANT]));
    }

    private void writeTouchValues(String modes) {
        mSharedPrefs.edit().putString(packageName, modes).apply();
    }

    public String getTouchValues() {
        String values = mSharedPrefs.getString(packageName, null);
        if (values == null || values.isEmpty()) {
            values = "0,0,0,0";
        }
        writeTouchValues(values);
        return values;
    }

    public void updateTouchModes(int value, int mode) {
        String[] values = getTouchValues().split(",");
        values[mode] = String.valueOf(value);
        String finalValues = values[Constants.TOUCH_GAME_MODE] + "," + values[Constants.TOUCH_RESPONSE] + ","
                + values[Constants.TOUCH_SENSITIVITY] + "," + values[Constants.TOUCH_RESISTANT];
        writeTouchValues(finalValues);
    }
}
