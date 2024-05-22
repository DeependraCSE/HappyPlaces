package com.example.happyplaces.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.example.happyplaces.models.HappyPlaceModel

class DatabaseHandler (context : Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object{
        private const val DATABASE_VERSION = 1      // database version
        private const val DATABASE_NAME = "HappyPlacesDatabase"     //database name
        private const val TABLE_HAPPY_PLACE = "HappyPlaceTable"     //table name

        //All the column name
        private const val KEY_ID = "_id"
        private const val KEY_NAME = "name"
        private const val KEY_TITLE = "title"
        private const val KEY_IMAGE = "image"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_DATE = "data"
        private const val KEY_LOCATION = "location"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_HAPPY_PLACE_TABLE = ("CREATE TABLE " + TABLE_HAPPY_PLACE + " ("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_IMAGE + " TEXT,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_DATE + " TEXT,"
                + KEY_LOCATION + " TEXT,"
                + KEY_LATITUDE + " TEXT,"
                + KEY_LONGITUDE + " TEXT)")
        db?.execSQL(CREATE_HAPPY_PLACE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_HAPPY_PLACE")
        onCreate(db)
    }

    fun addHappyPlace(happyPlaceModel: HappyPlaceModel):Long{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE,happyPlaceModel.title)
        contentValues.put(KEY_IMAGE,happyPlaceModel.image)
        contentValues.put(KEY_DESCRIPTION,happyPlaceModel.description)
        contentValues.put(KEY_DATE,happyPlaceModel.date)
        contentValues.put(KEY_LOCATION,happyPlaceModel.location)
        contentValues.put(KEY_LATITUDE,happyPlaceModel.latitude)
        contentValues.put(KEY_LONGITUDE,happyPlaceModel.longitude)

        val result = db.insert(TABLE_HAPPY_PLACE, null, contentValues)
        db.close()
        return result
    }

    fun updateHappyPlace(happyPlaceModel: HappyPlaceModel):Int{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE,happyPlaceModel.title)
        contentValues.put(KEY_IMAGE,happyPlaceModel.image)
        contentValues.put(KEY_DESCRIPTION,happyPlaceModel.description)
        contentValues.put(KEY_DATE,happyPlaceModel.date)
        contentValues.put(KEY_LOCATION,happyPlaceModel.location)
        contentValues.put(KEY_LATITUDE,happyPlaceModel.latitude)
        contentValues.put(KEY_LONGITUDE,happyPlaceModel.longitude)

        val success = db.update(TABLE_HAPPY_PLACE,  contentValues, KEY_ID + "=" + happyPlaceModel.id, null)
        db.close()
        return success
    }

    fun getHappyPlacesList() : ArrayList<HappyPlaceModel>{
        val happyPlaceList = ArrayList<HappyPlaceModel>()
        val sqlQuery = "SELECT * FROM $TABLE_HAPPY_PLACE"
        val db = this.readableDatabase
        try {
            val cursor : Cursor = db.rawQuery(sqlQuery, null)
            if (cursor.moveToFirst()){
                do {
                    val place = HappyPlaceModel(
                        cursor.getInt(cursor.getColumnIndex(KEY_ID)),
                        cursor.getString(cursor.getColumnIndex(KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndex(KEY_IMAGE)),
                        cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(KEY_DATE)),
                        cursor.getString(cursor.getColumnIndex(KEY_LOCATION)),
                        cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE))
                    )
                    happyPlaceList.add(place)
                }while (cursor.moveToNext())
            }
            cursor.close()
        }catch (e:SQLiteException){
            e.printStackTrace()
        }
        return happyPlaceList
    }

    fun deleteHappyPlace(happyPlaceModel: HappyPlaceModel):Int{
        val db = this.writableDatabase
        val success = db.delete(TABLE_HAPPY_PLACE, KEY_ID+"="+happyPlaceModel.id,null)
        db.close()
        return success
    }
}