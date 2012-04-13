package de.tzwebdesign.tucmensaapp;

/**
 * @author Tobias
 *
 */
public class Essen {

	/**
	 * Name
	 */
	public String name;
	
	/**
	 * Nummer
	 */
	public int nummer;
	
	/**
	 * BeschreibungsText
	 */
	public String text;
	
	/**
	 * Bildnummer
	 */
	public int bildnummer;
	
	/**
	 * Wertung der User
	 */
	public int wertung;
	
	/**
	 * Gastpreise
	 */
	public String preisgast;
	
	/**
	 * Mitarbeiterpreise
	 */
	public String preismitarbeiter;
	
	/**
	 * Studentenpreise
	 */
	public String preisstudent;
	
	/**
	 * vegetarisch
	 */
	public Boolean vegetarisch;
	
	/**
	 * mit alkohol
	 */
	public Boolean alkohol;
	
	/**
	 * mit rind
	 */
	public Boolean rind;
	
	/**
	 * mit schwein
	 */
	public Boolean schwein;
	
	/**
	 * letzte änderung de Essens als Time String
	 */
	public String lastchange;

	/**
	 * @param nameIn Name
	 * @param nummerIn Nummer
	 * @param textIn BeschreibungsText
	 * @param bildnummerIn Bildnummer
	 * @param wertungIn Wertung der User
	 * @param preisgastIn Gastpreise
	 * @param preismitarbeiterIn Mitarbeiterpreise
	 * @param preisstudentIn Studentenpreise
	 * @param vegetarischIn vegetarisch
	 * @param alkoholIn mit alkohol
	 * @param rindIn mit rind
	 * @param schweinIn mit schwein
	 * @param lastchangeIn letzte änderung de Essens als Time String
	 */
	public Essen(String nameIn, int nummerIn, String textIn, int bildnummerIn,
			int wertungIn, String preisgastIn, String preismitarbeiterIn,
			String preisstudentIn, Boolean vegetarischIn, Boolean alkoholIn,
			Boolean rindIn, Boolean schweinIn, String lastchangeIn) {

		name = nameIn;
		nummer = nummerIn;
		text = textIn;
		bildnummer = bildnummerIn;
		wertung = wertungIn;
		preisgast = preisgastIn;
		preismitarbeiter = preismitarbeiterIn;
		preisstudent = preisstudentIn;
		vegetarisch = vegetarischIn;
		alkohol = alkoholIn;
		rind = rindIn;
		schwein = schweinIn;
		lastchange = lastchangeIn;
	}

}
