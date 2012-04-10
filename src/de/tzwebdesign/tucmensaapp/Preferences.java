package de.tzwebdesign.tucmensaapp;

import de.tzwebdesign.tucmensaapp.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// Get a reference to the preferences
		ListPreference LPref;
		EditTextPreference ETPref;

		ETPref = (EditTextPreference) getPreferenceScreen().findPreference(
				"offset");

		ETPref.setSummary(ETPref.getText() + " "
				+ getResources().getText(R.string.Offset_text));

		LPref = (ListPreference) getPreferenceScreen().findPreference("mensa");
		LPref.setSummary(LPref.getEntry());

		LPref = (ListPreference) getPreferenceScreen().findPreference(
				"preiskat");
		LPref.setSummary(LPref.getEntry());

		LPref = (ListPreference) getPreferenceScreen()
				.findPreference("sprache");
		LPref.setSummary(LPref.getEntry());
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		Preference pref = findPreference(key);

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
		if (pref instanceof EditTextPreference) {

			EditTextPreference listPref = (EditTextPreference) pref;

			try {
				Integer.parseInt(listPref.getText());

			} catch (NumberFormatException nfe) {

				listPref.setText("0");

			}

			pref.setSummary(listPref.getText() + " "
					+ getResources().getText(R.string.Offset_text));
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		// Setup the initial values
		// sprache.setSummary(sharedPreferences.getBoolean(key, false) ?
		// "Disable this setting" : "Enable this setting");
		// mListPreference.setSummary("Current value is " +
		// sharedPreferences.getValue(key, ""));

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

}
