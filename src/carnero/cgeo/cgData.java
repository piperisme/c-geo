package carnero.cgeo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/*
 * database history:
 * 000-033: basic structure, tables
 * 034-036: added indexes
 */

public class cgData {
	public cgCacheWrap caches;
    private Context context = null;

    private cgDbHelper dbHelper = null;
    private SQLiteDatabase databaseRO = null;
    private SQLiteDatabase databaseRW = null;

    private static final String dbName = "data";
    private static final String dbTableCaches = "cg_caches";
    private static final String dbTableAttributes = "cg_attributes";
    private static final String dbTableWaypoints = "cg_waypoints";
    private static final String dbTableSpoilers = "cg_spoilers";
    private static final String dbTableLogs = "cg_logs";
    private static final String dbTableTrackables = "cg_trackables";
    private static final int dbVersion = 36;
    private static final String dbCreateCaches = ""
			+ "create table " + dbTableCaches + " ("
			+ "_id integer primary key autoincrement, "
            + "updated long not null, "
			+ "detailed integer not null default 0, "
            + "detailedupdate long, "
			+ "geocode text unique not null, "
			+ "reason integer not null default 0, " // cached, favourite...
			+ "cacheid text, "
			+ "guid text, "
			+ "type text, "
			+ "name text, "
			+ "owner text, "
			+ "hidden long, "
			+ "hint text, "
			+ "size text, "
			+ "difficulty float, "
			+ "terrain float, "
			+ "latlon text, "
			+ "latitude_string text, "
			+ "longitude_string text, "
			+ "location text, "
			+ "latitude double, "
			+ "longitude double, "
			+ "shortdesc text, "
			+ "description text, "
			+ "disabled integer not null default 0, "
			+ "archived integer not null default 0, "
			+ "members integer not null default 0, "
			+ "found integer not null default 0, "
			+ "favourite integer not null default 0, "
            + "inventorycoins integer default 0, "
            + "inventorytags integer default 0, "
            + "inventoryunknown integer default 0 "
			+ "); ";

    private static final String dbCreateAttributes = ""
			+ "create table " + dbTableAttributes + " ("
			+ "_id integer primary key autoincrement, "
			+ "geocode text not null, "
			+ "updated long not null, " // date of save
			+ "attribute text "
			+ "); ";

    private static final String dbCreateWaypoints = ""
			+ "create table " + dbTableWaypoints + " ("
			+ "_id integer primary key autoincrement, "
			+ "geocode text not null, "
			+ "updated long not null, " // date of save
			+ "type text not null default 'waypoint', "
			+ "prefix text, "
			+ "lookup text, "
			+ "name text, "
			+ "latlon text, "
			+ "latitude_string text, "
			+ "longitude_string text, "
			+ "latitude double, "
			+ "longitude double, "
			+ "note text "
			+ "); ";

    private static final String dbCreateSpoilers = ""
			+ "create table " + dbTableSpoilers + " ("
			+ "_id integer primary key autoincrement, "
			+ "geocode text not null, "
			+ "updated long not null, " // date of save
			+ "url text, "
			+ "title text, "
			+ "description text "
			+ "); ";

    private static final String dbCreateLogs = ""
			+ "create table " + dbTableLogs + " ("
			+ "_id integer primary key autoincrement, "
			+ "geocode text not null, "
			+ "updated long not null, " // date of save
			+ "type text not null default 'Note', "
			+ "author text, "
			+ "log text, "
			+ "date long, "
			+ "found integer not null default 0 "
			+ "); ";

    private static final String dbCreateTrackables = ""
			+ "create table " + dbTableTrackables + " ("
			+ "_id integer primary key autoincrement, "
			+ "geocode text not null, "
			+ "updated long not null, " // date of save
			+ "tbcode text not null, "
			+ "guid text, "
			+ "title text, "
			+ "owner text, "
			+ "released long, "
			+ "goal text, "
			+ "description text "
			+ "); ";
	private static SQLiteStatement sqlDetailedGc = null;
	private static SQLiteStatement sqlDetailedGuid = null;
	private static SQLiteStatement sqlCount = null;
	private static SQLiteStatement sqlCountDetailed = null;
	private static SQLiteStatement sqlCountTyped = null;
	private static SQLiteStatement sqlCountDetailedTyped = null;

	public cgData(Context contextIn) {
		context = contextIn;

		if (dbHelper == null) dbHelper = new cgDbHelper(context);

		if (databaseRO == null || databaseRO.isOpen() == false) {
			try {
				if (dbHelper == null) dbHelper = new cgDbHelper(context);
				databaseRO = dbHelper.getReadableDatabase();

				if (databaseRO != null && databaseRO.isOpen()) {
					Log.i(cgSettings.tag, "Connection to RO database established.");
				} else {
					Log.e(cgSettings.tag, "Failed to open connection to RO database.");
				}

				if (databaseRO.inTransaction() == true) databaseRO.endTransaction();
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgData.openDb.RO: " + e.toString());
			}
		}

		if (databaseRW == null || databaseRW.isOpen() == false) {
			try {
				if (dbHelper == null) dbHelper = new cgDbHelper(context);
				databaseRW = dbHelper.getWritableDatabase();

				if (databaseRW != null && databaseRW.isOpen()) {
					Log.i(cgSettings.tag, "Connection to RW database established.");
				} else {
					Log.e(cgSettings.tag, "Failed to open connection to RW database.");
				}

				if (databaseRW.inTransaction() == true) databaseRW.endTransaction();
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgData.openDb.RWs: " + e.toString());
			}
		}
	}

