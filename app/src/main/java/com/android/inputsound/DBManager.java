package com.android.inputsound;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.StringTokenizer;

/**
 * Created by 정승현 on 2015-09-24.
 */
public class DBManager extends SQLiteOpenHelper{


    public DBManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE LogData(Month INTEGER, Day INTEGER, InDcb INTEGER, OutDcb INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean insert(String query) {
        SQLiteDatabase db = getWritableDatabase();
        StringTokenizer st = new StringTokenizer(query);
        if(st.nextToken().equals("INSERT")) {
            db.execSQL(query);
            db.close();
            return true;
        }
        else {
            db.close();
            return false;
        }
    }

    public boolean update(String query) {
        SQLiteDatabase db = getWritableDatabase();
        StringTokenizer st = new StringTokenizer(query);
        if(st.nextToken().equals("UPDATE")){
            db.execSQL(query);
        db.close();
        return true;
        }
        else {
            db.close();
            return false;
        }
    }

    public boolean delete(String query) {
        SQLiteDatabase db = getWritableDatabase();
        StringTokenizer st = new StringTokenizer(query);
        if(st.nextToken().equals("DELETE")){
            db.execSQL(query);
            db.close();
            return true;
        }
        else {
            db.close();
            return false;
     }
    }

    public String select(String query) {
        SQLiteDatabase db = getReadableDatabase();
        StringTokenizer st = new StringTokenizer(query);
        String result="";
        if(st.nextToken().equals("SELECT")){
            Cursor cs = db.rawQuery(query,null);
            while(cs.moveToNext()){
                result+="Month : "+cs.getInt(0)+" Day : "+cs.getInt(1)+" InDecibel : "+cs.getInt(2)+" OutDecibel : "+cs.getInt(3)+"\n";
            }
            db.close();
            return result;
        }
        else {
            db.close();
            return "select_fail";
        }
    }


}
