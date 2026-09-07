package com.minews1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class HistoryDb extends SQLiteOpenHelper {
    public static class Reading { public long ts; public float temp, hum; Reading(long t,float a,float h){ts=t;temp=a;hum=h;} }
    public HistoryDb(Context c){super(c,"minew_history.db",null,2);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE readings(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, temp REAL NOT NULL, hum REAL NOT NULL, device_id TEXT NOT NULL)");db.execSQL("CREATE INDEX idx_ts ON readings(ts)");db.execSQL("CREATE INDEX idx_device_ts ON readings(device_id,ts)");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2){db.execSQL("ALTER TABLE readings ADD COLUMN device_id TEXT NOT NULL DEFAULT '78:05:41:06:91:BA'");db.execSQL("CREATE INDEX IF NOT EXISTS idx_device_ts ON readings(device_id,ts)");}}
    public void add(long ts,float temp,float hum){add("78:05:41:06:91:BA",ts,temp,hum);}
    public void add(String deviceId,long ts,float temp,float hum){getWritableDatabase().execSQL("INSERT INTO readings(ts,temp,hum,device_id) VALUES(?,?,?,?)",new Object[]{ts,temp,hum,deviceId.toUpperCase()});}
    public List<Reading> since(long from){return range("78:05:41:06:91:BA",from,Long.MAX_VALUE);}
    public List<Reading> since(String deviceId,long from){return range(deviceId,from,Long.MAX_VALUE);}
    public List<Reading> range(long from,long to){return range("78:05:41:06:91:BA",from,to);}
    public List<Reading> range(String deviceId,long from,long to){List<Reading> r=new ArrayList<>();Cursor c=getReadableDatabase().query("readings",new String[]{"ts","temp","hum"},"device_id=? AND ts>=? AND ts<?",new String[]{deviceId.toUpperCase(),Long.toString(from),Long.toString(to)},null,null,"ts ASC");try{while(c.moveToNext())r.add(new Reading(c.getLong(0),c.getFloat(1),c.getFloat(2)));}finally{c.close();}return r;}
}
