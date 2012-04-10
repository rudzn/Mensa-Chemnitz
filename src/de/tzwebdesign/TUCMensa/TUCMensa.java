package de.tzwebdesign.TUCMensa;

import java.text.NumberFormat;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import de.tzwebdesign.TUCMensa.MensaService.status;
import de.tzwebdesign.TUCMensa.R;

import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import android.graphics.Bitmap;

import android.os.Bundle;
import android.os.IBinder;

import android.os.Handler;
import android.preference.PreferenceManager;

import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.Window;

import android.view.MotionEvent;

import android.widget.ImageView;
import android.widget.ProgressBar;

import android.widget.RelativeLayout;

import android.widget.TextView;

public class TUCMensa extends Activity implements OnGestureListener {

	final Handler mHandler = new Handler();

	final Runnable completedloading = new Runnable() {
		public void run() {
			setProgressBarIndeterminateVisibility(false);
		}
	};
	final Runnable PullImage_animation_end = new Runnable() {
		public void run() {
			ProgressBar progress1 = (ProgressBar) findViewById(R.id.progressbar_default);
			progress1.setVisibility(View.INVISIBLE);
		}
	};

	final Runnable PushImage = new Runnable() {
		public void run() {
			PushImageIn();
		}
	};
	final Runnable PullImage_animation = new Runnable() {
		public void run() {
			ProgressBar progress1 = (ProgressBar) findViewById(R.id.progressbar_default);
			progress1.setVisibility(View.VISIBLE);
		}
	};
	final Runnable showdataXML = new Runnable() {
		public void run() {

			refresh();

		}
	};
	final Runnable ShowError = new Runnable() {
		public void run() {

			PushError();

		}
	};

	// boolean[] ImageLoadLock;

	private String Error = "";

	private void setError(String name) {
		synchronized (Error) {
			Error = name;
		}
	}

	private String getError() {
		synchronized (Error) {
			return Error;
		}
	}

	/**
	 * Speichert die derzeit angezeigten Essen
	 */
	private List<Essen> nodes;

	/**
	 * Semaphor damit Nodelist nicht gleochzeitig geschrieben und gelesen wird
	 */
	private Object lock1 = new Object();

	/**
	 * Speichert die aktuelle essensliste auf nodes
	 * 
	 * @param name
	 */
	private void setNodes(List<Essen> essenListe) {
		synchronized (lock1) {
			nodes = essenListe;
		}
	}

	/**
	 * liest die lokale nodeliste von nodes
	 * 
	 * @return
	 */
	private List<Essen> getNodes() {
		synchronized (lock1) {
			return nodes;
		}
	}

	private boolean config_update = true;

	private NumberFormat twoDigitsNumberformat = NumberFormat.getInstance();
	private NumberFormat fourDigitsNumberformat = NumberFormat.getInstance();

	/**
	 * Position des aktuellen Essens
	 */
	Integer essen_position = 0;

	private Bitmap image = null;

	private GestureDetector gestureScanner;

	private MensaService mensaService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		// startActivity(new Intent(this, ListView.class));
		gestureScanner = new GestureDetector(this);

		setContentView(R.layout.main);

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
		// Toast.makeText(TUCMensa.this,
		// "local_service_disconnected",Toast.LENGTH_LONG).show();

