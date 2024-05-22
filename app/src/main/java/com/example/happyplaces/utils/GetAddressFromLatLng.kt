package com.example.happyplaces.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class GetAddressFromLatLng(context : Context,
                           private val latitude : Double,
                           private val longitude : Double){
    private val TAG = "GetAddressFromLatLng"
    private val geoCoder : Geocoder = Geocoder(context, Locale.getDefault())
    private lateinit var mAddressListener : AddressListener

    fun getAddress(){
        var executor = Executors.newSingleThreadExecutor()
        var handler = Handler(Looper.getMainLooper())
        executor.execute {
            //background task
            val sb = StringBuilder()
            try {
                val addressList : List<Address>? = geoCoder.getFromLocation(latitude, longitude,1)
                if (!addressList.isNullOrEmpty()){
                    val address : Address = addressList[0]
                    for (i in 0..address.maxAddressLineIndex){
                        sb.append(address.getAddressLine(i)).append(" ")
                    }
                    sb.deleteCharAt(sb.length-1)
                }else{
                    Log.e(TAG,"AddressIsNullOrEmpty")
                }
            }catch (e : Exception){
                e.printStackTrace()
            }


            handler.post{
                //UI thread
                if (sb == null){
                    mAddressListener.onError()
                }else{
                    mAddressListener.onAddressFound(sb.toString())
                }
            }
        }
    }

    fun setAddressListener(addressListener: AddressListener){
        mAddressListener = addressListener
    }

    interface AddressListener{
        fun onAddressFound(address : String?)
        fun onError()
    }
}