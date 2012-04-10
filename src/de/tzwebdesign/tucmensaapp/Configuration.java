package de.tzwebdesign.tucmensaapp;

import android.content.SharedPreferences;

public class Configuration {
	Integer offset;
	Boolean ListViewFirst;
	String preiskat;
	String mensa;
	String sprache;
	Boolean imageloading;
	Boolean imageSizeSmall;
	Integer image_pixel_size;
	
	private SharedPreferences settings;

	public Configuration(SharedPreferences settingsIn) {
		settings=settingsIn;
		refresh();
	}

	public void refresh() {

		offset = Integer.parseInt(settings.getString("offset", "0"));
		ListViewFirst = settings.getBoolean("ListViewFirst", false);
		preiskat = settings.getString("preiskat", "s");
		mensa = settings.getString("mensa", "rh");
		sprache = settings.getString("sprache", "de");
		imageloading = settings.getBoolean("imageloading", false);
		imageSizeSmall = settings.getBoolean("imagesize", false);

		if (settings.getBoolean("image_pixel_size_big", true) == true)
			image_pixel_size = 350;
		else
			image_pixel_size = 190;

	}
}
