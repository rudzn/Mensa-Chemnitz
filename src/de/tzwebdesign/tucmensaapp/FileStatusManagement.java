package de.tzwebdesign.tucmensaapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.tzwebdesign.tucmensaapp.MensaService.filestatus;

/**
 * Klasse zur Filestatus speicherung
 */
public class FileStatusManagement {

	private class InternalObject {
		public String file;
		public filestatus status;

		// constructor
		public InternalObject(String fileIn, filestatus statusIn) {
			file = fileIn;
			status = statusIn;
		}
	}

	private List<InternalObject> statusList = new ArrayList<InternalObject>();

	/**
	 * @param fileIn Filename
	 * @param statusIn Status
	 */
	synchronized public void add(String fileIn, filestatus statusIn) {
		statusList.add(new InternalObject(fileIn, statusIn));

	}

	/**
	 * @param fileIn Filename
	 * @param statusIn Status
	 * @return True wenn beinhaltet
	 */
	synchronized public Boolean contains(String fileIn, filestatus statusIn) {
		for (InternalObject item : statusList) {
			if (item.file.equals(fileIn) && item.status.equals(statusIn))
				return true;
		}
		return false;
	}

	/**
	 * @param fileIn
	 *            Filename
	 * @return True wenn isReady
	 */
	synchronized public Boolean isReady(String fileIn) {
		for (InternalObject item : statusList) {
			if (item.file.equals(fileIn)
					&& item.status.equals(filestatus.ready))
				return true;
		}
		return false;
	}

	/**
	 * @param fileIn
	 *            Filename
	 * @return True wenn isUpdated
	 */
	synchronized public Boolean isUpdated(String fileIn) {
		for (InternalObject item : statusList) {
			if (item.file.equals(fileIn)
					&& item.status.equals(filestatus.updated))
				return true;
		}
		return false;
	}

	/**
	 * @param fileIn
	 *            Filename
	 */
	synchronized public void remove(String fileIn) {

		for (Iterator<InternalObject> it = statusList.iterator(); it.hasNext();)
			if (it.next().file.equals(fileIn))
				it.remove();

	}

	/**
	 * @param fileIn Filename
	 */
	synchronized public void setready(String fileIn) {
		remove(fileIn);

		add(fileIn, filestatus.ready);
	}

	/**
	 * @param fileIn Filename
	 */
	synchronized public void setupdated(String fileIn) {
		remove(fileIn);

		add(fileIn, filestatus.updated);
	}
}