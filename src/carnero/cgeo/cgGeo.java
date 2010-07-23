package carnero.cgeo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class cgGeo {
	private Context context = null;
	private cgeoapplication app = null;
	private LocationManager geoManager = null;
	private cgUpdateLoc geoUpdate = null;
	private cgWarning warning = null;
	private cgBase base = null;
	private cgSettings settings = null;
	private cgeoGeoListener geoNetListener = null;
	private cgeoGeoListener geoGpsListener = null;
    private cgeoGpsStatusListener geoGpsStatusListener = null;
	private Integer time = 0;
	private Integer distance = 0;
	private AlertDialog alertGps = null;
	private Location locGps = null;
	private Location locNet = null;
	private long lastLocated = 0l;
	
	public Location location = null;
	public int gps = -1;
	public Double latitudeNow = null;
	public Double longitudeNow = null;
	public Double altitudeNow = null;
	public Float bearingNow = null;
	public Float speedNow = null;
	public Float accuracyNow = null;
	public Integer satellitesVisible = null;
	public Integer satellitesFixed = null;

	public cgGeo(Context contextIn, cgeoapplication appIn, cgUpdateLoc geoUpdateIn, cgBase baseIn, cgSettings settingsIn, cgWarning warningIn, int timeIn, int distanceIn) {
		context = contextIn;
		app = appIn;
		geoUpdate = geoUpdateIn;
		base = baseIn;
		settings = settingsIn;
		warning = warningIn;
		time = timeIn;
		distance = distanceIn;
		
		geoNetListener = new cgeoGeoListener();
		geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);
		
		geoGpsListener = new cgeoGeoListener();
		geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);

        geoGpsStatusListener = new cgeoGpsStatusListener();
	}

	public void initGeo() {
		location = null;
		gps = -1;
		latitudeNow = null;
		longitudeNow = null;
		altitudeNow = null;
		bearingNow = null;
		speedNow = null;
		accuracyNow = null;
		satellitesVisible = 0;
		satellitesFixed = 0;

		if (geoManager == null) geoManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

		lastLoc();
        
		geoNetListener.setProvider(geoManager.NETWORK_PROVIDER);
		geoGpsListener.setProvider(geoManager.GPS_PROVIDER);
        geoManager.addGpsStatusListener(geoGpsStatusListener);

		geoManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time, distance, geoNetListener);
		geoManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance, geoGpsListener);

		if (alertGps == null && geoManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == false && geoManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setTitle("Location sources");
			dialog.setMessage("You have all localization services switched off. Please, activate network and/or GPS service in Menu / Settings / Location. c:geo needs it.");
			dialog.setCancelable(true);
			dialog.setPositiveButton("change settings", new DialogInterface.OnClickListener() {
			   public void onClick(DialogInterface dialog, int id) {
					((Activity)context).startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					dialog.dismiss();
			   }
			});
			dialog.setNeutralButton("dismiss", new DialogInterface.OnClickListener() {
			   public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
			   }
		   });

		   alertGps = dialog.create();
		   alertGps.show();
		}
	}

	public void closeGeo() {
		if (geoManager != null && geoNetListener != null) {
			geoManager.removeUpdates(geoNetListener);
		}
		if (geoManager != null && geoGpsListener != null) {
			geoManager.removeUpdates(geoGpsListener);
		}
		if (geoManager != null) {
			geoManager.removeGpsStatusListener(geoGpsStatusListener);
		}
	}

	public class cgeoGeoListener implements LocationListener {
		public String active = null;

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// nothing
		}

		@Override
		public void onLocationChanged(Location location) {
			if (location.getProvider().equals(LocationManager.GPS_PROVIDER) == true) locGps = location;
			else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER) == true) locNet = location;

			selectBest();
		}

		@Override
		public void onProviderDisabled(String provider) {
			if (provider.equals(LocationManager.NETWORK_PROVIDER) == true) {
				if (geoManager != null && geoNetListener != null) geoManager.removeUpdates(geoNetListener);
			} else if (provider.equals(LocationManager.GPS_PROVIDER) == true) {
				if (geoManager != null && geoGpsListener != null) geoManager.removeUpdates(geoGpsListener);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
			if (provider.equals(LocationManager.NETWORK_PROVIDER) == true) {
				if (geoNetListener == null) geoNetListener = new cgeoGeoListener();
				geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);
			} else if (provider.equals(LocationManager.GPS_PROVIDER) == true) {
				if (geoGpsListener == null) geoGpsListener = new cgeoGeoListener();
				geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);
			}
		}
		
		public void setProvider(String provider) {
			if (provider.equals(LocationManager.GPS_PROVIDER) == true) {
				if (geoManager != null && geoManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) active = provider;
				else active = null;
			} else if (provider.equals(LocationManager.NETWORK_PROVIDER) == true) {
				if (geoManager != null && geoManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) active = provider;
				else active = null;
			}
		}
	}

    public class cgeoGpsStatusListener implements GpsStatus.Listener {
		@Override
        public void onGpsStatusChanged(int event) {
			if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
				GpsStatus status = geoManager.getGpsStatus(null);
				Iterator<GpsSatellite> statusIterator = status.getSatellites().iterator();

				int satellites = 0;
				int fixed = 0;

				while (statusIterator.hasNext() == true) {
					GpsSatellite sat = statusIterator.next();
					if (sat.usedInFix() == true) fixed ++;
					satellites ++;
				}

				boolean changed = false;
				if (satellitesVisible == null || satellites != satellitesVisible) {
					satellitesVisible = satellites;
					changed = true;
				}
				if (satellitesFixed == null || fixed != satellitesFixed) {
					satellitesFixed = fixed;
					changed = true;
				}

				if (changed == true) selectBest();
			}
        }
    }

	private void selectBest() {
		// use the only available
		if (locNet == null && locGps != null) {
			assign(locGps);
			return;
		}

		if (locNet != null && locGps == null) {
			assign(locNet);
			return;
		}

		// gps is fixed
		if (satellitesFixed > 0) {
			assign(locGps);
			return;
		}

		assign(locNet);
	}

	private void assign(Location loc) {
		if (loc == null) {
			gps = -1;
			return;
		}

		location = loc;

		String provider = location.getProvider();
		if (provider.equals(LocationManager.GPS_PROVIDER) == true) {
			gps = 1;
		} else if (provider.equals(LocationManager.NETWORK_PROVIDER) == true) {
			gps = 0;
		} else if (provider.equals("last") == true) {
			gps = -1;
		}

		latitudeNow = location.getLatitude();
		longitudeNow = location.getLongitude();
		if (location.hasAltitude() && gps != -1) altitudeNow = location.getAltitude();
		else altitudeNow = null;
		if (location.hasBearing() && gps != -1) bearingNow = location.getBearing();
		else bearingNow = 0f;
		if (location.hasSpeed() && gps != -1) speedNow = location.getSpeed();
		else speedNow = 0f;
		if (location.hasAccuracy() && gps != -1) accuracyNow = location.getAccuracy();
		else accuracyNow = 999f;

		geoUpdate.updateLoc(this);

		if (gps > -1) (new publishLoc()).start();
	}

	private class publishLoc extends Thread {
		private publishLoc() {
			setPriority(Thread.MIN_PRIORITY);
		}

		@Override
		public void run() {
			if (settings.publicLoc == 1 && lastLocated < (System.currentTimeMillis() - (5 * 60 * 1000))) {
				final String host = "api.go4cache.com";
				final String path = "/";
				final String method = "POST";
				String action = null;
				if (app != null) action = app.getAction();
				else action = "";

				final String username = settings.getUsername();
				if (username != null) {
					final HashMap<String, String> params = new HashMap<String, String>();
					final String latStr = String.format((Locale)null, "%.6f", latitudeNow);
					final String lonStr = String.format((Locale)null, "%.6f", longitudeNow);
					params.put("u", username);
					params.put("lt", latStr);
					params.put("ln", lonStr);
					params.put("a", action);
					params.put("s", (base.sha1(username + "|" + latStr + "|" + lonStr + "|" + action + "|" + base.md5("carnero: developing your dreams"))).toLowerCase());
					base.request(host, path, method, params, false, false);

					lastLocated = System.currentTimeMillis();
				}
			}
		}
	}

	public void lastLoc() {
		Location lastGps = geoManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location lastGsm = geoManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if (lastGps == null && lastGsm == null) return;
		if (lastGps == null && lastGsm != null) {
			lastGsm.setProvider("last");
			assign(lastGsm);

			Log.i(cgSettings.tag, "Using last location from NETWORK");
			return;
		}
		if (lastGps != null && lastGsm == null) {
			lastGps.setProvider("last");
			assign(lastGps);

			Log.i(cgSettings.tag, "Using last location from GPS");
			return;
		}

		if (lastGps != null && lastGsm != null && lastGps.getTime() < lastGsm.getTime()) {
			lastGps.setProvider("last");
			assign(lastGps);

			Log.i(cgSettings.tag, "Using last location from GPS");
			return;
		} else {
			lastGsm.setProvider("last");
			assign(lastGsm);

			Log.i(cgSettings.tag, "Using last location from GSM");
			return;
		}
	}
}