    public void closeDb() {
		if (databaseRO != null) {
			if (databaseRO.inTransaction() == true) databaseRO.endTransaction();
			
			databaseRO.close();
            databaseRO.releaseMemory();

			Log.d(cgSettings.tag, "Closing RO database");
		}
		
		if (databaseRW != null) {
			if (databaseRW.inTransaction() == true) databaseRW.endTransaction();

			databaseRW.close();
            databaseRW.releaseMemory();

			Log.d(cgSettings.tag, "Closing RW database");
		}

		if (dbHelper != null) dbHelper.close();
    }

    private static class cgDbHelper extends SQLiteOpenHelper {
        cgDbHelper(Context context) {
            super(context, dbName, null, dbVersion);
        }
		
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(dbCreateCaches);
            db.execSQL(dbCreateAttributes);
            db.execSQL(dbCreateWaypoints);
            db.execSQL(dbCreateSpoilers);
            db.execSQL(dbCreateLogs);
            db.execSQL(dbCreateTrackables);

			db.execSQL("create index if not exists in_a on " + dbTableCaches + " (geocode)");
			db.execSQL("create index if not exists in_b on " + dbTableCaches + " (guid)");
			db.execSQL("create index if not exists in_c on " + dbTableCaches + " (reason)");
			db.execSQL("create index if not exists in_d on " + dbTableCaches + " (detailed)");
			db.execSQL("create index if not exists in_e on " + dbTableCaches + " (type)");
			db.execSQL("create index if not exists in_a on " + dbTableAttributes + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableWaypoints + " (geocode)");
			db.execSQL("create index if not exists in_b on " + dbTableWaypoints + " (geocode, type)");
			db.execSQL("create index if not exists in_a on " + dbTableSpoilers + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableLogs + " (geocode)");
			db.execSQL("create index if not exists in_a on " + dbTableTrackables + " (geocode)");

			Log.i(cgSettings.tag, "Just created new database.");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.beginTransaction();

			try {
				if (oldVersion < 34) {
					db.execSQL("drop table if exists " + dbTableCaches);
					db.execSQL("drop table if exists " + dbTableAttributes);
					db.execSQL("drop table if exists " + dbTableWaypoints);
					db.execSQL("drop table if exists " + dbTableSpoilers);
					db.execSQL("drop table if exists " + dbTableLogs);
					db.execSQL("drop table if exists " + dbTableTrackables);
					onCreate(db);

					Log.i(cgSettings.tag, "Just upgraded database from ver. " + oldVersion +" to ver. " + newVersion + ".");
				} else {
					db.execSQL("create index if not exists in_a on " + dbTableCaches + " (geocode)");
					db.execSQL("create index if not exists in_b on " + dbTableCaches + " (guid)");
					db.execSQL("create index if not exists in_c on " + dbTableCaches + " (reason)");
					db.execSQL("create index if not exists in_d on " + dbTableCaches + " (detailed)");
					db.execSQL("create index if not exists in_e on " + dbTableCaches + " (type)");
					db.execSQL("create index if not exists in_a on " + dbTableAttributes + " (geocode)");
					db.execSQL("create index if not exists in_a on " + dbTableWaypoints + " (geocode)");
					db.execSQL("create index if not exists in_b on " + dbTableWaypoints + " (geocode, type)");
					db.execSQL("create index if not exists in_a on " + dbTableSpoilers + " (geocode)");
					db.execSQL("create index if not exists in_a on " + dbTableLogs + " (geocode)");
					db.execSQL("create index if not exists in_a on " + dbTableTrackables + " (geocode)");

					Log.i(cgSettings.tag, "Just created indexes.");
				}

				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
        }
	}

