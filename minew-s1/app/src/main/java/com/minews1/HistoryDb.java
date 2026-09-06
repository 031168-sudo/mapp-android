package com.minews1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class HistoryDb extends SQLiteOpenHelper {
    public static class Reading { public long ts; public float temp, hum; Reading(long t,float a,float h){ts=t;temp=a;hum=h;} }
    public HistoryDb(Context c){super(c,"minew_history.db",null,1);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE readings(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, temp REAL NOT NULL, hum REAL NOT NULL)");db.execSQL("CREATE INDEX idx_ts ON readings(ts)");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
    public void add(long ts,float temp,float hum){getWritableDatabase().execSQL("INSERT INTO readings(ts,temp,hum) VALUES(?,?,?)",new Object[]{ts,temp,hum});}
    public List<Reading> since(long from){List<Reading> r=new ArrayList<>();Cursor c=getReadableDatabase().query("readings",new String[]{"ts","temp","hum"},"ts>=?",new String[]{Long.toString(from)},null,null,"ts ASC");try{while(c.moveToNext())r.add(new Reading(c.getLong(0),c.getFloat(1),c.getFloat(2)));}finally{c.close();}return r;}
}
