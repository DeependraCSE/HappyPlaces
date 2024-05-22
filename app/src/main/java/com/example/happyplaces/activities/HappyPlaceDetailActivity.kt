package com.example.happyplaces.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ActivityHappyPlaceDetailBinding
import com.example.happyplaces.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    private var binding : ActivityHappyPlaceDetailBinding? = null
    private var happyPlaceDetailModel : HappyPlaceModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolBarPlaceDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolBarPlaceDetail?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAIL)){
            //happyPlaceDetailModel = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAIL) as HappyPlaceModel
            //happyPlaceDetailModel = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAIL, HappyPlaceModel::class.java) as HappyPlaceModel
            happyPlaceDetailModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAIL, HappyPlaceModel::class.java)
            } else {
                intent.getParcelableExtra<HappyPlaceModel>(MainActivity.EXTRA_PLACE_DETAIL)
            }
        }
        if (happyPlaceDetailModel != null){
            supportActionBar?.title = happyPlaceDetailModel?.title
            binding?.tvDescription?.text = happyPlaceDetailModel?.description
            binding?.ivPaceImage?.setImageURI(Uri.parse(happyPlaceDetailModel?.image))
            binding?.tvLocation?.text = happyPlaceDetailModel?.location
            binding?.btnViewOnMap?.setOnClickListener{
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_PLACE_DETAIL, happyPlaceDetailModel)
                startActivity(intent)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}