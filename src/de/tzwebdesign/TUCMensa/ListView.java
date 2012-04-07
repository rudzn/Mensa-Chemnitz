package de.tzwebdesign.TUCMensa;

import java.io.File;

import java.text.NumberFormat;
import java.util.Calendar;

import org.w3c.dom.Attr;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

import android.view.Window;
import android.widget.LinearLayout;

import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ListView extends Activity {

	final Handler mHandler = new Handler();

	final Runnable completedloading = new Runnable() {
		public void run() {

			build();
			Toast.makeText(ListView.this, R.string.findNewEssen,
					Toast.LENGTH_LONG).show();

		}
	};
	final Runnable endIcon = new Runnable() {
		public void run() {
			setProgressBarIndeterminateVisibility(false);

		}
	};

	private void aktualisiere() {

		Thread t = new Thread() {

			public void run() {
				IOinstance.checkAllXML(mensa);
				boolean stat = IOinstance.LoadAllXML(mensa);

				if (stat)
					mHandler.post(completedloading);

				mHandler.post(endIcon);

			}

		};
		t.start();

	}

	private MensaService IOinstance;

	private boolean mIsBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			IOinstance = ((MensaService.LocalBinder) service).getService();

			setProgressBarIndeterminateVisibility(true);

			build();
			aktualisiere();

			// Tell the user about this for our demo.
			// Toast.makeText(TUCMensa.this,
			// "local_service_connected",Toast.LENGTH_LONG).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			IOinstance = null;

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
			bindService(new Intent(ListView.this, MensaService.class),
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

	File root = new File(Environment.getExternalStorageDirectory().toString()
			+ "/TUCMensa");
	String mensa, sprache;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		sv = new ScrollView(this);
		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		sv.addView(ll);

		this.setContentView(sv);

		doBindService();

	}

	ScrollView sv;
	LinearLayout ll;

	private void build() {

		try {

			ll.removeAllViewsInLayout();

			// ContentResolver contentResolver = context.getContentResolver();

			// final Cursor cursor =
			// contentResolver.query(Uri.parse("content://calendar/calendars"),
			// (new String[] { "_id", "displayName", "selected" }), null, null,
			// null);

			// while (cursor.moveToNext()) {

			// final String _id = cursor.getString(0);
			// final String displayName = cursor.getString(1);
			// final Boolean selected = !cursor.getString(2).equals("0");

			// System.out.println("Id: " + _id + " Display Name: " + displayName
			// + " Selected: " + selected);
			// }

			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);

			int offset = Integer.parseInt(settings.getString("offset", "0"));
			mensa = settings.getString("mensa", "rh");
			sprache = settings.getString("sprache",
					this.getString(R.string.sprache_default));

			nf.setMinimumIntegerDigits(2); // The minimum Digits required is 2
			nf.setMaximumIntegerDigits(2); // The maximum Digits required is 2
			nf.setGroupingUsed(false);

			nf2.setMinimumIntegerDigits(4); // The minimum Digits required is 2
			nf2.setMaximumIntegerDigits(4); // The maximum Digits required is 2
			nf2.setGroupingUsed(false);

			// setContentView(R.layout.listview);

			// File sdroot = new
			// File(Environment.getExternalStorageDirectory().toString());

			Calendar c = Calendar.getInstance();

			c.add(Calendar.HOUR, offset);

			int mYear;
			int mMonth;
			int mDay;

			NodeList nodes;

			while (true) {

				if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
					c.add(Calendar.DATE, 2);
				}
				if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
					c.add(Calendar.DATE, 1);
				}

				mYear = c.get(Calendar.YEAR);
				mMonth = c.get(Calendar.MONTH) + 1;
				mDay = c.get(Calendar.DAY_OF_MONTH);

				// NodeList nodes = loadXML(mYear, mMonth, mDay);

				try {
					nodes = IOinstance.getXML(mensa, mYear, mMonth, mDay, true);
					// nodes=loadXML( mYear, mMonth, mDay);

				} catch (CustomException e) {
					// if(e.getMessage()!= errors.XMLReadError.toString())
					// {
					// Toast.makeText(ListView.this,"BAM fehler!",Toast.LENGTH_LONG).show();
					// }
					// TODO

					break;
				}

				c.add(Calendar.HOUR, 24);

				TextView tw0 = new TextView(this);
				if (sprache.equals("de")) {
					tw0.setText(nf.format(mDay) + "." + nf.format(mMonth) + "."
							+ nf2.format(mYear));
				} else {
					tw0.setText(nf.format(mMonth) + "." + nf.format(mDay) + "."
							+ nf2.format(mYear));

				}

				tw0.setTypeface(null, 1);
				tw0.setTextSize(40);
				ll.addView(tw0);

				for (int i = 0; i < nodes.getLength(); i++) {
					// CheckBox cb = new CheckBox(this);
					// cb.setText("I'm dynamic!");
					// ll.addView(cb);

					Element element = (Element) nodes.item(i);

					NamedNodeMap attrs = element.getAttributes();
					TextView tw1 = new TextView(this);

					Attr attribute = (Attr) attrs.getNamedItem("name");
					tw1.setText(attribute.getValue());
					tw1.setTypeface(null, 1);
					tw1.setTextSize(20);
					ll.addView(tw1);

					TextView tw2 = new TextView(this);

					Attr attribute2 = (Attr) attrs.getNamedItem("eng");
					String eng_essen = attribute2.getValue();
					String text = "";
					attribute = (Attr) attrs.getNamedItem("name");
					if (sprache.equals("de") || eng_essen.length() == 0) {
						// tw2.setText(attribute.getValue());

						text = element.getFirstChild().getNodeValue().trim();
					} else {
						text = eng_essen;

					}

					tw2.setText(text);

					// tw2.setOnClickListener(new View.OnClickListener() {
					// public void onClick(View view) {
					// TextView tw = (TextView) findViewById(view.getId());
					// String hm= (String) tw.getText();

					// ContentValues event = new ContentValues();
					// event.put("calendar_id", calId);
					// event.put("title", "Event Title");
					// event.put("description", "Event Desc");
					// event.put("eventLocation", "Event Location");
					// long startTime = START_TIME_MS;
					// long endTime = END_TIME_MS;
					// event.put("dtstart", startTime);
					// event.put("dtend", endTime);
					// event.put("allDay", 1); // 0 for false, 1 for true

					// new AlertDialog.Builder(this)
					// .setTitle("Fehler")
					// .setMessage(
					// "Keine SD Karte gefunden! Bild nicht gecached!")
					// .show();
					// }
					// });

					ll.addView(tw2);

				}

				// Thread.sleep(2000);

			}

			/*
			 * RelativeLayout rl = new RelativeLayout(this);
			 * 
			 * 
			 * 
			 * Button t = new Button(this); t.setHeight(100); t.setWidth(100);
			 * t.setText("TEST123");
			 * 
			 * // x y ??? setContentView(rl);
			 */

		} catch (Exception e) {

			// TODO
		}
	}

	NumberFormat nf = NumberFormat.getInstance(); // Get Instance of
	NumberFormat nf2 = NumberFormat.getInstance(); // Get Instance of

}