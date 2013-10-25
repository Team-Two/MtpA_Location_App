package uk.ac.sussex.addison.m.androidmaplocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import uk.ac.sussex.addison.m.androidmaplocation.R;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewDebug.ExportedProperty;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends Activity implements LocationListener {

	// Google Map
	private GoogleMap googleMap;
	private Location location;
	private LocationManager locationManager;
	private Button myButton;
	private MarkerOptions myMarker;
	private String provider;
	private UserMarker currMarker;
	private Calendar currDate = Calendar.getInstance();
	private Resources resourceVals;
	private double lat = 0;
	private double lng = 0;
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		myButton = (Button) findViewById(R.id.radioTempBtn);
		myButton.setOnClickListener(new buttonListener());
		if (initilizeMap()) {
			onLocationChanged(location);			
		} else {
			askToContinue();
		} // try and refresh maps
		locationManager.requestLocationUpdates(provider, 20000, 10, this);
	} // method onCreate
	
	class buttonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (myMarker != null) {
				// create export instance and location for output
				XMLMarkerExport xmlData = new XMLMarkerExport();
				resourceVals = getResources();
				String fileName = resourceVals.getString(R.string.xmlLocFileName) + currMarker.getMarkerDate() + currMarker.getMarkerTime() + ".xml";
				
				// attempt create on XML output
				if (writeToSD(xmlData.writeMarkerXml(currMarker), fileName)) {
			        Toast.makeText(getBaseContext(),"Location saved: " + fileName, Toast.LENGTH_LONG).show();
				} else {
			        Toast.makeText(getBaseContext(),"Error in writing to SD card", Toast.LENGTH_LONG).show();
				} // end if can create xml output
		    } else {
		        Toast.makeText(getBaseContext(),"We haven't got a marker yet", Toast.LENGTH_LONG).show();
			} // end if got a marker
		} // method onClick
		
		public Boolean writeToSD(String vXMLData, String vFileName){
			File root=null; 
			try { 
				// check for SDcard  
				root = Environment.getExternalStorageDirectory(); 

				//check sdcard permission 
				if (root.canWrite()){
					// create file directory
					File fileDir = new File(root.getAbsolutePath() + resourceVals.getString(R.string.xmlDir));
					// check if directory already exists
					if (fileDir.exists() && fileDir.isDirectory()) {
						;
					} else {
						fileDir.mkdirs();
					} // end of if need to create dir
					// chain a buffer to a writer to a file and export the xml data
					File file= new File(fileDir, vFileName);
					FileWriter filewriter = new FileWriter(file); 
					BufferedWriter outBuffer = new BufferedWriter(filewriter); 
					outBuffer.write(vXMLData); 
					outBuffer.close(); 
					return true;
				} // end of if can write to sd card
			} catch (IOException e) { 
				return false;
			} // end of try-catch
			return true;
		} // method writeToSD
	    
	} // class buttonListener

	/**
	 * function to load map. If map is not created it will create it for you
	 * */
	private boolean initilizeMap() {
		if (googleMap == null) {
			// put the map on the screen
			googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

	        // check if map is created successfully or not
			if (googleMap == null) {
				return false;
			} else {
		        // Enabling MyLocation Layer of Google Map
		        googleMap.setMyLocationEnabled(true);

		        // Getting LocationManager object from System Service LOCATION_SERVICE
		        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		        if (locationManager == null) {
					return false;
				} // if can't get a locationmanager

		        // Creating a criteria object to retrieve provider
		        Criteria criteria = new Criteria();
		        if (criteria == null) {
					return false;
				}

		        // Getting the name of the best provider
		        provider = locationManager.getBestProvider(criteria, true);
		        if (provider == "") {
					return false;
				}

		        // Getting Current Location
		        location = locationManager.getLastKnownLocation(provider);
		        if (location == null) {
					return false;
				}
				return true;
			} // if - can display map
		} // if - can find map
		else {
			return false;
		}
	} // method - initializeMap
	
	private void askToContinue() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		// set title
		alertDialogBuilder.setTitle("Continue to look for location ?");

		// set dialog message
		alertDialogBuilder.setMessage("Yes - continue to look for GPS : No - Close app");
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				// if this button is clicked, just close
				// the dialog box and reset loopVal
				dialog.cancel();
			}
		});
		alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				// if this button is clicked, close
				// current activity
				MainActivity.this.finish();
			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	} // method askToContinue

	@Override
	protected void onResume() {
		super.onResume();
		if (initilizeMap()) {
	        Toast.makeText(getBaseContext(),"Resume location : " + Double.toString(lat).substring(0, 5) + "," + Double.toString(lng).substring(0, 5), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getApplicationContext(),"Waiting on exact location", Toast.LENGTH_SHORT).show();
		} // try and refresh maps
	} // method onResume

	@Override
	public void onLocationChanged(Location location) {
		// get current location
		lat = (double) (location.getLatitude());
		lng = (double) (location.getLongitude());
		LatLng currPos = new LatLng(lat, lng);

		// set camera zoom level
		float cameraZoom = (googleMap.getMinZoomLevel() + googleMap.getMaxZoomLevel())/2;
		
		// create and display marker
		myMarker = new MarkerOptions();
		myMarker.position(currPos);
		myMarker.snippet("Lat:" + location.getLatitude() + "Lng:"+ location.getLongitude());
		myMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title("ME");
		googleMap.addMarker(myMarker);
		googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currPos, cameraZoom));
		
		// invoke instance of the current marker
		currMarker = new UserMarker.Builder(1234, lat, lng, currDate).build();
        Toast.makeText(getBaseContext(),"Current location : " + Double.toString(lat).substring(0, 5) + "," + Double.toString(lng).substring(0, 5), Toast.LENGTH_LONG).show();
	} // onLocationChanged
  
	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(this, "Enabled new provider " + provider,
				Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, "Disabled provider " + provider,
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
} // class MainActivity
