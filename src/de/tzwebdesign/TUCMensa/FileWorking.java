package de.tzwebdesign.TUCMensa;

import java.util.ArrayList;
import java.util.List;

/**
 * Speichert gerade in arbeit befindliche Dateien (Multithreading, doppelte
 * arbeit erkennen und vermeiden)
 */
public class FileWorking {

	/**
	 * Speichert die gerade in arbeit befindlichen Dateien
	 */
	private List<String> fileList = new ArrayList<String>();

	/**
	 * löscht die Datei wieder aus 'in arbeit'
	 * 
	 * @param name
	 *            Dateiname
	 */
	synchronized public void remove(String name) {

		fileList.remove(name);

	}

	/**
	 * Setzt die Datei als 'in arbeit' wenn noch nicht als 'in arbeit' markiert
	 * 
	 * @param name
	 *            Dateiname
	 * @return True wenn 'in arbeit' gesetzt wurde, False wenn schon 'in arbeit'
	 */
	synchronized public boolean start(String name) {

		if (!fileList.contains(name)) {
			fileList.add(name);
			return true;
		}

		return false;

	}

}