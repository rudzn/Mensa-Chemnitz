package de.tzwebdesign.TUCMensa;

public class CustomException extends Exception {

	/**
	 * 
	 */
	public enum errors {
		ImageDownloadError, ImageWriteError, ImageReadError, WriteProtectedSD, MissingSD, DownloadTimeout, URLError, ConnectionError, ImageWorkerTimeout, XMLReadError, XMLSAXError, XMLParserError, XMLWriteError, XMLIOError, XMLNoEssenFound, XMLWorkerTimeout, XMLWorkerSleeperFailed, ImageWorkerSleeperFailed
	};

	private static final long serialVersionUID = -6795412236835007571L;

	public CustomException(errors code) {
		super(code.toString());
	}

}
