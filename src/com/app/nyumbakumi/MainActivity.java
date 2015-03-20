/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.nyumbakumi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.app.nyumbakumi.entity.MProfile;
import com.app.nyumbakumi.framework.Act;
import com.app.nyumbakumi.util.CommonUtils;
import com.app.nyumbakumi.util.MUserService;
import com.app.nyumbakumi.utils.Rounder;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends Act {
	public DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;	
	private DrawerAdapterRight adapterRight;
	private View relativeDrawerLayoutRight;
	private DrawerAdapter adapter;
	private View relativeDrawerLayout;	
	private ListView mDrawerListRight;

	public ArrayList<DrawerTitle> drawerTitlesRight = new ArrayList<DrawerTitle>();
	public ArrayList<DrawerTitle> drawerTitlesLeft = new ArrayList<MainActivity.DrawerTitle>();
	
	private boolean cancelDefault = true; 
	private boolean isNotification;
	
	private LocationManager locationManager;
	private Location lastKnownLocation;
	private AsyncTask<Void, Void, Void> mAlertp;
	private Criteria criteria;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			
			if (bundle != null) {	    	  
				int resultCode = bundle.getInt(MUserService.RESULT);
				
				if (resultCode == Activity.RESULT_OK) {
					String name = bundle.getString("NAME"),
							phone = bundle.getString("PHONE"),
							type = bundle.getString("USER_TYPE"),
					group = bundle.getString("GROUP"),
					hse_estate = bundle.getString("HSE_ESTATE"), 
							hse_no = bundle.getString("HSE_NO");
					setUserDetails(name, group, phone, type, hse_estate, hse_no);
					
					Log.i(TAG, name+", "+phone+", "+type+", "+group+", "+hse_estate+", "+hse_no);
					setDrawerDetails();
					
				}
			}
		}
	};

	public void setUserDetails(String...params) {
		SharedPreferences settings = getSharedPreferences(Act.PREFS_NAME, 0); 
		SharedPreferences.Editor editor = settings.edit(); 
		// User Details
		editor.putString("name", params[0]); 
		editor.putString("temp_group", params[1]); 
		editor.putString("sign_phone", params[2]); 		
		editor.putString("USER_ROLE", params[3]); 		
		if(params[4] != null && !params[4].equals("")) editor.putString("hse_estate", params[4]); 
		if(params[5] != null && !params[5].equals("")) editor.putString("hse_no", params[5]); 
		editor.commit(); 
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		
		try {
			isNotification = getIntent().getBooleanExtra("NOTIFICATION", false);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		createDrawerArray();		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		adapter = new DrawerAdapter();
		relativeDrawerLayout = findViewById(R.id.linearDrawer);

		mDrawerListRight = (ListView) findViewById(R.id.left_drawerRight);
		adapterRight = new DrawerAdapterRight();
		relativeDrawerLayoutRight = findViewById(R.id.linearDrawerRight);
		
		// set a custom shadow that overlays the main content when the drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener
		mDrawerList.setAdapter(adapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// set a custom shadow that overlays the main content when the drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.END);
		// set up the drawer's list view with items and click listener
		mDrawerListRight.setAdapter(adapterRight);
		mDrawerListRight.setOnItemClickListener(new DrawerItemClickListenerRight());
		
		setDrawerDetails();
		
		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description for accessibility */
				R.string.drawer_close  /* "close drawer" description for accessibility */
				) {
			public void onDrawerClosed(View view) {
				//getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				//getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);
		//mDrawerLayoutRight.setDrawerListener(mDrawerToggleRight);

		if (savedInstanceState == null) {
			selectItem(0);
		}
		
		setFontRegular((TextView) findViewById(R.id.textDrawerPhone));		
		setFontRegular((TextView) findViewById(R.id.textDrawerProfileName)); 	

		View v = findViewById(R.id.imageDrawerMenu);
		v.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDrawerLayout.closeDrawer(relativeDrawerLayout);				
			}
		});
		
		View vx = findViewById(R.id.imageDrawerMenuRight);
		vx.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDrawerLayout.closeDrawer(relativeDrawerLayoutRight);				
			}
		});
		/**
		 * Open the left Drawer by defaule
		 */
		mDrawerLayout.openDrawer(relativeDrawerLayout);
	}
	
	/**
	 * Returns the current location of the user
	 * @return LatLng
	 */
	private LatLng getLocation() {
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		lastKnownLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));
		
		if(lastKnownLocation != null){
			double latitude = lastKnownLocation.getLatitude();
			double longitude = lastKnownLocation.getLongitude();
			
			return new LatLng(latitude, longitude);
		}else return null;
	}
	
	/**
	 * Set Custom users details
	 */
	private void setDrawerDetails() {
		/**
		 * Set 	Name and Phone Number
		 */
		TextView textName = (TextView)findViewById(R.id.textDrawerProfileName);
		if(!getNameValue().equals(""))
			textName.setText(getNameValue());
		
		TextView textPhone = (TextView)findViewById(R.id.textDrawerPhone);
		if(!getPhoneValue().equals("")){
			/**
			 * Check the user type here...
			 */
			if(getUserRole().equals("Admin")) 
				textPhone.setText(getPhoneValue()+" | Group Admin");
			else textPhone.setText(getPhoneValue());
		}
		
		/**
		 * Change the profile Icon
		 */
		 ImageView profile = (ImageView)findViewById(R.id.imagePhoto);
		 if(getProfilePhoto() != null && !getProfilePhoto().equals("")) {
			 Log.i(TAG, getProfilePhoto());
			 try {
			 Bitmap bitmap = Rounder.getRoundedShape(getProfilePhoto(), 160, 160);
			 profile.setImageBitmap(bitmap);
			 }catch(Exception ex) {
				 ex.printStackTrace();
				 Bitmap bitmap = Rounder.getRoundedShape(R.drawable.avatar, this, 160, 160);
				 profile.setImageBitmap(bitmap);
			 }
		 }else {
			 Bitmap bitmap = Rounder.getRoundedShape(R.drawable.avatar, this, 160, 160);
			 profile.setImageBitmap(bitmap);
		 }
	}

	private void createDrawerArray() {
		drawerTitlesLeft.clear();
		
		drawerTitlesLeft.add(new DrawerTitle("Compose", false, R.drawable.icon_drawer_compose));
		drawerTitlesLeft.add(new DrawerTitle("Notifications", "5", false, R.drawable.icon_drawer_notification));
		drawerTitlesLeft.add(new DrawerTitle("Members", false, R.drawable.icon_drawer_member));
		drawerTitlesLeft.add(new DrawerTitle("Calendar", false, R.drawable.icon_drawer_calendar));
		drawerTitlesLeft.add(new DrawerTitle("Help", false, R.drawable.icon_drawer_help));
		drawerTitlesLeft.add(new DrawerTitle("Settings", false, R.drawable.icon_drawer_settings));
		
		drawerTitlesRight.clear();
		
		drawerTitlesRight.add(new DrawerTitle("Security", true, R.drawable.icon_panic_security));
		drawerTitlesRight.add(new DrawerTitle("Ambulance", true, R.drawable.icon_panic_ambulance));
		drawerTitlesRight.add(new DrawerTitle("Fire Station", true, R.drawable.icon_panic_fire_station));
		drawerTitlesRight.add(new DrawerTitle("Report An Incident", true, R.drawable.icon_panic_report));
		drawerTitlesRight.add(new DrawerTitle("Help", true, R.drawable.icon_drawer_help));
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(relativeDrawerLayout);
		if(drawerOpen) mDrawerLayout.closeDrawer(relativeDrawerLayout);
		else {
			mDrawerLayout.openDrawer(relativeDrawerLayout);
			mDrawerLayout.closeDrawer(relativeDrawerLayoutRight);
		}
		return super.onCreateOptionsMenu(menu);
	}	

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(relativeDrawerLayout);
		if(!drawerOpen) {
			mDrawerLayout.openDrawer(relativeDrawerLayout);
			mDrawerLayout.closeDrawer(relativeDrawerLayoutRight);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}        
		return super.onOptionsItemSelected(item);
	}

	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {
		// update the main content by replacing fragments
		if(isNotification) {
			String message = getIntent().getStringExtra("MESSAGE");

	        Log.i(TAG, getIntent().getStringExtra("SENDER")+", "+message);
			switchScreen(TimeLine.getInstance(message, getIntent().getStringExtra("SENDER")));
		}else switchScreen(new TimeLine());
		
		switch (position) {
		case 0:
			if (cancelDefault) {
				cancelDefault = false;
			}else {
				ComposeScreen dialog = new ComposeScreen();
				dialog.show(getSupportFragmentManager(), "ComposeScreen");
			}
			break;
		case 1:
			/*
			 * Notification. 
			 */
			switchScreen(new TimeLine());	
			break;
		case 2:
			/*
			 * Members List
			 */
			switchScreen(new MembersList());
			break;

		case 3:
			/*
			 * Calendar
			 */
			switchScreen(new CalendarScreen());
			break;
			
		case 4:
			//switchScreen(new CalendarScreen());
			break;

		default:
			switchScreen(new TimeLine());
			break;
		}

		// update selected item and title, then close the drawer
		mDrawerList.setItemChecked(position, true);
		setTitle(drawerTitlesLeft.get(position).getTitle());
		
		try {
			mDrawerLayout.closeDrawer(relativeDrawerLayout);
		} catch(Exception ex){ex.printStackTrace(); }
	}

	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListenerRight implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItemRight(position);
		}
	}

	private void selectItemRight(int position) {
		// update the main content by replacing fragments
		LatLng latlng = getLocation();
		switch (position) {
		case 0:
			/**
			 * Security {String user_id, String category, double latitude, double longitude}
			 */
			if(latlng != null) sendPanicAlert("Security", latlng);
			break;
		case 1:
			/**
			 * Ambulance
			 */
			if(latlng != null) sendPanicAlert("Ambulance", latlng);
			break;
		case 2:
			/**
			 * Fire Station
			 */
			if(latlng != null) sendPanicAlert("Fire Station", latlng);
			break;
		case 3:
			/**
			 * Report An Incident
			 */
			//if(latlng != null) sendPanicAlert("report an incident", latlng);
			break;

		default:
			switchScreen(new ProfileScreen());
			break;
		}

		// update selected item and title, then close the drawer
		mDrawerListRight.setItemChecked(position, true);
		setTitle(drawerTitlesLeft.get(position).getTitle());

		try {
			mDrawerLayout.closeDrawer(relativeDrawerLayoutRight);
		} catch(Exception ex){ex.printStackTrace(); }
	}
	
	/**
	 * An async task to send panic alerts to the service providers
	 * @param category Category of the alert / service
	 * @param latlng The location where the user is. 
	 */
	private void sendPanicAlert(final String category, final LatLng latlng) {
		mAlertp = new AsyncTask<Void, Void, Void>(){
			private boolean isSend = false;
			
			@Override
			protected Void doInBackground(Void... params) {
				try {
					String user_id = getTemporaryUserID();
					isSend = CommonUtils.sendPanic(user_id, category, latlng.latitude, latlng.longitude);
				} catch (Exception e) {
					Log.e(TAG, "Impossible to send panic alert", e);
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if(isSend) toast("Alert has been send successfully, you will be contacted shortly!");
				else toast("Sorry, could not send your alert, please try again!");
				
				mAlertp = null;
				super.onPostExecute(result);
			}

		};

		mAlertp.execute();
	}
	
	@Override
	public void setTitle(CharSequence title) {
		//mTitle = title;
		//try { getActionBar().setTitle(mTitle); }catch(Exception ex){ex.printStackTrace(); }
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	class DrawerTitle {
		private String title, notification;
		private boolean isNotification;
		private int ImageResource;

		/**
		 * Set the details for the Drawer Title
		 * @param title the title
		 * @param isNotification If this is a notification
		 * @param resource The image resource to be used. 
		 */
		public DrawerTitle(String title, boolean isNotification, int resource) {
			this.setTitle(title);
			this.setNotification(isNotification);
			this.setImageResource(resource);
			this.setNotification("");
		}

		/**
		 * Set the details for the Drawer Title
		 * @param title the title
		 * @param notification the notification
		 * @param isNotification If this is a notification
		 * @param resource The image resource to be used. 
		 */
		public DrawerTitle(String title, String notification, boolean isNotification, int resource) {
			this.setTitle(title);
			this.setNotification(isNotification);
			this.setImageResource(resource);
			this.setNotification(notification);
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getNotification() {
			return notification;
		}

		public void setNotification(String notification) {
			this.notification = notification;
		}

		public boolean isNotification() {
			return isNotification;
		}

		public void setNotification(boolean isNotification) {
			this.isNotification = isNotification;
		}

		public int getImageResource() {
			return ImageResource;
		}

		public void setImageResource(int imageResource) {
			ImageResource = imageResource;
		}
	}

	class DrawerAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return drawerTitlesLeft .size();
		}

		@Override
		public DrawerTitle getItem(int position) {
			return drawerTitlesLeft.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
		}

		@Override
		public View getView(final int position, View v, ViewGroup parent) {
			// Get company item from the arraylist at this position
			DrawerTitle drawerItem = getItem(position);

			// Right
			v = getLayoutInflater().inflate(R.layout.drawer_list_item,  parent, false);				

			// Set the content to be shown, fetched from the company instance acquired above.
			if(drawerItem != null) {
				TextView _title = (TextView) v.findViewById(R.id.drawerTitle);
				TextView _notification = (TextView) v.findViewById(R.id.drawerNotification);
				ImageView _image = (ImageView) v.findViewById(R.id.drawerImage);
				_image.setImageResource(drawerItem.getImageResource());

				_title.setText(drawerItem.getTitle());
				_notification.setText(drawerItem.getNotification());

				// The fonts
				setFontRegular(_title);
				setFontSemiBold(_notification);
				
				if(!drawerItem.isNotification()) _notification.setVisibility(View.GONE);
				else _notification.setVisibility(View.VISIBLE);				
				return v;
			}else return null;
		}

	}

	class DrawerAdapterRight extends BaseAdapter {

		@Override
		public int getCount() {
			return drawerTitlesRight.size();
		}

		@Override
		public DrawerTitle getItem(int position) {
			return drawerTitlesRight.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
		}

		@Override
		public View getView(final int position, View v, ViewGroup parent) {
			// Get company item from the arraylist at this position
			DrawerTitle drawerItem = getItem(position);

			// Right
			v = getLayoutInflater().inflate(R.layout.drawer_list_item,  parent, false);				

			// Set the content to be shown, fetched from the company instance acquired above.
			if(drawerItem != null) {
				TextView _title = (TextView) v.findViewById(R.id.drawerTitle);
				TextView _notification = (TextView) v.findViewById(R.id.drawerNotification);

				ImageView _image = (ImageView) v.findViewById(R.id.drawerImage);
				_image.setImageResource(drawerItem.getImageResource());
				
				_title.setText(drawerItem.getTitle());
				_notification.setText(drawerItem.getNotification());

				// The fonts
				setFontRegular(_title);
				setFontSemiBold(_notification);

				if(!drawerItem.isNotification()) _image.setVisibility(View.GONE);
				else _image.setVisibility(View.VISIBLE);

				_notification.setVisibility(View.GONE);			
				return v;
			}else return null;
		}
	}

	@Override
	public void openDrawerLeft() {
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(relativeDrawerLayout);
		if(drawerOpen) mDrawerLayout.closeDrawer(relativeDrawerLayout);
		else {
			mDrawerLayout.openDrawer(relativeDrawerLayout);
			mDrawerLayout.closeDrawer(relativeDrawerLayoutRight);
		}
	}	

	public boolean isLeftDrawerOpen() {
		return mDrawerLayout.isDrawerOpen(relativeDrawerLayout);
	}
	
	public boolean isRightDrawerOpen() {
		return mDrawerLayout.isDrawerOpen(relativeDrawerLayoutRight);
	}
	
	public void closeLeftDrawer() {
		mDrawerLayout.closeDrawer(relativeDrawerLayout);
	}
	
	public void closeRightDrawer() {
		 mDrawerLayout.closeDrawer(relativeDrawerLayoutRight);
	}
	
	@Override
	public void openDrawerRight() {
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(relativeDrawerLayoutRight);
		if(drawerOpen) mDrawerLayout.closeDrawer(relativeDrawerLayoutRight);
		else {
			mDrawerLayout.openDrawer(relativeDrawerLayoutRight);
			mDrawerLayout.closeDrawer(relativeDrawerLayout);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(receiver, new IntentFilter(MUserService.NOTIFICATION));
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}
}