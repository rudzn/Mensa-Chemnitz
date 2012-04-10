package de.tzwebdesign.TUCMensa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.tzwebdesign.TUCMensa.MensaService.filestatus;

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

	synchronized public void add(String fileIn, filestatus statusIn) {
		statusList.add(new InternalObject(fileIn, statusIn));

	}

	synchronized public Boolean contains(String fileIn, filestatus statusIn) {
		for (InternalObject item : statusList) {
			if (item.file.equals(fileIn) && item.status.equals(statusIn))
				return true;
		}
		return false;
	}

	synchronized public Boolean isReady(String fileIn) {
		for (InternalObject item : statusList) {
			if (item.file.equals(fileIn)
					&& item.status.equals(filestatus.ready))
				return true;
		}
		return false;
	}

	synchronized public Boolean isUpdated(String fileIn) {
		for (InternalObject item : statusList) {
			if (item.file.equals(fileIn)
					&& item.status.equals(filestatus.updated))
				return true;
		}
		return false;
	}

	synchronized public void remove(String fileIn) {
		// for (InternalObject item : statusList) {
		// if (item.file.equals(fileIn))
		// statusList.remove(item);
		// }

		for (Iterator<InternalObject> it = statusList.iterator(); it.hasNext();)
			if (it.next().file.equals(fileIn))
				it.remove();
	}

	synchronized public void setready(String fileIn) {
		remove(fileIn);

		add(fileIn, filestatus.ready);
	}

	synchronized public void setupdated(String fileIn) {
		remove(fileIn);

		add(fileIn, filestatus.updated);
	}
}