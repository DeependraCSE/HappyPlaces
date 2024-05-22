package com.example.happyplaces.activities

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ActivityMapBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private var binding : ActivityMapBinding? = null
    private var happyPlaceDetailModel : HappyPlaceModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolBarMap)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolBarMap?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAIL)){
            happyPlaceDetailModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAIL, HappyPlaceModel::class.java)
            } else {
                intent.getParcelableExtra<HappyPlaceModel>(MainActivity.EXTRA_PLACE_DETAIL)
            }
        }

        if (happyPlaceDetailModel != null){
            supportActionBar?.title = happyPlaceDetailModel?.title
        }

        val supportMapFragment : SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        var possition = LatLng(happyPlaceDetailModel!!.latitude, happyPlaceDetailModel!!.longitude)
        //possition = LatLng(22.686850, 75.833900)
        googleMap.addMarker(MarkerOptions().position(possition).title(happyPlaceDetailModel!!.location))
        val newLatLongZoom = CameraUpdateFactory.newLatLngZoom(possition, 15f)
        googleMap.animateCamera(newLatLongZoom)
    }
}