		it = new Intent(this, ListView.class);

	}

	private Intent it;

	private boolean mIsBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mensaService = ((MensaService.LocalBinder) service).getService();

			Resume();

			// Tell the user about this for our demo.
			// Toast.makeText(TUCMensa.this,
			// "local_service_connected",Toast.LENGTH_LONG).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mensaService = null;

			// Toast.makeText(TUCMensa.this,
			// "local_service_disconnected",Toast.LENGTH_LONG).show();
		}
	};

	void doBindService() {
		if (!mIsBound) {
			// Establish a connection with the service. We use an explicit
			// class name because we want a specific service implementation that
			// we know will be running in our own process (and thus won't be
			// supporting component replacement by other applications).
			bindService(new Intent(TUCMensa.this, MensaService.class),
					mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	private boolean ListViewShowed = false;

	private void Resume() {
		get_pref();
		if (config_update || mensaService.checkdate()) {
			essen_position = 0;
			config_update = false;

			if (mensaService.checkdate())
				ListViewShowed = false;

			ladeService();

		}
		if (mensaService.config.ListViewFirst && !ListViewShowed) {
			startActivity(it);
			// Toast.makeText(TUCMensa.this,"Starte Activity!",Toast.LENGTH_SHORT).show();
		}
		ListViewShowed = true;
	}

	/**
	 * Lädt Essen aus Speicher wenn verfügbar und startet danach eine
	 * aktualisierung (ansonsten gleich aus Netz geladen)
	 */
	private void ladeService() {

		Thread t = new Thread() {

			public void run() {
				try {

					setNodes(mensaService.getEssenList(false));
					mHandler.post(showdataXML);

					mensaService.checkAllXML();
					if (!mensaService.config.ListViewFirst)
						mensaService.LoadAllXML();

					try {
						if (mensaService.getXML_status(true, false) == status.Updated)
							setNodes(mensaService.getEssenList(false));

					} catch (CustomException e) {
						// Verwerfe
					}

					prepareAllImages();

					mensaService.deleteOldFiles();

					mHandler.post(completedloading);

				} catch (CustomException e) {
					setError(e.getMessage());

					mHandler.post(ShowError);
					mHandler.post(completedloading);
				}
			}

		};
		setProgressBarIndeterminateVisibility(true);
		t.start();

	}

	/**
	 * Semaphor damit nur UI oder Service auf Bildobjekt schreibt/liest aber
	 * nicht gleichzeitig
	 */
	private Object lock2 = new Object();

	/**
	 * Name des Bildes, wenn schnell geblättert wird, und die Threads vom
	 * Bildladen zurückkehren, soll nur das zuletzt gesuchte Bild noch
	 * gespeichert werden Ansonsten Flackern die Bilder in wilder reihenfolge
	 * vorm Nutzer, und am ende bleibt beliebig eins stehen.
	 */
	private int imageState;

	/**
	 * Gibt Bildzurück was Service in image zwischengespeichert hat
	 * 
	 * @return Bild
	 */
	private Bitmap getimage() {
		synchronized (lock2) {
			return image;
		}
	}

	/**
	 * Service speichert hier auf image das geholte Bild ab
	 * 
	 * @param name
	 *            Bildname
	 * @param bm
	 *            Bild
	 */
	private void setimage(int name, Bitmap bm) {
		synchronized (lock2) {
			if (imageState == name)
				image = bm;
		}
	}

	/**
	 * Setzt Bild auf Null
	 * 
	 * @param name
	 *            Bildname
	 */
	private void setimageState(int name) {
		synchronized (lock2) {
			imageState = name;
			image = null;
		}
	}

	/**
	 * Lädt in einem Thread das für die UI benötigte Bild asyncron
	 * 
	 * @param value
	 *            Bildname
	 */
	private void pullImage(int value) {

		final int name = value;

		Thread t = new Thread() {

			public void run() {

				try {
					if (mensaService.getImage_status(name, false, true) == status.nonExisting)
						mHandler.post(PullImage_animation);

					setimage(name, mensaService.getImage(name, false));
					mHandler.post(PushImage);

					mHandler.post(PullImage_animation_end);

				} catch (CustomException e) {

					setError(e.getMessage());

					mHandler.post(ShowError);
					mHandler.post(PullImage_animation_end);
				}

			}

		};

		t.start();

	}

	/**
	 * Startet einen Thread der im Hintergrund restliche benötige Bilder
	 * vorbereitet/runterlädt
	 */
	private void prepareAllImages() {
		if (!mensaService.config.imageloading)
			return;

		Thread t = new Thread() {

			public void run() {

				List<Essen> Nodes_temp = getNodes();

				for (int i = 0; i < Nodes_temp.size(); i++) {
					Essen element = Nodes_temp.get(i);

					// prepareImage(attribute.getValue(),i);

					try {
						mensaService.getImage_status(element.bildnummer, true,
								false);
					} catch (CustomException e) {
						// Verwerfe
					}

				}

			}

		};

		t.start();

	}

	@Override
	public boolean onTouchEvent(MotionEvent me)

	{

		return gestureScanner.onTouchEvent(me);

	}

	private boolean scrollstart = false;

	@Override
	public boolean onDown(MotionEvent e)

	{
		scrollstart = true;
		// viewA.setText("-" + "DOWN" + "-");

		// Toast.makeText(this, "onDown",
		// 1).show();

		return true;

	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY)

	{

		// Toast.makeText(this, "onFling",
		// 1).show();

		return true;

	}

	@Override
	public void onLongPress(MotionEvent e)

	{

		// Toast.makeText(this, "onLongPress",
		// 1).show();

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY)

	{
		if (!scrollstart) {
			return true;
		}
		// If distanceX > 0: scroll in right direction
		// If distanceX < 0: scroll in left direction

		if (distanceX > 0) {
			// Toast.makeText(this, "right", Toast.LENGTH_SHORT).show();
			forwardnow();
		}
		if (distanceX < 0) {
			// Toast.makeText(this, "left",Toast.LENGTH_SHORT).show();

			backwardnow();
		}
		// Toast.makeText(this, "onScroll",
		// 1).show();

		scrollstart = false;
		return true;

	}

	@Override
	public void onShowPress(MotionEvent e)

	{

		// Toast.makeText(this, "onShowPress",
		// 1).show();
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e)

	{

		// Toast.makeText(this, "onSingleTapUp",
		// 1).show();

		return true;

	}

	/**
	 * Lädt Einstellungen
	 */
	private void get_pref() {

		mensaService.refreshconfig();

		ImageView image1 = (ImageView) findViewById(R.id.ImageView01);
		if (!mensaService.config.imageSizeSmall) {
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.FILL_PARENT,
					RelativeLayout.LayoutParams.FILL_PARENT);
			lp.setMargins(10, 0, 10, 0);
			lp.addRule(RelativeLayout.BELOW, R.id.TextView04);
			lp.addRule(RelativeLayout.ABOVE, R.id.TextView01);
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
			image1.setLayoutParams(lp);

		} else {
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			lp.setMargins(10, 0, 10, 0);
			lp.addRule(RelativeLayout.BELOW, R.id.TextView04);
			lp.addRule(RelativeLayout.ABOVE, R.id.TextView01);
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
			image1.setLayoutParams(lp);

		}

	}

	@Override
	public void onResume() {
		super.onResume();

		if (mIsBound)
			Resume();

		doBindService();

	}

	/**
	 * Erzeugt das Menü für den Menübutton
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.Konfiguration);
		menu.add(0, 1, 0, R.string.Listenansicht);

		return true;
	}

	/**
	 * Verarbeitet den Klick auf Menüelemente
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			config();
			return true;
		case 1:

			// ListView.IOinstance=IOinstance;

			Intent it = new Intent(this, ListView.class);

			startActivity(it);
			return true;

		}
		return false;
	}

	/**
	 * Ändert das angezeigte Bild im UI
	 */
	private synchronized void PushImageIn() {

		Bitmap image_temo = getimage();
		if (image_temo == null)
			return;

		ImageView image1;
		image1 = (ImageView) findViewById(R.id.ImageView01);

		image1.setImageBitmap(image_temo);

	}

	/**
	 * Lädt Essensdaten&Bild ins UI
	 */
	private synchronized void refresh() {

		Essen element = getNodes().get(essen_position);

		ImageView image1;
		image1 = (ImageView) findViewById(R.id.ImageView01);

		image1.setImageBitmap(Bitmap.createBitmap(100, 100,
				Bitmap.Config.ALPHA_8));

		setimageState(element.bildnummer);
		pullImage(element.bildnummer);

		// ////////////

		TextView TextView_Data = (TextView) findViewById(R.id.TextView04);
		if (mensaService.config.sprache.equals("de")) {
			TextView_Data.setText(twoDigitsNumberformat.format(mensaService
					.getmDay())
					+ "."
					+ twoDigitsNumberformat.format(mensaService.getmMonth())
					+ "."
					+ fourDigitsNumberformat.format(mensaService.getmYear()));
		} else {
			TextView_Data.setText(twoDigitsNumberformat.format(mensaService
					.getmMonth())
					+ "."
					+ twoDigitsNumberformat.format(mensaService.getmDay())
					+ "."
					+ fourDigitsNumberformat.format(mensaService.getmYear()));
		}
		// ////////////

		TextView TextView_Name = (TextView) findViewById(R.id.TextView02);

		TextView_Name.setText(element.name);

		// ////////////

		TextView TextView_Preis = (TextView) findViewById(R.id.TextView03);

		if (mensaService.config.preiskat == "g")
			TextView_Preis.setText(this.getString(R.string.Preiskat) + " "
					+ element.preisgast + "€");
		if (mensaService.config.preiskat == "m")
			TextView_Preis.setText(this.getString(R.string.Preiskat) + " "
					+ element.preismitarbeiter + "€");
		if (mensaService.config.preiskat == "s")
			TextView_Preis.setText(this.getString(R.string.Preiskat) + " "
					+ element.preisstudent + "€");

		// /////////////////
		TextView TextView1 = (TextView) findViewById(R.id.TextView01);

		if (mensaService.config.sprache.equals("de")) {

			TextView1.setText(element.text);
		} else {

			TextView1.setText(element.text);
		}
	}

	/**
	 * Gibt einen Error im UI aus
	 */
	private synchronized void PushError() {
		TextView TextView04 = (TextView) findViewById(R.id.TextView04);

		TextView04.setText(getError());
	}

	public void backward(View Button02) {

		backwardnow();
	}

	/**
	 * Wechselt zum vorherigen Essen
	 */
	public void backwardnow() {

		List<Essen> nodes_temp = getNodes();

		if (nodes_temp == null)
			return;
		essen_position = (essen_position - 1 + nodes_temp.size())
				% nodes_temp.size();
		refresh();

	}

	public void forward(View Button03) {

		forwardnow();
	}

	/**
	 * Wechselt zum nächsten Essen
	 */
	private void forwardnow() {

		List<Essen> nodes_temp = getNodes();

		if (nodes_temp == null)
			return;
		essen_position = (essen_position + 1) % nodes_temp.size();
		refresh();

	}

	/**
	 * Startet die Konfiguration Activity
	 */
	private void config() {
		config_update = true;
		Intent it = new Intent(this, Preferences.class);

		startActivity(it);

		// A toast is a view containing a quick little message for the user.
		// Toast.makeText(this,
		// "Here you can maintain your user credentials.",
		// Toast.LENGTH_LONG).show();

	}

}