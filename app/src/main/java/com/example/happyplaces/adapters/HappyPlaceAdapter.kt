package com.example.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.activities.AddHappyPlaceActivity
import com.example.happyplaces.activities.MainActivity
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ItemHappyPlaceBinding
import com.example.happyplaces.models.HappyPlaceModel

open class HappyPlaceAdapter(private val context : Context,
                             private val list : ArrayList<HappyPlaceModel>):
    RecyclerView.Adapter<HappyPlaceAdapter.MyViewHolder>() {
    private var onClickListener : OnClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemHappyPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position]
        holder.tvTitle.text = model.title
        holder.tvDescription.text = model.description
        holder.ivPlaceImage.setImageURI(Uri.parse(model.image))
        holder.itemView.setOnClickListener{
            if (onClickListener != null){
                onClickListener?.onClick(position, model)
            }
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    fun notifyEditItem(activity : Activity, position : Int){
        val intent = Intent(context,AddHappyPlaceActivity::class.java )
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAIL, list[position])
        (activity as MainActivity).resultLauncherForAddPlace.launch(intent)
        notifyItemChanged(position)
    }

    fun removeAt(position : Int){
        val dbHandler = DatabaseHandler(context)
        val isDeleted = dbHandler.deleteHappyPlace(list[position])
        if (isDeleted > 0){
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    class MyViewHolder(binding : ItemHappyPlaceBinding):RecyclerView.ViewHolder(binding.root){
        val tvTitle = binding.tvTitle
        val tvDescription = binding.tvDescription
        val ivPlaceImage = binding.ivPlaceImage
    }

    interface OnClickListener{
        fun onClick(position : Int, model : HappyPlaceModel)
    }
}