package de.tzwebdesign.TUCMensa;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

public class Essen {

	public String name;
	public int nummer;
	public String text;
	public int bildnummer;
	public int wertung;
	public String preisgast;
	public String preismitarbeiter;
	public String preisstudent;
	public Boolean vegetarisch;
	public Boolean alkohol;
	public Boolean rind;
	public Boolean schwein;
	public String lastchange;

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
