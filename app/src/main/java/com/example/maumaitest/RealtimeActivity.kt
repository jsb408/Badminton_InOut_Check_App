package com.example.maumaitest

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_realtime.*
import kotlinx.android.synthetic.main.list_item_results.view.*
import java.io.File

class RealtimeActivity : AppCompatActivity() {
    private var tempFile: File? = null
    private val PICKFROMALBUM = 1
    private var isPermissioned = 0

    val permissionList = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime)

        checkPermission()

        if(isPermissioned >= permissionList.size) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
            startActivityForResult(intent, PICKFROMALBUM)
        }

        resultRecyclerView.adapter = mAdapter()
        resultRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun checkPermission() {
        for (pms: String in permissionList) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, pms)

            if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissionList, 0)
            } else isPermissioned++
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (idx in grantResults.indices) {
            if (grantResults[idx] != PackageManager.PERMISSION_GRANTED) finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            tempFile.let {
                if(tempFile!!.exists()) {
                    if (tempFile!!.delete()) tempFile = null
                }
            }
        } else if (requestCode == PICKFROMALBUM) {
            data?.data?.let {
                try {
                    videoView.setVideoURI(data.data)
                    videoView.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

class mAdapter() : RecyclerView.Adapter<mAdapter.ItemViewHolder>() {
    override fun getItemCount(): Int = dataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): mAdapter.ItemViewHolder {
        val adapterView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_results, parent, false)
        return ItemViewHolder(adapterView)
    }

    override fun onBindViewHolder(holder: mAdapter.ItemViewHolder, position: Int) {
        holder.bindItemData(dataList[position])
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItemData(itemData: TestData) {
            itemView.resultText.text = itemData.result
            itemView.timeText.text = itemData.time
        }
    }
}
