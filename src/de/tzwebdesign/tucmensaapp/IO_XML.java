package de.tzwebdesign.tucmensaapp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Environment;

import de.tzwebdesign.tucmensaapp.CustomException.errors;

public class IO_XML {

	private MensaService mensaService;

	private Object FileSync = new Object();

	private NumberFormat twoDigitsNumberformat = NumberFormat.getInstance();
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

	/**
	 * Konstruktor
	 */
	public IO_XML(MensaService parent) {
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
	public synchronized boolean fileExists_XML(String mensa, int Year,
			int Month, int Day) {

		return (new File(getFilename_XML(mensa, Year, Month, Day))).exists();
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
	public boolean loadXMLtoSD(String mensa, int Year, int Month, int Day)
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
				if (Environment.getExternalStorageState().contains("mounted")) {

					if (sdroot.canWrite()) {

						if (!root.exists()) {
							mensaService.CreateDir();
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
	 * Liest Essen aus XML Datei für bestimmtes Datum
	 * 
	 * @param mensa
	 *            rh oder st
	 * @param Year
	 * @param Month
	 * @param Day
	 * @return EssenListe
	 * @throws CustomException
	 */
	public List<Essen> getEssenList(String mensa, int Year, int Month, int Day)
			throws CustomException {

		List<Essen> essenListe = new ArrayList<Essen>();

		Document doc = readXMLasXMLDocument(mensa, Year, Month, Day);
		NodeList nodes_temp = doc.getElementsByTagName("essen");

		for (int essen_position = 0; essen_position < nodes_temp.getLength(); essen_position++) {

			String name;
			int nummer;
			String text;
			int bildnummer;
			int wertung;
			String preisgast;
			String preismitarbeiter;
			String preisstudent;
			Boolean vegetarisch;
			Boolean alkohol;
			Boolean rind;
			Boolean schwein;
			String lastchange;

			NamedNodeMap attrs = nodes_temp.item(essen_position)
					.getAttributes();

			Attr attribute = (Attr) attrs.getNamedItem("name");
			name = attribute.getValue();

			attribute = (Attr) attrs.getNamedItem("number");
			nummer = Integer.valueOf(attribute.getValue());

			text = nodes_temp.item(essen_position).getFirstChild()
					.getNodeValue().trim();

			attribute = (Attr) attrs.getNamedItem("bild");
			bildnummer = Integer.valueOf(attribute.getValue());

			attribute = (Attr) attrs.getNamedItem("wertung");
			wertung = Integer.valueOf(attribute.getValue());

			attribute = (Attr) attrs.getNamedItem("preisg");
			preisgast = attribute.getValue();

			attribute = (Attr) attrs.getNamedItem("preism");
			preismitarbeiter = attribute.getValue();

			attribute = (Attr) attrs.getNamedItem("preiss");
			preisstudent = attribute.getValue();

			attribute = (Attr) attrs.getNamedItem("vegetarisch");
			vegetarisch = attribute.getValue() != "false";

			attribute = (Attr) attrs.getNamedItem("alk");
			alkohol = attribute.getValue() != "false";

			attribute = (Attr) attrs.getNamedItem("rind");
			rind = attribute.getValue() != "false";

			attribute = (Attr) attrs.getNamedItem("schwein");
			schwein = attribute.getValue() != "false";

			attribute = (Attr) attrs.getNamedItem("lastchange");
			lastchange = attribute.getValue();

			Essen newEssen = new Essen(name, nummer, text, bildnummer, wertung,
					preisgast, preismitarbeiter, preisstudent, vegetarisch,
					alkohol, rind, schwein, lastchange);

			essenListe.add(newEssen);
		}

		return essenListe;

	}

	/**
	 * delete XML file by name
	 * 
	 * @param filename
	 */
	public void deleteXML(File file) {
		synchronized (FileSync) {
			file.delete();
		}
	}

}