    public boolean isThere(String geocode, String guid, boolean detailed, boolean checkTime) {
		Cursor cursor = null;

        int cnt = 0;
        long dataUpdated = 0;
        long dataDetailedUpdate = 0;
        int dataDetailed = 0;

		try {
            if (geocode != null && geocode.length() > 0) {
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] {"_id", "detailed", "detailedupdate", "updated"},
                        "geocode = \"" + geocode + "\"",
                        null,
                        null,
                        null,
                        null,
                        "1"
                        );
            } else if (guid != null && guid.length() > 0) {
                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] {"_id", "detailed", "detailedupdate", "updated"},
                        "guid = \"" + guid + "\"",
                        null,
                        null,
                        null,
                        null,
                        "1"
                        );
            } else {
                return false;
            }

			if (cursor != null) {
                int index = 0;
				cnt = cursor.getCount();

                if (cnt > 0) {
                    cursor.moveToFirst();
                    
                    index = cursor.getColumnIndex("updated");
                    dataUpdated = (long)cursor.getLong(index);
                    index = cursor.getColumnIndex("detailedupdate");
                    dataDetailedUpdate = (long)cursor.getLong(index);
                    index = cursor.getColumnIndex("detailed");
                    dataDetailed = (int)cursor.getInt(index);
                }
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.isThere: " + e.toString());
		}

		if (cursor != null) {
			cursor.close();
		}

        if (cnt > 0) {
            if (detailed == true && dataDetailed == 0) {
                // is there, but not with details
                return false;
            }

            if (checkTime == true) {
                if (detailed == false && dataUpdated < (System.currentTimeMillis() - (30 * 60 * 1000))) {
                    // cache basic: older than 30 mins
                    return false;
                }
                if (detailed == true && dataDetailedUpdate < (System.currentTimeMillis() - (30 * 60 * 1000))) {
                    // cache details: older than 30 mins
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public boolean isOffline(String geocode, String guid) {
        long reason = 0;

		try {
			if (sqlDetailedGc == null) sqlDetailedGc = databaseRO.compileStatement("select reason from " + dbTableCaches + "  where geocode = ? limit 1");
			if (sqlDetailedGuid == null) sqlDetailedGuid = databaseRO.compileStatement("select reason from " + dbTableCaches + " where guid = ? limit 1");

            if (geocode != null && geocode.length() > 0) {
				sqlDetailedGc.bindString(1, geocode);
                reason = sqlDetailedGc.simpleQueryForLong();
            } else if (guid != null && guid.length() > 0) {
				sqlDetailedGuid.bindString(1, guid);
                reason = sqlDetailedGuid.simpleQueryForLong();
            } else {
                return false;
            }
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.isOffline: " + e.toString());
		}

		if (reason == 1l) {
			return true;
		} else {
			return false;
		}
    }

    public String getGeocodeForGuid(String guid) {
        if (guid == null || guid.length() == 0) return null;

		Cursor cursor = null;
        String geocode = null;

		try {
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] {"_id", "geocode"},
                    "guid = \"" + guid + "\"",
                    null,
                    null,
                    null,
                    null,
                    "1"
                    );

			if (cursor != null) {
                int index = 0;

                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    index = cursor.getColumnIndex("geocode");
                    geocode = (String)cursor.getString(index);
                }
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getGeocodeForGuid: " + e.toString());
		}

		if (cursor != null) {
			cursor.close();
		}

        return geocode;
    }

    public String getCacheidForGeocode(String geocode) {
        if (geocode == null || geocode.length() == 0) return null;

		Cursor cursor = null;
        String cacheid = null;

		try {
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] {"_id", "cacheid"},
                    "geocode = \"" + geocode + "\"",
                    null,
                    null,
                    null,
                    null,
                    "1"
                    );

			if (cursor != null) {
                int index = 0;

                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    index = cursor.getColumnIndex("cacheid");
                    cacheid = (String)cursor.getString(index);
                }
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.getCacheidForGeocode: " + e.toString());
		}

		if (cursor != null) {
			cursor.close();
		}

        return cacheid;
    }

    public boolean saveCache(cgCache cache) {
        if (cache == null) return false;

        ContentValues values = new ContentValues();

        if (cache.updated == null) {
            values.put("updated", System.currentTimeMillis());
        } else {
            values.put("updated", cache.updated);
        }
        values.put("reason", cache.reason);
        if (cache.detailed == true) {
            values.put("detailed", 1);
        } else {
            values.put("detailed", 0);
        }
        values.put("detailedupdate", cache.detailedUpdate);
        values.put("geocode", cache.geocode);
        values.put("cacheid", cache.cacheid);
        values.put("guid", cache.guid);
        values.put("type", cache.type);
        values.put("name", cache.name);
        values.put("owner", cache.owner);
        if (cache.hidden == null) {
            values.put("hidden", 0);
        } else {
            values.put("hidden", cache.hidden.getTime());
        }
        values.put("hint", cache.hint);
        values.put("size", cache.size);
        values.put("difficulty", cache.difficulty);
        values.put("terrain", cache.terrain);
        values.put("latlon", cache.latlon);
        values.put("latitude_string", cache.latitudeString);
        values.put("longitude_string", cache.longitudeString);
        values.put("location", cache.location);
        values.put("latitude", cache.latitude);
        values.put("longitude", cache.longitude);
        values.put("shortdesc", cache.shortdesc);
        values.put("description", cache.description);
        if (cache.disabled == true) values.put("disabled", 1);
        else values.put("disabled", 0);
        if (cache.archived == true) values.put("archived", 1);
		else values.put("archived", 0);
        if (cache.members == true) values.put("members", 1);
        else values.put("members", 0);
        if (cache.found == true) values.put("found", 1);
        else values.put("found", 0);
        if (cache.favourite == true) values.put("favourite", 1);
		else values.put("favourite", 0);
        values.put("inventorycoins", cache.inventoryCoins);
        values.put("inventorytags", cache.inventoryTags);
        values.put("inventoryunknown", cache.inventoryUnknown);

        saveAttributes(cache.geocode, cache.attributes);
        saveWaypoints(cache.geocode, cache.waypoints, true);
        saveSpoilers(cache.geocode, cache.spoilers);
        saveLogs(cache.geocode, cache.logs);
        saveInventory(cache.geocode, cache.inventory);

        if (isThere(cache.geocode, cache.guid, false, false) == true) {
            int rows = databaseRW.update(dbTableCaches, values, "geocode = \"" + cache.geocode + "\"", null);
			values = null;

            if (rows > 0) return true;
        } else {
            long id = databaseRW.insert(dbTableCaches, null, values);
			values = null;

            if (id > 0) return true;
        }

		values = null;
		
        return false;
    }

    public boolean saveAttributes(String geocode, ArrayList<String> attributes) {
        if (geocode == null || geocode.length() == 0 || attributes == null || attributes.isEmpty()) return false;

		databaseRW.beginTransaction();
		try {
			databaseRW.delete(dbTableAttributes, "geocode = \"" + geocode + "\"", null);

			ContentValues values = new ContentValues();
			for (String oneAttribute : attributes) {
				values.clear();
				values.put("geocode", geocode);
				values.put("updated", System.currentTimeMillis());
				values.put("attribute", oneAttribute);

				databaseRW.insert(dbTableAttributes, null, values);
			}
			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}

        return true;
    }

    public boolean saveWaypoints(String geocode, ArrayList<cgWaypoint> waypoints, boolean drop) {
        if (geocode == null || geocode.length() == 0 || waypoints == null || waypoints.isEmpty()) return false;

		boolean ok = false;
		databaseRW.beginTransaction();
		try {
			if (drop == true) databaseRW.delete(dbTableWaypoints, "geocode = \"" + geocode + "\" and type <> \"own\"", null);

			ContentValues values = new ContentValues();
			for (cgWaypoint oneWaypoint : waypoints) {
				if (oneWaypoint.type.equalsIgnoreCase("own") == true) continue;

				values.clear();
				values.put("geocode", geocode);
				values.put("updated", System.currentTimeMillis());
				values.put("type", oneWaypoint.type);
				values.put("prefix", oneWaypoint.prefix);
				values.put("lookup", oneWaypoint.lookup);
				values.put("name", oneWaypoint.name);
				values.put("latlon", oneWaypoint.latlon);
				values.put("latitude_string", oneWaypoint.latitudeString);
				values.put("longitude_string", oneWaypoint.longitudeString);
				values.put("latitude", oneWaypoint.latitude);
				values.put("longitude", oneWaypoint.longitude);
				values.put("note", oneWaypoint.note);

				databaseRW.insert(dbTableWaypoints, null, values);
			}
			databaseRW.setTransactionSuccessful();
			ok = true;
		} finally {
			databaseRW.endTransaction();
		}

		return ok;
    }

    public boolean saveOwnWaypoint(int id, String geocode, cgWaypoint waypoint) {
        if (((geocode == null || geocode.length() == 0) && id <= 0) || waypoint == null) return false;

		boolean ok = false;
		databaseRW.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("geocode", geocode);
			values.put("updated", System.currentTimeMillis());
			values.put("type", waypoint.type);
			values.put("prefix", waypoint.prefix);
			values.put("lookup", waypoint.lookup);
			values.put("name", waypoint.name);
			values.put("latlon", waypoint.latlon);
			values.put("latitude_string", waypoint.latitudeString);
			values.put("longitude_string", waypoint.longitudeString);
			values.put("latitude", waypoint.latitude);
			values.put("longitude", waypoint.longitude);
			values.put("note", waypoint.note);

			if (id <= 0) {
				databaseRW.insert(dbTableWaypoints, null, values);
				ok = true;
			} else {
				final int rows = databaseRW.update(dbTableWaypoints, values, "_id = " + id, null);
				if (rows > 0) ok = true;
				else ok = false;
			}
			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}

		return ok;
    }

    public boolean deleteWaypoint(int id) {
        if (id == 0) return false;

        int deleted = databaseRW.delete(dbTableWaypoints, "_id = " + id, null);
        
        if (deleted > 0) return true;
        
        return false;
    }



    public boolean saveSpoilers(String geocode, ArrayList<cgSpoiler> spoilers) {
        if (geocode == null || geocode.length() == 0 || spoilers == null || spoilers.isEmpty()) return false;

		databaseRW.beginTransaction();
		try {
			databaseRW.delete(dbTableSpoilers, "geocode = \"" + geocode + "\"", null);

			ContentValues values = new ContentValues();
			for (cgSpoiler oneSpoiler : spoilers) {
				values.clear();
				values.put("geocode", geocode);
				values.put("updated", System.currentTimeMillis());
				values.put("url", oneSpoiler.url);
				values.put("title", oneSpoiler.title);
				values.put("description", oneSpoiler.description);

				databaseRW.insert(dbTableSpoilers, null, values);
			}
			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}

        return true;
    }

    public boolean saveLogs(String geocode, ArrayList<cgLog> logs) {
        if (geocode == null || geocode.length() == 0 || logs == null || logs.isEmpty()) return false;

		databaseRW.beginTransaction();
		try {
			databaseRW.delete(dbTableLogs, "geocode = \"" + geocode + "\"", null);

			ContentValues values = new ContentValues();
			for (cgLog oneLog : logs) {
				values.clear();
				values.put("geocode", geocode);
				values.put("updated", System.currentTimeMillis());
				values.put("type", oneLog.type);
				values.put("author", oneLog.author);
				values.put("log", oneLog.log);
				values.put("date", oneLog.date);
				values.put("found", oneLog.found);

				databaseRW.insert(dbTableLogs, null, values);
			}
			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}

        return true;
    }

    public boolean saveInventory(String geocode, ArrayList<cgTrackable> trackables) {
        if (geocode == null || geocode.length() == 0 || trackables == null || trackables.isEmpty()) return false;

		databaseRW.beginTransaction();
		try {
			databaseRW.delete(dbTableTrackables, "geocode = \"" + geocode + "\"", null);

			ContentValues values = new ContentValues();
			for (cgTrackable oneTrackable : trackables) {
				values.clear();
				values.put("geocode", geocode);
				values.put("updated", System.currentTimeMillis());
				values.put("tbcode", oneTrackable.geocode);
				values.put("guid", oneTrackable.guid);
				values.put("title", oneTrackable.name);
				values.put("owner", oneTrackable.owner);
				if (oneTrackable.released != null) {
					values.put("released", oneTrackable.released.getTime());
				} else {
					values.put("released", 0l);
				}
				values.put("goal", oneTrackable.goal);
				values.put("description", oneTrackable.details);

				databaseRW.insert(dbTableTrackables, null, values);

				saveLogs(oneTrackable.geocode, oneTrackable.logs);
			}
			databaseRW.setTransactionSuccessful();
		} finally {
			databaseRW.endTransaction();
		}

        return true;
    }

    public cgCache loadCache(String geocode, String guid) {
        Object[] geocodes = new Object[1];
        Object[] guids = new Object[1];

        if (geocode != null && geocode.length() > 0) {
            geocodes[0] = geocode;
        } else {
            geocodes = null;
        }

        if (guid != null && guid.length() > 0) {
            guids[0] = guid;
        } else {
            guids = null;
        }

        ArrayList<cgCache> caches =  loadCaches(geocodes, guids, false);
        if (caches != null && caches.isEmpty() == false) {
            return caches.get(0);
        }

        return null;
    }

    public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids) {
		return loadCaches(geocodes, guids, true);
	}

    public ArrayList<cgCache> loadCaches(Object[] geocodes, Object[] guids, boolean lite) {
		Cursor cursor = null;
        ArrayList<cgCache> caches = new ArrayList<cgCache>();

		try {
            if (geocodes != null && geocodes.length > 0) {
                StringBuilder all = new StringBuilder();
                for (Object one : geocodes) {
                    if (all.length() > 0) {
                        all.append(", ");
                    }
                    all.append("\"");
					all.append((String)one);
					all.append("\"");
                }

                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] {
                            "_id", "updated", "reason", "detailed", "detailedupdate", "geocode", "cacheid", "guid", "type", "name", "owner",
                            "hidden", "hint", "size", "difficulty", "terrain", "latlon", "latitude_string", "longitude_string", "location", "latitude", "longitude","shortdesc",
                            "description", "disabled", "archived", "members", "found", "favourite", "inventorycoins", "inventorytags", "inventoryunknown"
                        },
                        "geocode in (" + all.toString() + ")",
                        null,
                        null,
                        null,
                        null,
                        null
                        );
            } else if (guids != null && guids.length > 0) {
                StringBuilder all = new StringBuilder();
                for (Object one : guids) {
                    if (all.length() > 0) {
                        all.append(", ");
                    }
                    all.append("\"");
					all.append((String)one);
					all.append("\"");
                }

                cursor = databaseRO.query(
                        dbTableCaches,
                        new String[] {
                            "_id", "updated", "reason", "detailed", "detailedupdate", "geocode", "cacheid", "guid", "type", "name", "owner",
                            "hidden", "hint", "size", "difficulty", "terrain", "latlon", "latitude_string", "longitude_string", "location", "latitude", "longitude","shortdesc",
                            "description", "disabled", "archived", "members", "found", "favourite", "inventorycoins", "inventorytags", "inventoryunknown"
                        },
                        "guid in (" + all.toString() + ")",
                        null,
                        null,
                        null,
                        null,
                        null
                        );
            } else {
                return null;
            }

			if (cursor != null) {
                int index = 0;

                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    do {
                        cgCache cache = new cgCache();

                        cache.updated = (long)cursor.getLong(cursor.getColumnIndex("updated"));
                        cache.reason = (int)cursor.getInt(cursor.getColumnIndex("reason"));
                        index = cursor.getColumnIndex("detailed");
                        if ((int)cursor.getInt(index) == 1) {
                            cache.detailed = true;
                        } else {
                            cache.detailed = false;
                        }
                        cache.detailedUpdate = (Long)cursor.getLong(cursor.getColumnIndex("detailedupdate"));
                        cache.geocode = (String)cursor.getString(cursor.getColumnIndex("geocode"));
                        cache.cacheid = (String)cursor.getString(cursor.getColumnIndex("cacheid"));
                        cache.guid = (String)cursor.getString(cursor.getColumnIndex("guid"));
                        cache.type = (String)cursor.getString(cursor.getColumnIndex("type"));
                        cache.name = (String)cursor.getString(cursor.getColumnIndex("name"));
                        cache.owner = (String)cursor.getString(cursor.getColumnIndex("owner"));
                        cache.hidden = new Date((long)cursor.getLong(cursor.getColumnIndex("hidden")));
                        cache.hint = (String)cursor.getString(cursor.getColumnIndex("hint"));
                        cache.size = (String)cursor.getString(cursor.getColumnIndex("size"));
                        cache.difficulty = (Float)cursor.getFloat(cursor.getColumnIndex("difficulty"));
                        cache.terrain = (Float)cursor.getFloat(cursor.getColumnIndex("terrain"));
                        cache.latlon = (String)cursor.getString(cursor.getColumnIndex("latlon"));
                        cache.latitudeString = (String)cursor.getString(cursor.getColumnIndex("latitude_string"));
                        cache.longitudeString = (String)cursor.getString(cursor.getColumnIndex("longitude_string"));
                        cache.location = (String)cursor.getString(cursor.getColumnIndex("location"));
                        index = cursor.getColumnIndex("latitude");
                        if (cursor.isNull(index) == true) {
                            cache.latitude = null;
                        } else {
                            cache.latitude = (Double)cursor.getDouble(index);
                        }
                        index = cursor.getColumnIndex("longitude");
                        if (cursor.isNull(index) == true) {
                            cache.longitude = null;
                        } else {
                            cache.longitude = (Double)cursor.getDouble(index);
                        }
                        cache.shortdesc = (String)cursor.getString(cursor.getColumnIndex("shortdesc"));
                        cache.description = (String)cursor.getString(cursor.getColumnIndex("description"));
                        index = cursor.getColumnIndex("disabled");
                        if ((int)cursor.getLong(index) == 1) {
                            cache.disabled = true;
                        } else {
                            cache.disabled = false;
                        }
                        index = cursor.getColumnIndex("archived");
                        if ((int)cursor.getLong(index) == 1) {
                            cache.archived = true;
                        } else {
                            cache.archived = false;
                        }
                        index = cursor.getColumnIndex("members");
                        if ((int)cursor.getLong(index) == 1) {
                            cache.members = true;
                        } else {
                            cache.members = false;
                        }
                        index = cursor.getColumnIndex("found");
                        if ((int)cursor.getLong(index) == 1) {
                            cache.found = true;
                        } else {
                            cache.found = false;
                        }
                        index = cursor.getColumnIndex("favourite");
                        if ((int)cursor.getLong(index) == 1) {
                            cache.favourite = true;
                        } else {
                            cache.favourite = false;
                        }
                        cache.inventoryCoins = (Integer)cursor.getInt(cursor.getColumnIndex("inventorycoins"));
                        cache.inventoryTags = (Integer)cursor.getInt(cursor.getColumnIndex("inventorytags"));
                        cache.inventoryUnknown = (Integer)cursor.getInt(cursor.getColumnIndex("inventoryunknown"));

						if (lite == false) {
							ArrayList<String> attributes = loadAttributes(cache.geocode);
							if (attributes != null && attributes.isEmpty() == false) {
								cache.attributes.clear();
								cache.attributes.addAll(attributes);
							}

							ArrayList<cgWaypoint> waypoints = loadWaypoints(cache.geocode);
							if (waypoints != null && waypoints.isEmpty() == false) {
								cache.waypoints.clear();
								cache.waypoints.addAll(waypoints);
							}

							ArrayList<cgSpoiler> spoilers = loadSpoilers(cache.geocode);
							if (spoilers != null && spoilers.isEmpty() == false) {
								cache.spoilers.clear();
								cache.spoilers.addAll(spoilers);
							}

							ArrayList<cgLog> logs = loadLogs(cache.geocode);
							if (logs != null && logs.isEmpty() == false) {
								cache.logs.clear();
								cache.logs.addAll(logs);
							}

							ArrayList<cgTrackable> inventory = loadInventory(cache.geocode);
							if (inventory != null && inventory.isEmpty() == false) {
								cache.inventory.clear();
								cache.inventory.addAll(inventory);
							}
						}

                        caches.add(cache);
                    } while (cursor.moveToNext());
                } else {
					if (cursor != null) {
						cursor.close();
					}
                    
                    return null;
                }
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.loadCaches: " + e.toString());
		}

        if (cursor != null) cursor.close();

        return caches;
    }

    public ArrayList<String> loadAttributes(String geocode) {
        if (geocode == null || geocode.length() == 0) return null;

		Cursor cursor = null;
        ArrayList<String> attributes = new ArrayList<String>();

        cursor = databaseRO.query(
                dbTableAttributes,
                new String[] {"_id", "attribute"},
                "geocode = \"" + geocode + "\"",
                null,
                null,
                null,
                null,
                "100"
                );

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                attributes.add((String)cursor.getString(cursor.getColumnIndex("attribute")));
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();

        return attributes;
    }

    public cgWaypoint loadWaypoint(Integer id) {
        if (id == null || id == 0) return null;

		Cursor cursor = null;
        cgWaypoint waypoint = new cgWaypoint();

        cursor = databaseRO.query(
                dbTableWaypoints,
                new String[] {"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude_string", "longitude_string", "latitude", "longitude", "note"},
                "_id = " + id,
                null,
                null,
                null,
                null,
                "100"
                );

        if (cursor != null && cursor.getCount() > 0) {
            int index;
            cursor.moveToFirst();

            waypoint.id = (int)cursor.getInt(cursor.getColumnIndex("_id"));
            waypoint.geocode = (String)cursor.getString(cursor.getColumnIndex("geocode"));
            waypoint.type = (String)cursor.getString(cursor.getColumnIndex("type"));
            waypoint.prefix = (String)cursor.getString(cursor.getColumnIndex("prefix"));
            waypoint.lookup = (String)cursor.getString(cursor.getColumnIndex("lookup"));
            waypoint.name = (String)cursor.getString(cursor.getColumnIndex("name"));
            waypoint.latlon = (String)cursor.getString(cursor.getColumnIndex("latlon"));
            waypoint.latitudeString = (String)cursor.getString(cursor.getColumnIndex("latitude_string"));
            waypoint.longitudeString = (String)cursor.getString(cursor.getColumnIndex("longitude_string"));
            index = cursor.getColumnIndex("latitude");
            if (cursor.isNull(index) == true) {
                waypoint.latitude = null;
            } else {
                waypoint.latitude = (Double)cursor.getDouble(index);
            }
            index = cursor.getColumnIndex("longitude");
            if (cursor.isNull(index) == true) {
                waypoint.longitude = null;
            } else {
                waypoint.longitude = (Double)cursor.getDouble(index);
            }
            waypoint.note = (String)cursor.getString(cursor.getColumnIndex("note"));
        }

        if (cursor != null) cursor.close();

        return waypoint;
    }

    public ArrayList<cgWaypoint> loadWaypoints(String geocode) {
        if (geocode == null || geocode.length() == 0) return null;

		Cursor cursor = null;
        ArrayList<cgWaypoint> waypoints = new ArrayList<cgWaypoint>();

        cursor = databaseRO.query(
                dbTableWaypoints,
                new String[] {"_id", "geocode", "updated", "type", "prefix", "lookup", "name", "latlon", "latitude_string", "longitude_string", "latitude", "longitude", "note"},
                "geocode = \"" + geocode + "\"",
                null,
                null,
                null,
                null,
                "100"
                );

        if (cursor != null && cursor.getCount() > 0) {
            int index;
            cursor.moveToFirst();

            do {
                cgWaypoint waypoint = new cgWaypoint();
                waypoint.id = (int)cursor.getInt(cursor.getColumnIndex("_id"));
                waypoint.geocode = (String)cursor.getString(cursor.getColumnIndex("geocode"));
                waypoint.type = (String)cursor.getString(cursor.getColumnIndex("type"));
                waypoint.prefix = (String)cursor.getString(cursor.getColumnIndex("prefix"));
                waypoint.lookup = (String)cursor.getString(cursor.getColumnIndex("lookup"));
                waypoint.name = (String)cursor.getString(cursor.getColumnIndex("name"));
                waypoint.latlon = (String)cursor.getString(cursor.getColumnIndex("latlon"));
                waypoint.latitudeString = (String)cursor.getString(cursor.getColumnIndex("latitude_string"));
                waypoint.longitudeString = (String)cursor.getString(cursor.getColumnIndex("longitude_string"));
                index = cursor.getColumnIndex("latitude");
                if (cursor.isNull(index) == true) {
                    waypoint.latitude = null;
                } else {
                    waypoint.latitude = (Double)cursor.getDouble(index);
                }
                index = cursor.getColumnIndex("longitude");
                if (cursor.isNull(index) == true) {
                    waypoint.longitude = null;
                } else {
                    waypoint.longitude = (Double)cursor.getDouble(index);
                }
                waypoint.note = (String)cursor.getString(cursor.getColumnIndex("note"));
                
                waypoints.add(waypoint);
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();

        return waypoints;
    }

    public ArrayList<cgSpoiler> loadSpoilers(String geocode) {
        if (geocode == null || geocode.length() == 0) return null;

		Cursor cursor = null;
        ArrayList<cgSpoiler> spoilers = new ArrayList<cgSpoiler>();

        cursor = databaseRO.query(
                dbTableSpoilers,
                new String[] {"_id", "url", "title", "description"},
                "geocode = \"" + geocode + "\"",
                null,
                null,
                null,
                null,
                "100"
                );

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                cgSpoiler spoiler = new cgSpoiler();
                spoiler.url = (String)cursor.getString(cursor.getColumnIndex("url"));
                spoiler.title = (String)cursor.getString(cursor.getColumnIndex("title"));
                spoiler.description = (String)cursor.getString(cursor.getColumnIndex("description"));

                spoilers.add(spoiler);
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();

        return spoilers;
    }

    public ArrayList<cgLog> loadLogs(String geocode) {
        if (geocode == null || geocode.length() == 0) return null;

		Cursor cursor = null;
        ArrayList<cgLog> logs = new ArrayList<cgLog>();

        cursor = databaseRO.query(
                dbTableLogs,
                new String[] {"_id", "type", "author", "log", "date", "found"},
                "geocode = \"" + geocode + "\"",
                null,
                null,
                null,
                null,
                "100"
                );

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                cgLog log = new cgLog();
                log.type = (String)cursor.getString(cursor.getColumnIndex("type"));
                log.author = (String)cursor.getString(cursor.getColumnIndex("author"));
                log.log = (String)cursor.getString(cursor.getColumnIndex("log"));
                log.date = (String)cursor.getString(cursor.getColumnIndex("date"));
                log.found = (int)cursor.getInt(cursor.getColumnIndex("found"));

                logs.add(log);
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();

        return logs;
    }

    public ArrayList<cgTrackable> loadInventory(String geocode) {
        if (geocode == null || geocode.length() == 0) return null;

		Cursor cursor = null;
        ArrayList<cgTrackable> trackables = new ArrayList<cgTrackable>();

        cursor = databaseRO.query(
                dbTableTrackables,
                new String[] {"_id", "updated", "tbcode", "guid", "title", "owner", "released", "goal", "description"},
                "geocode = \"" + geocode + "\"",
                null,
                null,
                null,
                null,
                "100"
                );

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                cgTrackable trackable = new cgTrackable();
                trackable.geocode = (String)cursor.getString(cursor.getColumnIndex("tbcode"));
                trackable.guid = (String)cursor.getString(cursor.getColumnIndex("guid"));
                trackable.name = (String)cursor.getString(cursor.getColumnIndex("title"));
                trackable.owner = (String)cursor.getString(cursor.getColumnIndex("owner"));
                String releasedPre = cursor.getString(cursor.getColumnIndex("released"));
                if (releasedPre != null && Long.getLong(releasedPre) != null) {
                    trackable.released = new Date(Long.getLong(releasedPre));
                } else {
                    trackable.released = null;
                }
                trackable.goal = (String)cursor.getString(cursor.getColumnIndex("goal"));
                trackable.details = (String)cursor.getString(cursor.getColumnIndex("description"));
                trackable.logs = loadLogs(trackable.geocode);

                trackables.add(trackable);
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();

        return trackables;
    }

    public int getAllStoredCachesCount(boolean detailedOnly, String cachetype) {
        int count = 0;

        try {
			if (sqlCount == null) sqlCount = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 1");
			if (sqlCountDetailed == null) sqlCountDetailed = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 1 and detailed = 1");
			if (sqlCountTyped == null) sqlCountTyped = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 1 and type = ?");
			if (sqlCountDetailedTyped == null) sqlCountDetailedTyped = databaseRO.compileStatement("select count(_id) from " + dbTableCaches + " where reason = 1 and detailed = 1 and type = ?");

			if (detailedOnly == false) {
				if (cachetype == null) {
					count = (int)sqlCount.simpleQueryForLong();
				} else {
					sqlCountTyped.bindString(1, cachetype);
					count = (int)sqlCountTyped.simpleQueryForLong();
				}
			} else {
				if (cachetype == null) {
					count = (int)sqlCountDetailed.simpleQueryForLong();
				} else {
					sqlCountDetailedTyped.bindString(1, cachetype);
					count = (int)sqlCountDetailedTyped.simpleQueryForLong();
				}
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.loadAllStoredCachesCount: " + e.toString());
		}

        return count;
    }

    public ArrayList<String> loadBatchOfStoredGeocodes(boolean detailedOnly, Double latitude, Double longitude, String cachetype) {
		Cursor cursor = null;
        ArrayList<String> geocodes = new ArrayList<String>();

        StringBuilder specifySql = new StringBuilder();
        if (detailedOnly == true) {
            specifySql.append(" and detailed = 1");
        }
		if (cachetype != null) {
			specifySql.append(" and type = \"");
			specifySql.append(cachetype);
			specifySql.append("\"");
		}

		try {
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] {"_id", "geocode", "(abs(latitude-" + String.format((Locale)null, "%.6f", latitude) + ") + abs(longitude-" + String.format((Locale)null, "%.6f", longitude) + ")) as dif"},
                    "reason = 1" + specifySql.toString(),
                    null,
                    null,
                    null,
                    "dif",
                    "1000"
                    );

			if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    do {
                        geocodes.add((String)cursor.getString(cursor.getColumnIndex("geocode")));
                    } while (cursor.moveToNext());
                } else {
                    cursor.close();
                    return null;
                }

				cursor.close();
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.loadAllGeocodes: " + e.toString());
		}

        return geocodes;
    }

    public boolean markStored(String geocode) {
        if (geocode == null || geocode.length() == 0) return false;

        ContentValues values = new ContentValues();
        values.put("reason", 1);
        int rows = databaseRW.update(dbTableCaches, values, "geocode = \"" + geocode + "\"", null);

        if (rows > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean markDropped(String geocode) {
        if (geocode == null || geocode.length() == 0) return false;

		try {
			ContentValues values = new ContentValues();
			values.put("reason", 0);
			int rows = databaseRW.update(dbTableCaches, values, "geocode = \"" + geocode + "\"", null);

			if (rows > 0) {
				databaseRO.delete(dbTableAttributes, "geocode = \"" + geocode + "\"" , null);
				databaseRO.delete(dbTableSpoilers, "geocode = \"" + geocode + "\"" , null);
				databaseRO.delete(dbTableLogs, "geocode = \"" + geocode + "\"" , null);
				databaseRO.delete(dbTableWaypoints, "geocode = \"" + geocode + "\" and type <> \"own\"" , null);
				databaseRO.delete(dbTableTrackables, "geocode = \"" + geocode + "\"" , null);
				
				return true;
			}
		} catch(Exception e) {
			Log.e(cgSettings.tag, "cgData.markDropped: " + e.toString());
		}

        return false;
    }

    public void dropStored() {
		Cursor cursor = null;
		ArrayList<String> geocodes = new ArrayList<String>();

		try {
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] {"_id", "geocode"},
                    "reason = 1",
                    null,
                    null,
                    null,
                    null,
                    null
                    );

			if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    do {
                        geocodes.add("\"" + (String)cursor.getString(cursor.getColumnIndex("geocode")) + "\"");
                    } while (cursor.moveToNext());
                } else {
                    cursor.close();
                    return;
                }

				cursor.close();
			}

			databaseRW.delete(dbTableCaches, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableAttributes, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableSpoilers, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableLogs, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableWaypoints, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ") and type <> \"own\"", null);
			databaseRW.delete(dbTableTrackables, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);

			geocodes.clear();
        } catch(Exception e) {
            Log.e(cgSettings.tag, "cgData.dropStored: " + e.toString());
        }
    }

	public void clearCache() {
		Cursor cursor = null;
		ArrayList<String> geocodes = new ArrayList<String>();

		try {
            cursor = databaseRO.query(
                    dbTableCaches,
                    new String[] {"_id", "geocode"},
                    "reason = 0",
                    null,
                    null,
                    null,
                    null,
                    null
                    );

			if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    do {
                        geocodes.add("\"" + (String)cursor.getString(cursor.getColumnIndex("geocode")) + "\"");
                    } while (cursor.moveToNext());
                } else {
                    cursor.close();
                    return;
                }

				cursor.close();
			}

			databaseRW.delete(dbTableCaches, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableAttributes, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableSpoilers, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableLogs, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);
			databaseRW.delete(dbTableWaypoints, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ") and type <> \"own\"", null);
			databaseRW.delete(dbTableTrackables, "geocode in (" + cgBase.implode(", ", geocodes.toArray()) + ")", null);

			geocodes.clear();
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgData.clearCache: " + e.toString());
		}
	}
}