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
	// private NotificationManager mNM;

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

	public void setdate() {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		offset = Integer.parseInt(settings.getString("offset", "0"));

		Calendar c = Calendar.getInstance();

		c.add(Calendar.HOUR, offset);

		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			c.add(Calendar.DATE, 2);
		}
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			c.add(Calendar.DATE, 1);
		}

		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH) + 1;
		mDay = c.get(Calendar.DAY_OF_MONTH);
	}

	public boolean checkdate() {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		offset = Integer.parseInt(settings.getString("offset", "0"));

		Calendar c = Calendar.getInstance();

		c.add(Calendar.HOUR, offset);

		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			c.add(Calendar.DATE, 2);
		}
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			c.add(Calendar.DATE, 1);
		}
		if (mYear != c.get(Calendar.YEAR))
			return true;
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

	@Override
	public void onCreate() {
		// mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		// showNotification();

		// Toast.makeText(this,"Service created ...", Toast.LENGTH_LONG).show();

		setdate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Log.i("LocalService", "Received start id " + startId + ": " +
		// intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		// mNM.cancel(R.string.local_service_started);

		// Tell the user we stopped.
		// Toast.makeText(this, R.string.local_service_stopped,
		// Toast.LENGTH_SHORT).show();

		// Toast.makeText(this, "Service destroyed ...",
		// Toast.LENGTH_LONG).show();

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

	private final File sdroot = new File(Environment
			.getExternalStorageDirectory().toString());
	private final File root = new File(Environment
			.getExternalStorageDirectory().toString() + "/TUCMensa");

	private String storage_state = Environment.getExternalStorageState();

	private NumberFormat nf = NumberFormat.getInstance();
	private NumberFormat nf2 = NumberFormat.getInstance();

	public enum status {
		nonExisting, working, Existing, Updated, nowUpdated, nonUpdated
	};

	public MensaService() {
		nf.setMinimumIntegerDigits(2); // The minimum Digits required is 2
		nf.setMaximumIntegerDigits(2); // The maximum Digits required is 2
		nf.setGroupingUsed(false);

		nf2.setMinimumIntegerDigits(4); // The minimum Digits required is 4
		nf2.setMaximumIntegerDigits(4); // The maximum Digits required is 4
		nf2.setGroupingUsed(false);

	}

	private String inWork_Image_working = "";
	private String inWork_Image_status = "";

	private void inWork_Image_set_working(String name) {
		synchronized (inWork_Image_working) {

			inWork_Image_working += "|" + name + "|";

		}
	}

	private void inWork_Image_remove_working(String name) {
		synchronized (inWork_Image_working) {
			inWork_Image_working = inWork_Image_working.replace("|" + name
					+ "|", "");
		}
	}

	private boolean inWork_Image_start_working(String name) {
		synchronized (inWork_Image_working) {
			if (!inWork_Image_working.contains("|" + name + "|")) {
				inWork_Image_set_working(name);
				return true;
			}

			return false;
		}
	}

	private void inWork_Image_set_ready(String name) {
		synchronized (inWork_Image_status) {
			if (inWork_Image_is_ready(name))
				inWork_Image_status = inWork_Image_status.replace("|" + name
						+ "u|", "|" + name + "r|");
			else
				inWork_Image_status += "|" + name + "r|";
		}
	}

	private void inWork_Image_set_updated(String name) {
		synchronized (inWork_Image_status) {
			if (inWork_Image_is_ready(name))
				inWork_Image_status = inWork_Image_status.replace("|" + name
						+ "r|", "|" + name + "u|");
			else
				inWork_Image_status += "|" + name + "u|";
		}
	}

	private void inWork_Image_remove_status(String name) {
		synchronized (inWork_Image_status) {
			inWork_Image_status = inWork_Image_status.replace(
					"|" + name + "r|", "");
			inWork_Image_status = inWork_Image_status.replace(
					"|" + name + "u|", "");

		}
	}

	private boolean inWork_Image_is_updated(String name) {
		synchronized (inWork_Image_status) {
			if (inWork_Image_status.contains("|" + name + "u|")) {
				return true;
			}

			return false;
		}
	}

	private boolean inWork_Image_is_ready(String name) {
		synchronized (inWork_Image_status) {
			if (inWork_Image_status.contains("|" + name + "r|")) {
				return true;
			}

			return false;
		}
	}

	public Bitmap getImage(String imgName, boolean isExistingCheck,
			int image_pixel_size) throws CustomException {

		getImage_status(imgName, false, isExistingCheck, image_pixel_size);

		return readImage(imgName, image_pixel_size);

	}

	private status prepareImage(String imgName, boolean updateNow,
			boolean isExistingCheck, int image_pixel_size)
			throws CustomException {

		String imgName_4 = nf2.format(Integer.parseInt(imgName)) + "_"
				+ nf2.format(image_pixel_size);

		if (inWork_Image_is_updated(imgName_4))
			return status.Updated;

		if (inWork_Image_is_ready(imgName_4) && !updateNow)
			return status.Existing;

		if (inWork_Image_start_working(imgName_4)) {

			if (!fileExists_Image(imgName, image_pixel_size) || updateNow) {
				if (isExistingCheck) {
					inWork_Image_remove_working(imgName_4);
					return status.nonExisting;
				}

				try {
					Boolean stat = loadIMAGEtoSD(imgName, image_pixel_size);
					inWork_Image_set_updated(imgName_4);
					inWork_Image_remove_working(imgName_4);
					if (stat)
						return status.nowUpdated;
					else
						return status.nonUpdated;

				} catch (CustomException e) {
					inWork_Image_remove_working(imgName_4);
					throw e;
				}
			}
			inWork_Image_set_ready(imgName_4);
			inWork_Image_remove_working(imgName_4);
			return status.Existing;
		}
		return status.working;
	}

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

	private synchronized boolean fileExists_Image(String name,
			int image_pixel_size) {

		return (new File(getFilename_Image(name, image_pixel_size))).exists();
	}

	private String getFilename_Image(String name, int image_pixel_size) {

		String string = root.toString() + "/essenprev_" + nf2.format(mYear)
				+ "_" + nf.format(mMonth) + "_" + nf.format(mDay) + "-"
				+ nf2.format(Integer.parseInt(name)) + "_"
				+ nf2.format(image_pixel_size) + ".png";
		return string;

	}

	// private String loadIMAGEtoSD_string = "";
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

	private String inWork_XML_working = "";
	private String inWork_XML_status = "";

	private void inWork_XML_set_working(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_working) {

			inWork_XML_working += "|" + Year + Month + Day + mensa + "|";

		}
	}

	private void inWork_XML_remove_working(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_working) {
			inWork_XML_working = inWork_XML_working.replace("|" + Year + Month + Day + mensa + "|",
					"");
		}
	}

	private boolean inWork_XML_start_working(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_working) {
			if (!inWork_XML_working.contains("|" + Year + Month + Day + mensa + "|")) {
				inWork_XML_set_working(mensa,Year,Month,Day);
				return true;
			}

			return false;
		}
	}

	private void inWork_XML_set_ready(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_status) {
			if (inWork_XML_is_ready(mensa,Year,Month,Day))
				inWork_XML_status = inWork_XML_status.replace(
						"|" + Year + Month + Day + mensa + "u|", "|" + Year + Month + Day + mensa + "r|");
			else
				inWork_XML_status += "|" + Year + Month + Day + mensa + "r|";
		}
	}

	private void inWork_XML_set_updated(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_status) {
			if (inWork_XML_is_ready(mensa,Year,Month,Day))
				inWork_XML_status = inWork_XML_status.replace(
						"|" + Year + Month + Day + mensa + "r|", "|" + Year + Month + Day + mensa + "u|");
			else
				inWork_XML_status += "|" + Year + Month + Day + mensa + "u|";
		}
	}

	private void inWork_XML_remove_status(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_status) {
			inWork_XML_status = inWork_XML_status
					.replace("|" + Year + Month + Day + mensa + "r|", "");
			inWork_XML_status = inWork_XML_status
					.replace("|" + Year + Month + Day + mensa + "u|", "");

		}
	}

	private boolean inWork_XML_is_updated(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_status) {
			if (inWork_XML_status.contains("|" + Year + Month + Day + mensa + "u|")) {
				return true;
			}

			return false;
		}
	}

	private boolean inWork_XML_is_ready(String mensa, int Year, int Month, int Day) {
		synchronized (inWork_XML_status) {
			if (inWork_XML_status.contains("|" + Year + Month + Day + mensa + "r|")) {
				return true;
			}

			return false;
		}
	}

	private status prepareXML(String mensa, int inYear, int inMonth, int inDay,
			boolean updateNow, boolean isExistingCheck) throws CustomException {

		if (inWork_XML_is_updated(mensa,inYear,inMonth,inDay))
			return status.Updated;

		if (inWork_XML_is_ready(mensa,inYear,inMonth,inDay) && !updateNow)
			return status.Existing;

		if (inWork_XML_start_working(mensa,inYear,inMonth,inDay)) {

			if (updateNow || !fileExists_XML(mensa,inYear,inMonth,inDay)) {
				if (isExistingCheck) {
					inWork_XML_remove_working(mensa,inYear,inMonth,inDay);
					return status.nonExisting;
				}

				try {
					Boolean stat = loadXMLtoSD(mensa,inYear,inMonth,inDay);
					inWork_XML_set_updated(mensa,inYear,inMonth,inDay);
					inWork_XML_remove_working(mensa,inYear,inMonth,inDay);
					if (stat)
						return status.nowUpdated;
					else
						return status.nonUpdated;

				} catch (CustomException e) {
					inWork_XML_remove_working(mensa,inYear,inMonth,inDay);
					throw e;
				}
			}
			inWork_XML_set_ready(mensa,inYear,inMonth,inDay);
			inWork_XML_remove_working(mensa,inYear,inMonth,inDay);
			return status.Existing;
		}
		return status.working;
	}

	public NodeList getXML(String mensa, boolean isExistingCheck)
			throws CustomException {

		return getXML(mensa, mYear, mMonth, mDay, isExistingCheck);

	}

	public NodeList getXML(String mensa, int Year, int Month, int Day,
			boolean isExistingCheck) throws CustomException {

		getXML_status(mensa, Year, Month, Day, false, isExistingCheck);

		return loadXMLintoRuntime(mensa, Year, Month, Day);

	}

	public status getXML_status(String mensa, boolean updateNow,
			boolean isExistingCheck) throws CustomException {

		return getXML_status(mensa, mYear, mMonth, mDay, updateNow,
				isExistingCheck);
	}

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

	private boolean loadXMLtoSD(String mensa, int Year, int Month, int Day)
			throws CustomException {
		if(mensa=="st")
			mensa="strana";
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
					&& readXMLs(mensa, Year, Month, Day).equals(hm)) {
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

	private synchronized boolean fileExists_XML(String mensa, int Year,
			int Month, int Day) {

		return (new File(getFilename_XML(mensa, Year, Month, Day))).exists();
	}

	private String getFilename_XML(String mensa, int Year, int Month, int Day) {
		String string = root.toString() + "/essenprev_" + nf2.format(Year)
				+ "_" + nf.format(Month) + "_" + nf.format(Day) + "_" + mensa
				+ ".xml";
		return string;

	}

	private Object FileSync = new Object();

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

	public NodeList loadXMLintoRuntime(String mensa) throws CustomException {

		Document doc = readXML(mensa, mYear, mMonth, mDay);
		NodeList nodes = doc.getElementsByTagName("essen");
		return nodes;

	}

	public NodeList loadXMLintoRuntime(String mensa, int Year, int Month,
			int Day) throws CustomException {

		Document doc = readXML(mensa, Year, Month, Day);
		NodeList nodes = doc.getElementsByTagName("essen");
		return nodes;

	}

	public Document readXML(String mensa, int Year, int Month, int Day)
			throws CustomException {
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

	public String readXMLs(String mensa, int Year, int Month, int Day)
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

	public void del_oldpic() {
		
		//Kann in nachfolgenden Versionen wieder weg
		CreateDir();
		
		
		Calendar caldel = new GregorianCalendar(mYear, mMonth, mDay);

		if (storage_state.contains("mounted")) {
			if (root.canWrite()) {

				if (root.isDirectory()) {

					String u1,u2mensa,imgName,imgpixelSize;
					int  u2day, u2month, u2year;
					imgName="";
					imgpixelSize="";
					u2mensa="";
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
								if(MimeType.equals("png"))
								imgName = filelist_of_cache[i]
										.toString().substring(
												root.toString().length() + 22,
												root.toString().length() + 26);
								
								if(MimeType.equals("png"))
								imgpixelSize = filelist_of_cache[i]
										.toString().substring(
												root.toString().length() + 27,
												root.toString().length() + 31);
								
								if(MimeType.equals("xml"))
								u2mensa= filelist_of_cache[i].toString()
								.substring(
										root.toString().length() + 22,
										root.toString().length() + 23);
								u2day = Integer.parseInt(filelist_of_cache[i].toString()
										.substring(
												root.toString().length() + 19,
												root.toString().length() + 21));
								u2month = Integer.parseInt(filelist_of_cache[i].toString()
										.substring(
												root.toString().length() + 16,
												root.toString().length() + 18));
								u2year = Integer.parseInt(filelist_of_cache[i].toString()
										.substring(
												root.toString().length() + 11,
												root.toString().length() + 15));
								
			
								// u2mensa = filelist_of_cache[i].toString()
								// .substring(
								// root.toString().length() + 22,
								// root.toString().length() + 24);

								Calendar caldel2 = new GregorianCalendar(
										u2year,
										u2month,
										u2day);

								if (caldel.compareTo(caldel2) == 1) {
									synchronized (FileSync) {

										if (MimeType.equals("xml") &&
												inWork_XML_start_working(u2mensa,u2year,u2month,u2day)
												) {
											inWork_XML_remove_status(u2mensa,u2year,u2month,u2day);
											filelist_of_cache[i].delete();
											inWork_XML_remove_working(u2mensa,u2year,u2month,u2day);
										}
										if (MimeType.equals("png") && inWork_Image_start_working(imgName
												+ "_" + imgpixelSize)
												) {
											inWork_Image_remove_status(imgName
													+ "_" + imgpixelSize);
											filelist_of_cache[i].delete();
											inWork_Image_remove_working(imgName
													+ "_" + imgpixelSize);
										}
									}
								}
							}
						} catch (Exception e)
						{

						}

					}
				}
			}
		}
	}

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