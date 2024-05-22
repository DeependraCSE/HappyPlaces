package com.example.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentProviderClient
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.GetAddressFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Objects
import java.util.UUID
import java.util.concurrent.Executors

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "AddHappyPlaceActivity"
    private var cal = Calendar.getInstance()
    private lateinit var setDateListener : DatePickerDialog.OnDateSetListener
    private var binding : ActivityAddHappyPlaceBinding? = null
    private var saveImageToInternalStorage : Uri? = null
    private var mLatitude : Double = 0.0
    private var mLongitude : Double = 0.0
    private var id : Int = 0

    private var mHappyPlaceDetails : HappyPlaceModel? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    companion object{
        private const val IMAGE_DIRECTORY = "HappyPlacesImage"
    }
    private var resultLauncherForGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if (result.resultCode == Activity.RESULT_OK){
            if (result.data != null){
                val contentUri = result?.data?.data
                try {
                    val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentUri)
                    saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                    Log.e("Gallery Image","path:$saveImageToInternalStorage")
                    binding?.ivPlaceImage?.setImageBitmap(selectedImageBitmap)
                }catch (e : IOException){
                    e.printStackTrace()
                }
            }
        }
    }
    private var resultLauncherForCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if (result.resultCode == Activity.RESULT_OK){
            if (result.data != null){
                val thumbNail : Bitmap = result?.data?.extras!!.get("data") as Bitmap
                saveImageToInternalStorage = saveImageToInternalStorage(thumbNail)
                Log.e("Camera Image","path:$saveImageToInternalStorage")
                binding?.ivPlaceImage?.setImageBitmap(thumbNail)
            }
        }
    }

    private val autoCompleteAddress = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if (result.resultCode == Activity.RESULT_OK){
            val place : Place = Autocomplete.getPlaceFromIntent(result!!.data)
            binding?.etLocation?.setText(place.address)
            mLatitude = place.latLng!!.latitude
            mLongitude = place.latLng!!.longitude
        }else{
            binding?.etLocation?.setText("Place Api Not Working, Address not found")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolBarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolBarAddPlace?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        if (!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,resources.getString(R.string.google_maps_api_key))
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAIL)){
            mHappyPlaceDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAIL, HappyPlaceModel::class.java)
            } else {
                intent.getParcelableExtra<HappyPlaceModel>(MainActivity.EXTRA_PLACE_DETAIL)
            }
        }

        setDateListener = DatePickerDialog.OnDateSetListener{_, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        updateDateInView()
        if (mHappyPlaceDetails != null){
            supportActionBar?.title = "Edit Happy Place"
            id = mHappyPlaceDetails!!.id

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)

            binding?.btnSave?.text = "UPDATE"
        }
        binding?.btnSave?.setOnClickListener(this)
        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvCurrentLocation?.setOnClickListener(this)
    }

    private fun isLocationEnable():Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = LocationRequest.create().apply {
            interval = 1000
            numUpdates = 1
            priority = Priority.PRIORITY_HIGH_ACCURACY
            fastestInterval = 50
            maxWaitTime = 1000
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallBack, Looper.myLooper())
    }

    private val mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            //super.onLocationResult(locationResult)
            val mLastLocation : Location = locationResult!!.lastLocation!!
            mLatitude = mLastLocation.latitude
            mLongitude = mLastLocation.longitude

            val getAddressFromLatLng = GetAddressFromLatLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)
            getAddressFromLatLng.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    binding?.etLocation?.setText(address)
                }

                override fun onError() {
                    Log.e(TAG,"CurrentLocationError")
                }
            })
            getAddressFromLatLng.getAddress()
        }
    }



    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.etDate ->{
                DatePickerDialog(this@AddHappyPlaceActivity, setDateListener
                    , cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            R.id.tvAddImage ->{
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from gallery","Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){_,which->
                    when(which){
                        0-> choosePhotoFromGallery()
                        1-> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btnSave ->{
                when{
                    binding?.etTitle?.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter title",Toast.LENGTH_LONG).show()
                    }
                    binding?.etDescription?.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter description",Toast.LENGTH_LONG).show()
                    }
                    binding?.etLocation?.text.isNullOrEmpty()->{
                        Toast.makeText(this, "Please enter location",Toast.LENGTH_LONG).show()
                    }
                    saveImageToInternalStorage == null->{
                        Toast.makeText(this, "Please select image",Toast.LENGTH_LONG).show()
                    }
                    else->{
                        val happyPlaceModel = HappyPlaceModel(id,
                            binding?.etTitle?.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding?.etDescription?.text.toString(),
                            binding?.etDate?.text.toString(),
                            binding?.etLocation?.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)
                        var result = 0;
                        if (id == 0){
                            result = dbHandler.addHappyPlace(happyPlaceModel).toInt()
                        }else if (id > 0){
                            result = dbHandler.updateHappyPlace(happyPlaceModel)
                        }
                        if (result > 0){
                            //Toast.makeText(this, "The happy place detailed are inserted successfully",Toast.LENGTH_LONG).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
            }
            R.id.etLocation ->{
                try{
                    val fields = listOf(Place.Field.ID,Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this@AddHappyPlaceActivity)
                    autoCompleteAddress.launch(intent)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            R.id.tvCurrentLocation->{
                if (!isLocationEnable()){
                    Toast.makeText(this,"Your location provider is turned off. Please turn on",Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withContext(this).withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION).withListener(
                        object : MultiplePermissionsListener{

                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if (report!!.areAllPermissionsGranted()){
                                    requestNewLocationData()
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ) {
                                showRationalDialogForPermissions()
                            }
                        }
                    ).onSameThread().check()
                }
            }
        }
    }

    private fun takePhotoFromCamera(){
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA).withListener(
            object : MultiplePermissionsListener{

                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        resultLauncherForCamera.launch(cameraIntent)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }
        ).onSameThread().check()
    }

    private fun choosePhotoFromGallery(){
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(
                object : MultiplePermissionsListener{

                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()){
                            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            resultLauncherForGallery.launch(galleryIntent)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }
            ).onSameThread().check()
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage(
            "It looks like you have turned off permission required for for this feature, " +
                    "It can be enable under the application setting")
            .setPositiveButton("GO TO SETTINGS"){dialog,_ ->
                try {
                    val intent  = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data = uri
                    startActivity(intent)
                    dialog.dismiss()
                }catch (e : ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){dialog,_ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateDateInView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap : Bitmap) : Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        try {
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        }catch (e : IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}