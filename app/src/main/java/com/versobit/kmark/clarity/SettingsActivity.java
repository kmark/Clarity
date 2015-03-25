/*
 * Copyright (C) 2015 Kevin Mark
 *
 * This file is part of Clarity.
 *
 * Clarity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Clarity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Clarity.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.kmark.clarity;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.versobit.kmark.clarity.dialogs.AboutDialog;
import com.versobit.kmark.clarity.dialogs.DbProcessorDialog;

public final class SettingsActivity extends PreferenceActivity {

    private static final String ALIAS = BuildConfig.APPLICATION_ID + ".SettingsActivityLauncher";

    static void setDefaultPreferences(Context ctx) {
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_about, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static final class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceCategory header;

            // Add general preferences
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValue(findPreference(Setting.THUMBNAIL_DIM.toString()));
            Preference dbProc = findPreference(Setting.DATABASE_PROCESSOR.toString());
            dbProc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new DbProcessorDialog().show(getFragmentManager(), DbProcessorDialog.FRAGMENT_TAG);
                    return true;
                }
            });
            Preference showIcon = findPreference(Setting.SHOW_ICON.toString());
            showIcon.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Toggle enabled status for the alias, not the actual activity
                    getActivity().getPackageManager().setComponentEnabledSetting(
                            new ComponentName(getActivity(), ALIAS),
                            (Boolean)newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                    );
                    return true;
                }
            });

            // Add About preferences and a corresponding header
            header = new PreferenceCategory(getActivity());
            header.setTitle(R.string.pref_header_about);
            getPreferenceScreen().addPreference(header);
            addPreferencesFromResource(R.xml.pref_about);

            Preference aboutVersion = findPreference(Setting.ABOUT_VERSION.toString());
            aboutVersion.setTitle(getString(R.string.pref_title_about_clarity, BuildConfig.VERSION_NAME));
            aboutVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AboutDialog().show(getFragmentManager(), AboutDialog.FRAGMENT_TAG);
                    return true;
                }
            });
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

}
