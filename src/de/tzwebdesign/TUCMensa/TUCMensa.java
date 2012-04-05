package de.tzwebdesign.TUCMensa;


import java.text.NumberFormat;


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


public class TUCMensa extends Activity implements OnGestureListener{

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

	//boolean[] ImageLoadLock;

	
	
	private String Error="";
	
	private void setError(String name) {
        synchronized(Error) {
		Error=name;
        }
    }
	
	private String getError() {
        synchronized(Error) {
        	return Error;
        }
    }



	private NodeList nodes;
	private Object lock1 = new Object();

	
	private void setNodes(NodeList name) {
        synchronized(lock1) {
        	nodes=name;
        }
    }
	
	private NodeList getNodes() {
        synchronized(lock1) {
        	return nodes;
        }
    }
	

	private String preiskat, mensa, sprache;
	private boolean imageloading=false;
	private boolean imagesize=false;
	private int image_pixel_size;
	private boolean config_update = true;
	private boolean ListViewFirst=false;





	private NumberFormat nf = NumberFormat.getInstance();
	private NumberFormat nf2 = NumberFormat.getInstance();

	
	
	Integer essen_position = 0;

	private Bitmap image = null;


	
	private GestureDetector gestureScanner;
	
	private MensaService IOinstance;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);


		//startActivity(new Intent(this, ListView.class));
		gestureScanner = new GestureDetector(this);
		
		
		
		setContentView(R.layout.main);

		nf.setMinimumIntegerDigits(2); // The minimum Digits required is 2
		nf.setMaximumIntegerDigits(2); // The maximum Digits required is 2
		nf.setGroupingUsed(false);

		nf2.setMinimumIntegerDigits(4); // The minimum Digits required is 4
		nf2.setMaximumIntegerDigits(4); // The maximum Digits required is 4
		nf2.setGroupingUsed(false);
       // Toast.makeText(TUCMensa.this, "local_service_disconnected",Toast.LENGTH_LONG).show();
	
		
		it=new Intent(this, ListView.class);
		
	}
	
	private Intent it;
	

	private boolean mIsBound;


	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	    	IOinstance = ((MensaService.LocalBinder)service).getService();
	        
	        
			Resume();
			
			
			
			
	        // Tell the user about this for our demo.
	        //Toast.makeText(TUCMensa.this, "local_service_connected",Toast.LENGTH_LONG).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	    	IOinstance = null;
	        
	        //Toast.makeText(TUCMensa.this, "local_service_disconnected",Toast.LENGTH_LONG).show();
	    }
	};

	void doBindService() {
	    if (!mIsBound) {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
	    bindService(new Intent(TUCMensa.this, MensaService.class), mConnection, Context.BIND_AUTO_CREATE);
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
	
	
	
	
	
	
	
	
	
	
	
	
	private boolean ListViewShowed=false;

	private void Resume() {
		get_pref();
		if (config_update || IOinstance.checkdate()) {
			essen_position = 0;
			config_update = false;

			if(IOinstance.checkdate())
				ListViewShowed=false;


			
			ladeService();
			
		}
		if(ListViewFirst && !ListViewShowed){		
			startActivity(it);
			//Toast.makeText(TUCMensa.this,"Starte Activity!",Toast.LENGTH_SHORT).show();
		}
		ListViewShowed=true;
	}

	
	
	private void ladeService() {

		Thread t = new Thread() {

			public void run() {
			try{
					
						

							
								setNodes(IOinstance.getXML(mensa,false));
								mHandler.post(showdataXML);
								
								IOinstance.checkAllXML(mensa);
								if(!ListViewFirst)
								IOinstance.LoadAllXML(mensa);
								
								
								try {
									if(IOinstance.getXML_status(mensa,true,false)==status.Updated)
										setNodes(IOinstance.getXML(mensa,false));
									
								} catch (CustomException e) {
									//Verwerfe
								}
								

								prepareAllImages();
								
								
								
								IOinstance.del_oldpic();
								
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
	private Object lock2 = new Object();


	private String imageState;
	

	private Bitmap getimage() {
        synchronized(lock2) {
        	return image;
        }
    }
	
	private void setimage(String name,Bitmap bm) {
        synchronized(lock2) {
        	if(imageState==name)
        		image=bm;
        }
    }
	private void setimageState(String name) {
        synchronized(lock2) {
        	imageState=name;
        	image=null;
        }
    }

	
	private void pullImage(String value) {


		final String name = value;

		
	
		
		
		Thread t = new Thread() {

			public void run() {

			
				
				try {
							if(IOinstance.getImage_status(name,false,true,image_pixel_size)==status.nonExisting)
								mHandler.post(PullImage_animation);
							
								setimage(name,IOinstance.getImage(name,false,image_pixel_size));
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
	private void prepareAllImages() {
		if(!imageloading)return;

		Thread t = new Thread() {

			public void run() {
				
				NodeList Nodes_temp=getNodes();
				
				for(int i=0; i<Nodes_temp.getLength();i++)
				{
					NamedNodeMap attrs = Nodes_temp.item(i).getAttributes();
					Attr attribute = (Attr) attrs.getNamedItem("bild");
				
					//prepareImage(attribute.getValue(),i);
					
				try {
					IOinstance.getImage_status(attribute.getValue(),true,false,image_pixel_size);
				} catch (CustomException e) {
				//Verwerfe
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
	
	private boolean scrollstart=false;
	@Override	  
    public boolean onDown(MotionEvent e)
 
    {
		scrollstart=true;
        //viewA.setText("-" + "DOWN" + "-");
	
		// Toast.makeText(this, "onDown",
		// 1).show();

        return true;
 
    }
	

    @Override
 
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
 
    {
 
    	// Toast.makeText(this, "onFling",
    	//		 1).show();
 
        return true;
 
    }
 
    
 
    @Override
 
    public void onLongPress(MotionEvent e)
 
    {
 
    	// Toast.makeText(this, "onLongPress",
    	//		 1).show();
 
    }
 
    
 
    @Override
 
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
 
    {   
    	if(!scrollstart)
    	{
    		return true;
    	}
    	//If distanceX > 0: scroll in right direction 
    	//If distanceX < 0: scroll in left direction 
    	
    	if(distanceX>0){
        	 //Toast.makeText(this, "right",        			 Toast.LENGTH_SHORT).show();
    		forwardnow();
    	}
    	if(distanceX<0)
    	{
        	 //Toast.makeText(this, "left",Toast.LENGTH_SHORT).show();
    		
    		backwardnow();
    	}
    	// Toast.makeText(this, "onScroll",
    	//		 1).show();
    	
    	scrollstart=false;
        return true;
 
    }
 
    
 
    @Override
 
    public void onShowPress(MotionEvent e)
 
    {
 
    	// Toast.makeText(this, "onShowPress",
    	//		 1).show();
    }    
 
    
 
    @Override
 
    public boolean onSingleTapUp(MotionEvent e)    
 
    {
 
    	// Toast.makeText(this, "onSingleTapUp",
    	//		1).show();
 
        return true;
 
    }

    
    
	private void get_pref() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		//offset = Integer.parseInt(settings.getString("offset", "0"));
		IOinstance.setdate();
		ListViewFirst = settings.getBoolean("ListViewFirst", false);
		preiskat = settings.getString("preiskat", "s");
		mensa = settings.getString("mensa", "rh");
		sprache = settings.getString("sprache", this
				.getString(R.string.sprache_default));
		imageloading = settings.getBoolean("imageloading", false);
		imagesize = settings.getBoolean("imagesize", false);
		
	    	if(settings.getBoolean("image_pixel_size_big", true)==true)
	    		image_pixel_size=350;
	    	else
	    		image_pixel_size=190;
    	
		ImageView image1 = (ImageView) findViewById(R.id.ImageView01);
		if(!imagesize)
		{
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT); 
		lp.setMargins(10, 0, 10, 0); 
		lp.addRule(RelativeLayout.BELOW, R.id.TextView04); 
		lp.addRule(RelativeLayout.ABOVE, R.id.TextView01); 
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL); 
		image1.setLayoutParams(lp);
		
		}
		else
		{
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT); 
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
		
		if(mIsBound)
		Resume();
		
		doBindService();


	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.Konfiguration);
		menu.add(0, 1, 0, R.string.Listenansicht);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			config();
			return true;
		case 1:
			
			//ListView.IOinstance=IOinstance;
			
			Intent it=new Intent(this, ListView.class);
			
			startActivity(it);
			return true;

		}
		return false;
	}

	




	private synchronized void PushImageIn() {
		
		Bitmap image_temo= getimage();
		if(image_temo==null)
			return;
		
		ImageView image1;
		image1 = (ImageView) findViewById(R.id.ImageView01);

		image1.setImageBitmap(image_temo);
		
		

	}

	private synchronized void refresh() {
	
		NodeList nodes_temp=getNodes();
		
		NamedNodeMap attrs = nodes_temp.item(essen_position).getAttributes();
		Attr attribute = (Attr) attrs.getNamedItem("bild");

		ImageView image1;
		image1 = (ImageView) findViewById(R.id.ImageView01);

		image1.setImageBitmap(Bitmap.createBitmap(100, 100,
				Bitmap.Config.ALPHA_8));
		
		setimageState(attribute.getValue());
		pullImage(attribute.getValue());

		// ////////////

		TextView TextView_Data = (TextView) findViewById(R.id.TextView04);
		if (sprache.equals("de")) {
			TextView_Data.setText(nf.format(IOinstance.getmDay()) + "." + nf.format(IOinstance.getmMonth())
					+ "." + nf2.format(IOinstance.getmYear()));
		} else {
			TextView_Data.setText(nf.format(IOinstance.getmMonth()) + "." + nf.format(IOinstance.getmDay())
					+ "." + nf2.format(IOinstance.getmYear()));
		}
		// ////////////

		attribute = (Attr) attrs.getNamedItem("name");
		TextView TextView_Name = (TextView) findViewById(R.id.TextView02);

		TextView_Name.setText(attribute.getValue());

		// ////////////

		attribute = (Attr) attrs.getNamedItem("preis" + preiskat);
		TextView TextView_Preis = (TextView) findViewById(R.id.TextView03);

		TextView_Preis.setText(this.getString(R.string.Preiskat)
				+" "+ attribute.getValue() + "€");

		// /////////////////
		TextView TextView1 = (TextView) findViewById(R.id.TextView01);
		attribute = (Attr) attrs.getNamedItem("eng");
		String eng_essen = attribute.getValue();
		if (sprache.equals("de") || eng_essen.length() == 0) {

			Element element = (Element) nodes_temp.item(essen_position);

			TextView1.setText(element.getFirstChild().getNodeValue().trim());
		} else {

			TextView1.setText(eng_essen);
		}
	}
	private synchronized void PushError() {
		TextView TextView04 = (TextView) findViewById(R.id.TextView04);
	
	

			TextView04.setText(getError());
	}


	public void backward(View Button02) {

		backwardnow();
	}
	public void backwardnow() {

		NodeList nodes_temp=getNodes();
		
		if(nodes_temp==null)return;
		essen_position = (essen_position - 1 + nodes_temp.getLength())
				% nodes_temp.getLength();
		refresh();

	}

	public void forward(View Button03) {
 
		forwardnow();
	}
	private void forwardnow() {
		
		NodeList nodes_temp=getNodes();
		
		if(nodes_temp==null)return;
		essen_position = (essen_position + 1) % nodes_temp.getLength();
		refresh();

	}

	private void config() {
		config_update = true;
		Intent it=new Intent(this, Preferences.class);
		
		startActivity(it);

		// A toast is a view containing a quick little message for the user.
		// Toast.makeText(this,
		// "Here you can maintain your user credentials.",
		// Toast.LENGTH_LONG).show();

	}

}