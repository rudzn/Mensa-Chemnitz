package de.tzwebdesign.TUCMensa;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tzwebdesign.TUCMensa.CustomException.errors;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class MensaService extends Service {

	/**
	 * Zeitversatz in Stunden um den der nächste Tag vorgezogen wird in der
	 * Anzeige
	 */
	private int offset;

	private int mYear;
	private int mMonth;
	private int mDay;

	public int getmYear() {
		return mYear;
	}

	public int getmMonth() {
		return mMonth;
	}

	public int getmDay() {
		return mDay;
	}

	/**
	 * Setzt die Datumsfelder unter einbeziehung des UserKonfigurierbaren
	 * Offsets Wochenenden werden übersprungen
	 */
	private void setdate() {

		Calendar c = Calendar.getInstance();

		c.add(Calendar.HOUR, config.offset);

		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			c.add(Calendar.DATE, 2);
		}
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			c.add(Calendar.DATE, 1);
		}

		mYear = c.get(Calendar.YEAR);
		// +1 Weil Java bei Monaten von 0 bis 11 zählt
		mMonth = c.get(Calendar.MONTH) + 1;
		mDay = c.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Check ob Datum mittlerweile verändert (Weil App lange im Standby usw)
	 * 
	 * @return Liefert True wenn Datum von App mit neu berechnetem nicht
	 *         übereinstimmt
	 */
	public boolean checkdate() {

		Calendar c = Calendar.getInstance();

		c.add(Calendar.HOUR, config.offset);

		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			c.add(Calendar.DATE, 2);
		}
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			c.add(Calendar.DATE, 1);
		}
		if (mYear != c.get(Calendar.YEAR))
			return true;
		// +1 weil Java bei Monaten von 0 bis 11 zählt
		if (mMonth != (c.get(Calendar.MONTH) + 1))
			return true;
		if (mDay != c.get(Calendar.DAY_OF_MONTH))
			return true;

		return false;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		MensaService getService() {
			return MensaService.this;
		}
	}

	public Configuration config;

	@Override
	public void onCreate() {
		refreshconfig();
	}

	public void refreshconfig() {
		if (config != null) {
			config.refresh();

		} else {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			config = new Configuration(settings);

		}
		setdate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {

	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new MensaService.LocalBinder();

	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################
	// ###########################################################################################

	/**
	 * Image file Status Objekt
	 */
	private FileStatusManagement imagefileStatusObject = new FileStatusManagement();

	/**
	 * Objekt zur verwaltung gerade in arbeit befindlicher Bilder
	 */
	private FileWorking imageFileWorking = new FileWorking();

	/**
	 * Objekt zur verwaltung gerade in arbeit befindlicher XML Dateien
	 */
	private FileWorking xmlFileWorking = new FileWorking();

	/**
	 * Pfad zur SD Karte
	 */
	private final File sdroot = new File(Environment
			.getExternalStorageDirectory().toString());

	/**
	 * Pfad zum App Ordner auf SD Karte
	 */
	private final File root = new File(Environment
			.getExternalStorageDirectory().toString() + "/TUCMensa");

	private IO_XML ioXML = new IO_XML(this);
	private IO_Image ioImage = new IO_Image(this);

	private NumberFormat twoDigitsNumberformat = NumberFormat.getInstance();
	private NumberFormat fourDigitsNumberformat = NumberFormat.getInstance();

	/**
	 * status den eine Datei haben kann
	 * 
	 */
	public enum status {
		nonExisting, working, Existing, Updated, nowUpdated, nonUpdated
	};

	/**
	 * Konstruktor
	 */
	public MensaService() {
		twoDigitsNumberformat.setMinimumIntegerDigits(2); // The minimum Digits
															// required is 2
		twoDigitsNumberformat.setMaximumIntegerDigits(2); // The maximum Digits
															// required is 2
		twoDigitsNumberformat.setGroupingUsed(false);

		fourDigitsNumberformat.setMinimumIntegerDigits(4); // The minimum Digits
															// required is 4
		fourDigitsNumberformat.setMaximumIntegerDigits(4); // The maximum Digits
															// required is 4
		fourDigitsNumberformat.setGroupingUsed(false);

	}

	/**
	 * Liefert Bild
	 * 
	 * @param imgName
	 *            Bildname
	 * @param isExistingCheck
	 *            Bild nicht nachladen wenn fehlend
	 * @return Bild
	 * @throws CustomException
	 */
	public Bitmap getImage(String imgName, boolean isExistingCheck)
			throws CustomException {

		getImage_status(imgName, false, isExistingCheck,
				config.image_pixel_size);

		return ioImage.readImage(imgName, config.image_pixel_size);

	}

	/**
	 * Liefert Bild
	 * 
	 * @param imgName
	 *            Bildname
	 * @param isExistingCheck
	 *            Bild nicht nachladen wenn fehlend
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return Bild
	 * @throws CustomException
	 */
	public Bitmap getImage(String imgName, boolean isExistingCheck,
			int image_pixel_size) throws CustomException {

		getImage_status(imgName, false, isExistingCheck, image_pixel_size);

		return ioImage.readImage(imgName, image_pixel_size);

	}

	/**
	 * Liefert Bild wen vorhanden, ansonsten wird es nachgeladen (parameter
	 * weichen verhalten ab)
	 * 
	 * @param imgName
	 *            Bildname
	 * @param updateNow
	 *            Bild aktualisieren
	 * @param isExistingCheck
	 *            Bild nicht nachladen wenn fehlend (überschreibt updateNow)
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return Status des Bildes
	 * @throws CustomException
	 */
	private status prepareImage(String imgName, boolean updateNow,
			boolean isExistingCheck, int image_pixel_size)
			throws CustomException {

		String imgNameWithSize = fourDigitsNumberformat.format(Integer
				.parseInt(imgName))
				+ "_"
				+ fourDigitsNumberformat.format(image_pixel_size);

		if (imagefileStatusObject.isUpdated(imgNameWithSize))
			return status.Updated;

		if (imagefileStatusObject.isReady(imgNameWithSize) && !updateNow)
			return status.Existing;

		if (imageFileWorking.start(imgNameWithSize)) {

			if (!ioImage.fileExists_Image(imgName, image_pixel_size)
					|| updateNow) {
				if (isExistingCheck) {
					imageFileWorking.remove(imgNameWithSize);
					return status.nonExisting;
				}

				try {
					Boolean stat = ioImage.loadIMAGEtoSD(imgName,
							image_pixel_size);
					imagefileStatusObject.setupdated(imgNameWithSize);
					imageFileWorking.remove(imgNameWithSize);
					if (stat)
						return status.nowUpdated;
					else
						return status.nonUpdated;

				} catch (CustomException e) {
					imageFileWorking.remove(imgNameWithSize);
					throw e;
				}
			}
			imagefileStatusObject.setready(imgNameWithSize);
			imageFileWorking.remove(imgNameWithSize);
			return status.Existing;
		}
		return status.working;
	}

	/**
	 * Liefert den Bildstatus und Läd es wenn nötig nach (je nach parameter)
	 * 
	 * @param imgName
	 *            Bildname
	 * @param updateNow
	 *            aktualisiert Bild
	 * @param isExistingCheck
	 *            Wenn nicht vorhanden auch nicht nachladen (überschreibt
	 *            updateNow)
	 * @return Status
	 * @throws CustomException
	 */
	public status getImage_status(String imgName, boolean updateNow,
			boolean isExistingCheck) throws CustomException {

		return getImage_status(imgName, updateNow, isExistingCheck,
				config.image_pixel_size);

	}

	/**
	 * Liefert den Bildstatus und Läd es wenn nötig nach (je nach parameter)
	 * 
	 * @param imgName
	 *            Bildname
	 * @param updateNow
	 *            aktualisiert Bild
	 * @param isExistingCheck
	 *            Wenn nicht vorhanden auch nicht nachladen (überschreibt
	 *            updateNow)
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return Status
	 * @throws CustomException
	 */
	public status getImage_status(String imgName, boolean updateNow,
			boolean isExistingCheck, int image_pixel_size)
			throws CustomException {

		status stat = null;
		for (int i = 0; i < 150; i++) {
			stat = prepareImage(imgName, updateNow, isExistingCheck,
					image_pixel_size);

			if (stat != status.working)
				return stat;

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new CustomException(errors.ImageWorkerSleeperFailed);
			}

		}

		throw new CustomException(errors.ImageWorkerTimeout);

	}

	// private String loadIMAGEtoSD_string = "";

	public enum filestatus {
		ready, updated
	};

	/**
	 * XML file Status Objekt
	 */
	private FileStatusManagement xmlfileStatusObject = new FileStatusManagement();

	/**
	 * Prüft ob Datei vorhanden, wenn nicht wird sie aus Netz geladen
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param inYear
	 * @param inMonth
	 * @param inDay
	 * @param updateNow
	 *            Wenn True wird die Datei aktualisiert (auch wenn schon
	 *            vorhanden)
	 * @param isExistingCheck
	 *            Wenn True wird keine aktualisierung vorgenommen sondern nur
	 *            die existenz geprüft (überschreibt updateNow)
	 * @return
	 * @throws CustomException
	 */
	private status prepareXML(String mensa, int inYear, int inMonth, int inDay,
			boolean updateNow, boolean isExistingCheck) throws CustomException {

		if (xmlfileStatusObject
				.isUpdated("" + inYear + inMonth + inDay + mensa))
			return status.Updated;

		if (xmlfileStatusObject
				.isUpdated("" + inYear + inMonth + inDay + mensa) && !updateNow)
			return status.Existing;

		if (xmlFileWorking.start(mensa + inYear + inMonth + inDay)) {

			if (updateNow
					|| !ioXML.fileExists_XML(mensa, inYear, inMonth, inDay)) {
				if (isExistingCheck) {
					xmlFileWorking.remove(mensa + inYear + inMonth + inDay);
					return status.nonExisting;
				}

				try {
					Boolean stat = ioXML.loadXMLtoSD(mensa, inYear, inMonth,
							inDay);
					xmlfileStatusObject.setupdated("" + inYear + inMonth
							+ inDay + mensa);
					xmlFileWorking.remove(mensa + inYear + inMonth + inDay);
					if (stat)
						return status.nowUpdated;
					else
						return status.nonUpdated;

				} catch (CustomException e) {
					xmlFileWorking.remove(mensa + inYear + inMonth + inDay);
					throw e;
				}
			}
			xmlfileStatusObject.setready("" + inYear + inMonth + inDay + mensa);
			xmlFileWorking.remove(mensa + inYear + inMonth + inDay);
			return status.Existing;
		}
		return status.working;
	}

	/**
	 * Liefert XML des in der App aktellen Datums (Berechnets Datum)
	 * 
	 * @param isExistingCheck
	 *            True unterdrückt aktualisierung, prüft nur existens
	 * @return nodelist
	 * @throws CustomException
	 */
	public NodeList getXML(boolean isExistingCheck) throws CustomException {

		return getXML(config.mensa, mYear, mMonth, mDay, isExistingCheck);

	}

	/**
	 * Liefert XML des in der App aktellen Datums (Berechnets Datum)
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param isExistingCheck
	 *            True unterdrückt aktualisierung, prüft nur existens
	 * @return nodelist
	 * @throws CustomException
	 */
	public NodeList getXML(String mensa, boolean isExistingCheck)
			throws CustomException {

		return getXML(mensa, mYear, mMonth, mDay, isExistingCheck);

	}

	/**
	 * Liefert XML für ein bestimmtes Datum
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @param isExistingCheck
	 *            Wenn True aktualisieren unterdrücken
	 * @return nodelist
	 * @throws CustomException
	 */
	public NodeList getXML(String mensa, int Year, int Month, int Day,
			boolean isExistingCheck) throws CustomException {

		getXML_status(mensa, Year, Month, Day, false, isExistingCheck);

		return ioXML.readXMLasNodeList(mensa, Year, Month, Day);

	}

	/**
	 * Prüft ob Datei vorhanden (für aktuelles App Datum), wenn nicht wird sie
	 * aus Netz gelden. Dabei wird gewartet das ein Slot zur bearbeitung frei
	 * wird
	 * 
	 * @param updateNow
	 *            Wenn True wird die Datei aktualisiert (auch wenn schon
	 *            vorhanden)
	 * @param isExistingCheck
	 *            Wenn True wird keine aktualisierung durchgeführt, sonern nur
	 *            auf existenz überprüft (überschreibt updateNow)
	 * @return status
	 * @throws CustomException
	 */
	public status getXML_status(boolean updateNow, boolean isExistingCheck)
			throws CustomException {

		return getXML_status(config.mensa, mYear, mMonth, mDay, updateNow,
				isExistingCheck);
	}

	/**
	 * Prüft ob Datei vorhanden (für aktuelles App Datum), wenn nicht wird sie
	 * aus Netz gelden. Dabei wird gewartet das ein Slot zur bearbeitung frei
	 * wird
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param updateNow
	 *            Wenn True wird die Datei aktualisiert (auch wenn schon
	 *            vorhanden)
	 * @param isExistingCheck
	 *            Wenn True wird keine aktualisierung durchgeführt, sonern nur
	 *            auf existenz überprüft (überschreibt updateNow)
	 * @return status
	 * @throws CustomException
	 */
	public status getXML_status(String mensa, boolean updateNow,
			boolean isExistingCheck) throws CustomException {

		return getXML_status(mensa, mYear, mMonth, mDay, updateNow,
				isExistingCheck);
	}

	/**
	 * Prüft ob Datei vorhanden (für Datum von prameter), wenn nicht wird sie
	 * aus Netz geladen. Dabei wird gewartet das ein Slot zur bearbeitung frei
	 * wird
	 * 
	 * @param Year
	 * @param Month
	 * @param Day
	 * @param updateNow
	 *            Wenn True wird die Datei aktualisiert (auch wenn schon
	 *            vorhanden)
	 * @param isExistingCheck
	 *            Wenn True wird keine aktualisierung vorgenommen sondern nur
	 *            die existenz geprüft (überschreibt updateNow)
	 * @return status
	 * @throws CustomException
	 */
	public status getXML_status(int Year, int Month, int Day,
			boolean updateNow, boolean isExistingCheck) throws CustomException {

		return getXML_status(config.mensa, Year, Month, Day, updateNow,
				isExistingCheck);

	}

	/**
	 * Prüft ob Datei vorhanden (für Datum von prameter), wenn nicht wird sie
	 * aus Netz geladen. Dabei wird gewartet das ein Slot zur bearbeitung frei
	 * wird
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @param updateNow
	 *            Wenn True wird die Datei aktualisiert (auch wenn schon
	 *            vorhanden)
	 * @param isExistingCheck
	 *            Wenn True wird keine aktualisierung vorgenommen sondern nur
	 *            die existenz geprüft (überschreibt updateNow)
	 * @return status
	 * @throws CustomException
	 */
	public status getXML_status(String mensa, int Year, int Month, int Day,
			boolean updateNow, boolean isExistingCheck) throws CustomException {
		status stat = null;
		for (int i = 0; i < 150; i++) {
			stat = prepareXML(mensa, Year, Month, Day, updateNow,
					isExistingCheck);

			if (stat != status.working)
				return stat;

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new CustomException(errors.XMLWorkerSleeperFailed);
			}

		}

		throw new CustomException(errors.XMLWorkerTimeout);

	}

	/**
	 * Legt Mensa App Verzeichnis auf SD Karte an
	 */
	public void CreateDir() {

		root.mkdir();

		File noMedia = new File(root + "/.nomedia");
		try {
			noMedia.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Löscht Bilder und Essenslisten die veraltet sind aus dem Speicher
	 */
	public void deleteOldFiles() {

		// Kann in nachfolgenden Versionen wieder weg
		CreateDir(); // Reicht die .nomedia datei nach für allte installationen

		Calendar caldel = new GregorianCalendar(mYear, mMonth, mDay);

		if (Environment.getExternalStorageState().contains("mounted")) {
			if (root.canWrite()) {

				if (root.isDirectory()) {

					String u1, u2mensa, imgName, imgpixelSize;
					int u2day, u2month, u2year;
					imgName = "";
					imgpixelSize = "";
					u2mensa = "";
					File[] filelist_of_cache = root.listFiles();

					for (int i = 0; i <= filelist_of_cache.length - 1; i++) {

						try {

							u1 = filelist_of_cache[i].toString().substring(
									root.toString().length() + 1,
									root.toString().length() + 10);
							if (u1.equals("essenprev")) {

								String MimeType = filelist_of_cache[i]
										.toString().substring(
												filelist_of_cache[i].toString()
														.length() - 3,
												filelist_of_cache[i].toString()
														.length());
								if (MimeType.equals("png"))
									imgName = filelist_of_cache[i]
											.toString()
											.substring(
													root.toString().length() + 22,
													root.toString().length() + 26);

								if (MimeType.equals("png"))
									imgpixelSize = filelist_of_cache[i]
											.toString()
											.substring(
													root.toString().length() + 27,
													root.toString().length() + 31);

								if (MimeType.equals("xml"))
									u2mensa = filelist_of_cache[i]
											.toString()
											.substring(
													root.toString().length() + 22,
													root.toString().length() + 23);
								u2day = Integer.parseInt(filelist_of_cache[i]
										.toString().substring(
												root.toString().length() + 19,
												root.toString().length() + 21));
								u2month = Integer.parseInt(filelist_of_cache[i]
										.toString().substring(
												root.toString().length() + 16,
												root.toString().length() + 18));
								u2year = Integer.parseInt(filelist_of_cache[i]
										.toString().substring(
												root.toString().length() + 11,
												root.toString().length() + 15));

								// u2mensa = filelist_of_cache[i].toString()
								// .substring(
								// root.toString().length() + 22,
								// root.toString().length() + 24);

								Calendar caldel2 = new GregorianCalendar(
										u2year, u2month, u2day);

								if (caldel.compareTo(caldel2) == 1) {

									if (MimeType.equals("xml")
											&& xmlFileWorking.start(""
													+ u2mensa + u2year
													+ u2month + u2day)) {
										xmlfileStatusObject.remove("" + u2year
												+ u2month + u2day + u2mensa);
										ioXML.deleteXML(filelist_of_cache[i]);

										xmlFileWorking.remove("" + u2mensa
												+ u2year + u2month + u2day);
									}
									if (MimeType.equals("png")
											&& imageFileWorking.start(imgName
													+ "_" + imgpixelSize)) {
										imagefileStatusObject.remove(imgName
												+ "_" + imgpixelSize);
										ioImage.deleteXML(filelist_of_cache[i]);
										imageFileWorking.remove(imgName
												+ imgpixelSize);
									}

								}
							}
						} catch (Exception e) {

						}

					}
				}
			}
		}
	}

	/**
	 * Lädt so lange XML Dateien (Essenstage) aus dem netz bis keine mehr
	 * vorliegen / aktualisiert bestehende
	 * 
	 * @return true wenn daten aktualisert
	 */
	public boolean LoadAllXML() {
		return LoadAllXML("rh") || LoadAllXML("st");
	}

	/**
	 * Lädt so lange XML Dateien (Essenstage) aus dem netz bis keine mehr
	 * vorliegen / aktualisiert bestehende
	 * 
	 * @param mensa
	 *            rh oder st
	 * @return true wenn daten aktualisert
	 */
	public boolean LoadAllXML(String mensa) {
		Calendar cal = new GregorianCalendar(mYear, mMonth - 1, mDay);
		boolean stat = false;

		while (true) {

			if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
				cal.add(Calendar.DATE, 2);
			}
			if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
				cal.add(Calendar.DATE, 1);
			}
			int Year = cal.get(Calendar.YEAR);
			int Month = cal.get(Calendar.MONTH) + 1;
			int Day = cal.get(Calendar.DAY_OF_MONTH);

			try {
				status statreturn = getXML_status(mensa, Year, Month, Day,
						true, false);
				// if(statreturn==status.nonUpdated)
				stat = (status.nowUpdated == statreturn) || stat;

			} catch (CustomException e) {
				break;
				// TODO
			}

			cal.add(Calendar.HOUR, 24);

		}
		return stat;
	}

	/**
	 * Erzeugt den Statusspeicher über XML Dateien
	 * 
	 * @param mensa
	 *            rh oder st
	 */
	public void checkAllXML() {
		checkAllXML("rh");
		checkAllXML("st");
	}

	/**
	 * Erzeugt den Statusspeicher über XML Dateien
	 * 
	 * @param mensa
	 *            rh oder st
	 */
	public void checkAllXML(String mensa) {
		Calendar cal = new GregorianCalendar(mYear, mMonth - 1, mDay);

		for (int i = 0; i < 15; i++) {

			if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
				cal.add(Calendar.DATE, 2);
			}
			if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
				cal.add(Calendar.DATE, 1);
			}
			int Year = cal.get(Calendar.YEAR);
			int Month = cal.get(Calendar.MONTH) + 1;
			int Day = cal.get(Calendar.DAY_OF_MONTH);

			try {

				getXML_status(mensa, Year, Month, Day, false, true);

			} catch (CustomException e) {
				break;
				// oder verwerfe fehler
			}

			cal.add(Calendar.HOUR, 24);

		}

	}

}