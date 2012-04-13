package de.tzwebdesign.tucmensaapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import de.tzwebdesign.tucmensaapp.CustomException.errors;

/**
 * Klasse zur verwaltung von Bildern
 */
public class IO_Image {

	private MensaService mensaService;

	/**
	 * zweistellig mit führender Null wenn nötig
	 */
	private NumberFormat twoDigitsNumberformat = NumberFormat.getInstance();

	/**
	 * Vierstellig mit führender Null wenn nötig
	 */
	private NumberFormat fourDigitsNumberformat = NumberFormat.getInstance();

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

	private Object fileSempahor = new Object();

	/**
	 * Konstruktor
	 * @param parent Objekt welches dieses Objekt Initialisiert hat
	 */
	public IO_Image(MensaService parent) {

		mensaService = parent;

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
	 * Lädt Bild aus Netz und speichert es auf SD
	 * 
	 * @param name
	 *            Bildname
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return True wenn Bild aktualisiert wurde (nicht implementiert=>immer False)
	 * @throws CustomException CustomException
	 */
	public boolean loadIMAGEtoSD(int name, int image_pixel_size)
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

	}

	/**
	 * Liest Bild von SD
	 * 
	 * @param name
	 *            Bildname
	 * @param image_pixel_size
	 *            Bildgröße
	 * @return Bild
	 * @throws CustomException CustomException
	 */
	public Bitmap readImage(int name, int image_pixel_size)
			throws CustomException {
		synchronized (fileSempahor) {

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
	private void saveImage(int name, Bitmap image, int image_pixel_size)
			throws CustomException {
		synchronized (fileSempahor) {

			try {

				if (Environment.getExternalStorageState().contains("mounted")) {
					if (sdroot.canWrite()) {

						if (!root.exists()) {
							mensaService.CreateDir();
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
	public boolean fileExists_Image(int name, int image_pixel_size) {
		synchronized (fileSempahor) {

			return (new File(getFilename_Image(name, image_pixel_size)))
					.exists();
		}
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
	private String getFilename_Image(int name, int image_pixel_size) {

		String string = root.toString() + "/essenprev_"
				+ fourDigitsNumberformat.format(mensaService.getmYear()) + "_"
				+ twoDigitsNumberformat.format(mensaService.getmMonth()) + "_"
				+ twoDigitsNumberformat.format(mensaService.getmDay()) + "-"
				+ fourDigitsNumberformat.format(name) + "_"
				+ fourDigitsNumberformat.format(image_pixel_size) + ".png";
		return string;

	}

	/**
	 * delete Image by Name
	 * 
	 * @param file Datei
	 */
	public void deleteXML(File file) {
		synchronized (fileSempahor) {

			file.delete();
		}
	}
}
