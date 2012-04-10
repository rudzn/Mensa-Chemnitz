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
	public void setdate() {




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
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		config = new Configuration(settings);
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
	 * Pfad zur SD Karte
	 */
	private final File sdroot = new File(Environment
			.getExternalStorageDirectory().toString());

	/**
	 * Pfad zum App Ordner auf SD Karte
	 */
	private final File root = new File(Environment
			.getExternalStorageDirectory().toString() + "/TUCMensa");

	// TODO lieber jedesmal vor speicher verwendung checken, statt hier nur bei
	// Service-Start Status zu entnehmen?
	private String storage_state = Environment.getExternalStorageState();

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
	 * Objekt zur verwaltung gerade in arbeit befindlicher Bilder
	 */
	private FileWorking imageFileWorking = new FileWorking();

	/**
	 * Image file Status Objekt
	 */
	private FileStatusManagement imagefileStatusObject = new FileStatusManagement();

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

		return readImage(imgName, image_pixel_size);

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

			if (!fileExists_Image(imgName, image_pixel_size) || updateNow) {
				if (isExistingCheck) {
					imageFileWorking.remove(imgNameWithSize);
					return status.nonExisting;
				}

				try {
					Boolean stat = loadIMAGEtoSD(imgName, image_pixel_size);
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

	/**
	 * Prüft ob Bild vorhanden
	 * 
	 * @param name
	 *            Bildname
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return True wenn Bild existiert
	 */
	private synchronized boolean fileExists_Image(String name,
			int image_pixel_size) {

		return (new File(getFilename_Image(name, image_pixel_size))).exists();
	}

	/**
	 * Erzeugt Dateinamen für Bild
	 * 
	 * @param name
	 *            Bildname
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return Dateiname
	 */
	private String getFilename_Image(String name, int image_pixel_size) {

		String string = root.toString() + "/essenprev_"
				+ fourDigitsNumberformat.format(mYear) + "_"
				+ twoDigitsNumberformat.format(mMonth) + "_"
				+ twoDigitsNumberformat.format(mDay) + "-"
				+ fourDigitsNumberformat.format(Integer.parseInt(name)) + "_"
				+ fourDigitsNumberformat.format(image_pixel_size) + ".png";
		return string;

	}

	// private String loadIMAGEtoSD_string = "";

	/**
	 * Lädt Bild aus Netz und speichert es auf SD
	 * 
	 * @param name
	 *            Bildname
	 * @param image_pixel_size
	 *            Bildgröße
	 * @throws CustomException
	 */
	private boolean loadIMAGEtoSD(String name, int image_pixel_size)
			throws CustomException {

		String image_pixel_size_string;
		try {
			if (image_pixel_size == 190)
				image_pixel_size_string = "";
			else
				image_pixel_size_string = "_" + image_pixel_size;
			String url = "http://www-user.tu-chemnitz.de/~fnor/mensa/bilder"
					+ image_pixel_size_string + "/" + name + ".png";
			URL aURL;

			aURL = new URL(url);

			URLConnection conn = aURL.openConnection();
			conn.setConnectTimeout(10000);
			conn.connect();

			InputStream input_stream = conn.getInputStream();

			BufferedInputStream buffered_input_stream = new BufferedInputStream(
					input_stream);
			Bitmap image = BitmapFactory.decodeStream(input_stream);
			if (image == null)
				throw new CustomException(errors.ImageDownloadError);

			buffered_input_stream.close();
			input_stream.close();

			// TODO
			// Not IMPLEMENTED: Vergleich ob datei neu. da nie nach neuer
			// gesucht wird hier nicht implementiert
			// if(fileExists_Image(name) && readImage(name).equals(image))
			// {
			// return false;

			// }
			saveImage(name, image, image_pixel_size);
			// return true;
			return false;// Folglich immer False...

		} catch (MalformedURLException e) {

			throw new CustomException(errors.URLError);

		} catch (IOException e) {

			throw new CustomException(errors.ConnectionError);

		}
		// loadIMAGEtoSD_string = "";
		// return true;

	}

	/**
	 * Speichert Bild auf SD
	 * 
	 * @param name
	 *            Bildname
	 * @param image
	 *            Bild
	 * @param image_pixel_size
	 *            Bildgröße
	 * @throws CustomException
	 */
	private synchronized void saveImage(String name, Bitmap image,
			int image_pixel_size) throws CustomException {
		try {

			if (storage_state.contains("mounted")) {
				if (sdroot.canWrite()) {

					if (!root.exists()) {
						CreateDir();
					}

					File gpxfile = new File(getFilename_Image(name,
							image_pixel_size));

					FileOutputStream fop = new FileOutputStream(gpxfile);

					image.compress(Bitmap.CompressFormat.PNG, 100, fop);

					fop.flush();
					fop.close();

				} else {
					throw new CustomException(errors.WriteProtectedSD);
				}
			} else {
				throw new CustomException(errors.MissingSD);
			}

		} catch (IOException g) {
			throw new CustomException(errors.ImageWriteError);
		}

	}

	/**
	 * Liest Bild von SD
	 * 
	 * @param name
	 *            Bildname
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return Bild
	 * @throws CustomException
	 */
	private synchronized Bitmap readImage(String name, int image_pixel_size)
			throws CustomException {

		Bitmap image = null;
		try {

			File file = new File(getFilename_Image(name, image_pixel_size));
			FileInputStream fileinput = new FileInputStream(file);

			BufferedInputStream buffered_input_stream = new BufferedInputStream(
					fileinput);
			image = BitmapFactory.decodeStream(buffered_input_stream);

			fileinput.close();
			buffered_input_stream.close();

		} catch (IOException e) {

			// loadXMLintoRuntime_string = "IOException";
			// return false;
			throw new CustomException(errors.ImageReadError);

		}

		// loadXMLintoRuntime_string = "";

		return image;
	}

	/**
	 * Objekt zur verwaltung gerade in arbeit befindlicher XML Dateien
	 */
	private FileWorking xmlFileWorking = new FileWorking();

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

			if (updateNow || !fileExists_XML(mensa, inYear, inMonth, inDay)) {
				if (isExistingCheck) {
					xmlFileWorking.remove(mensa + inYear + inMonth + inDay);
					return status.nonExisting;
				}

				try {
					Boolean stat = loadXMLtoSD(mensa, inYear, inMonth, inDay);
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

		return readXMLasNodeList(mensa, Year, Month, Day);

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
	 * Lädt XML und speichert auf SD
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @return True wenn speichern ohne fehler
	 * @throws CustomException
	 */
	private boolean loadXMLtoSD(String mensa, int Year, int Month, int Day)
			throws CustomException {
		if (mensa == "st")
			mensa = "strana";
		try {
			String url = "http://www-user.tu-chemnitz.de/~fnor/mensa/webservice_xml_2.php?mensa="
					+ mensa
					+ "&tag="
					+ Day
					+ "&monat="
					+ Month
					+ "&jahr="
					+ Year;

			URL aURL;

			aURL = new URL(url);

			URLConnection conn = aURL.openConnection();
			conn.setConnectTimeout(10000);
			conn.connect();

			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);

			StringBuilder sb = new StringBuilder();

			String line;

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "UTF-8"));

			while ((line = reader.readLine()) != null) {

				sb.append(line);

			}

			bis.close();
			is.close();

			String hm = sb.toString();
			if (!hm.contains("<essen")) {

				throw new CustomException(errors.XMLNoEssenFound);
			}

			if (fileExists_XML(mensa, Year, Month, Day)
					&& readXMLasString(mensa, Year, Month, Day).equals(hm)) {
				return false;

			}
			SaveXML(Year, Month, Day, mensa, hm);
			return true;

		} catch (MalformedURLException e) {

			throw new CustomException(errors.URLError);

		} catch (IOException e) {

			throw new CustomException(errors.ConnectionError);
		}
	}

	/**
	 * Prüft ob XML Datei vorhanden
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @return True wenn XML Datei vorhanden
	 */
	private synchronized boolean fileExists_XML(String mensa, int Year,
			int Month, int Day) {

		return (new File(getFilename_XML(mensa, Year, Month, Day))).exists();
	}

	/**
	 * Erzeugt den Speicher-Dateinamen für XML Datei
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @return Dateiname
	 */
	private String getFilename_XML(String mensa, int Year, int Month, int Day) {
		String string = root.toString() + "/essenprev_"
				+ fourDigitsNumberformat.format(Year) + "_"
				+ twoDigitsNumberformat.format(Month) + "_"
				+ twoDigitsNumberformat.format(Day) + "_" + mensa + ".xml";
		return string;

	}

	private Object FileSync = new Object();

	/**
	 * Speichert data als XMl Datei auf SD
	 * 
	 * @param Year
	 * @param Month
	 * @param Day
	 * @param mensa
	 *            rh oder st
	 * @param data
	 *            zu Speichernder Dateiinhalt
	 * @throws CustomException
	 */
	private void SaveXML(int Year, int Month, int Day, String mensa, String data)
			throws CustomException {
		synchronized (FileSync) {
			try {
				if (storage_state.contains("mounted")) {

					if (sdroot.canWrite()) {

						if (!root.exists()) {
							CreateDir();
						}
						// new AlertDialog.Builder(this)
						// .setTitle("Fehler")
						// .setMessage(
						// "Cache Ordner konnte nicht erstellt werden!")
						// .show();
						// }

						File gpxfile = new File(getFilename_XML(mensa, Year,
								Month, Day));

						// Create file
						FileWriter fstream;

						fstream = new FileWriter(gpxfile);

						BufferedWriter out = new BufferedWriter(fstream);
						out.write(data);
						// Close the output stream
						out.close();
					} else {
						// new AlertDialog.Builder(this)
						// .setTitle("Fehler")
						// .setMessage(
						// "Keine Schreibrechte auf SD Karte!")
						// .show();

						// loadIMAGEtoSD_string =
						// "Keine Schreibrechte auf SD Karte!";
						// return false;

						throw new CustomException(errors.WriteProtectedSD);

					}
				} else {
					// new AlertDialog.Builder(this)
					// .setTitle("Fehler")
					// .setMessage(
					// "Keine SD Karte gefunden! Bild nicht gecached!")
					// .show();

					// loadIMAGEtoSD_string = "Keine SD Karte gefunden!";
					// return false;
					throw new CustomException(errors.MissingSD);

				}

			} catch (IOException e) {
				throw new CustomException(errors.XMLWriteError);
			}

			// return true;
		}
	}

	/**
	 * Legt Mensa App Verzeichnis auf SD Karte an
	 */
	private void CreateDir() {

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
	 * Liest Essen aus XML Datei für bestimmtes Datum
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @return nodelist
	 * @throws CustomException
	 */
	private NodeList readXMLasNodeList(String mensa, int Year, int Month,
			int Day) throws CustomException {

		Document doc = readXMLasXMLDocument(mensa, Year, Month, Day);
		NodeList nodes = doc.getElementsByTagName("essen");
		return nodes;

	}

	/**
	 * Liest XML Datei von SD
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @return xml document
	 * @throws CustomException
	 */
	private Document readXMLasXMLDocument(String mensa, int Year, int Month,
			int Day) throws CustomException {
		synchronized (FileSync) {
			try {

				String path = getFilename_XML(mensa, Year, Month, Day);

				FileInputStream fileinput;
				fileinput = new FileInputStream(path);
				BufferedInputStream buffered_input_stream = new BufferedInputStream(
						fileinput);

				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(buffered_input_stream);
				doc.getDocumentElement().normalize();
				// System.out.println("Root element " +
				// doc.getDocumentElement().getNodeName());
				return doc;
			} catch (FileNotFoundException e) {
				throw new CustomException(errors.XMLReadError);
			} catch (ParserConfigurationException e) {
				throw new CustomException(errors.XMLParserError);
			} catch (SAXException e) {
				throw new CustomException(errors.XMLSAXError);
			} catch (IOException e) {
				throw new CustomException(errors.XMLIOError);
			}

		}
	}

	/**
	 * Liest XML Datei von SD als String
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @return XML als String
	 * @throws CustomException
	 */
	private String readXMLasString(String mensa, int Year, int Month, int Day)
			throws CustomException {
		synchronized (FileSync) {
			try {

				String path = getFilename_XML(mensa, Year, Month, Day);

				BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(path)));
				StringBuffer contentOfFile = new StringBuffer();
				String line;
				while ((line = br.readLine()) != null) {
					contentOfFile.append(line);
				}
				String content = contentOfFile.toString();

				return content;
			} catch (FileNotFoundException e) {
				throw new CustomException(errors.XMLReadError);
			} catch (IOException e) {
				throw new CustomException(errors.XMLIOError);
			}

		}
	}

	/**
	 * Löscht Bilder und Essenslisten die veraltet sind aus dem Speicher
	 */
	public void deleteOldFiles() {

		// Kann in nachfolgenden Versionen wieder weg
		CreateDir(); // Reicht die .nomedia datei nach für allte installationen

		Calendar caldel = new GregorianCalendar(mYear, mMonth, mDay);

		if (storage_state.contains("mounted")) {
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
									synchronized (FileSync) {

										if (MimeType.equals("xml")
												&& xmlFileWorking.start(""
														+ u2mensa + u2year
														+ u2month + u2day)) {
											xmlfileStatusObject.remove(""
													+ u2year + u2month + u2day
													+ u2mensa);
											filelist_of_cache[i].delete();
											xmlFileWorking.remove("" + u2mensa
													+ u2year + u2month + u2day);
										}
										if (MimeType.equals("png")
												&& imageFileWorking
														.start(imgName + "_"
																+ imgpixelSize)) {
											imagefileStatusObject
													.remove(imgName + "_"
															+ imgpixelSize);
											filelist_of_cache[i].delete();
											imageFileWorking.remove(imgName
													+ imgpixelSize);
										}
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