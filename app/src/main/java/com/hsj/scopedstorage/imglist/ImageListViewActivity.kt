package com.hsj.scopedstorage.imglist

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.hsj.scopedstorage.R
import java.io.File

/**
 * Glide -> placeholder 넣기전에 잘 안된 이유가 뭐지
 */
class ImageListViewActivity : AppCompatActivity(){
    private var gridView : GridView? = null
    private var adapter: ImageGalleryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gridView = findViewById(R.id.gridView)
        setContentView(R.layout.activity_imagelist)

    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            setAdapter()
            Log.d("tag", "permission granted")


        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),0)
            // Permission is not granted
            Log.d("tag", "permission not granted")

        }
    }

    private fun setAdapter() {
        gridView = findViewById(R.id.gridView)
        adapter = ImageGalleryAdapter(this, createItemList(contentResolver))
        gridView!!.adapter = adapter
        adapter!!.notifyDataSetChanged()

    }

    private fun createItemList(contentResolver: ContentResolver): ArrayList<PickerItem> {
        val arrayList = ArrayList<PickerItem>().apply {
            add(0, PickerItem(null, false)) //사진 촬영 아이템
        }
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(MediaStore.Images.Thumbnails.DATA)
        val orderBy = MediaStore.Images.Media._ID + " DESC"
        val cursor = contentResolver.query(uri, columns, null, null, orderBy)?.apply {
//            val columnData = MediaStore.Images.ImageColumns._ID
            val columnData = MediaStore.Images.Media.DATA
            while (moveToNext()) {
                val path = getString(getColumnIndexOrThrow(columnData))
                val fileUri = Uri.fromFile(File(path))
                arrayList.add(PickerItem(fileUri, false))
            }
        }

        cursor?.close()
        return arrayList
    }


    class ViewHolder(val itemView: View) {
        val ivThumbnail = itemView.findViewById<ImageView>(R.id.iv_thumbnail)!!
        val ivBroken = itemView.findViewById<ImageView>(R.id.iv_broken)!!
        val ivPhoto = itemView.findViewById<ImageView>(R.id.iv_photo)!!
        val selector = itemView.findViewById<FrameLayout>(R.id.frame_selector)!!
        var item: PickerItem? = null
    }

    class ImageGalleryAdapter(private val context: Context, private val data: ArrayList<PickerItem>) : BaseAdapter() {
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return data.size
        }

        override fun getItem(position: Int): PickerItem? {
            return if (data.isNotEmpty()) data[position] else null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var view = convertView
            val viewHolder: ViewHolder
            if (null == view) {
                view = LayoutInflater.from(context).inflate(R.layout.view_image_picker_item, parent, false)
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }

            val item = getItem(position)
            viewHolder.selector.isSelected = item!!.isSelected

            if (null == viewHolder.item || viewHolder.item != item) {
                viewHolder.ivThumbnail.visibility = View.VISIBLE
                viewHolder.ivPhoto.visibility = View.GONE
                viewHolder.selector.visibility = View.VISIBLE
                viewHolder.item = item
                viewHolder.itemView.postDelayed({
                    if (null == viewHolder.item?.uri) {
                        viewHolder.ivPhoto.visibility = View.VISIBLE
                        viewHolder.ivThumbnail.visibility = View.GONE
                        viewHolder.selector.visibility = View.GONE
                    } else {
                        Glide.with(context)
                            .load(viewHolder.item!!.uri)
                            .thumbnail(0.25f)
                            .placeholder(R.drawable.ic_launcher_background)
                            .listener(object : RequestListener<Drawable>{
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    viewHolder.selector.visibility = View.GONE
                                    viewHolder.ivBroken.visibility = View.VISIBLE
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    viewHolder.selector.visibility = View.VISIBLE
                                    viewHolder.ivBroken.visibility = View.GONE
                                    return false
                                }



                            })
                            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(viewHolder.ivThumbnail)
                    }
                }, 1)
            }
            return view
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            0 -> {  // 1
                if (grantResults.isEmpty()) {  // 2
                    throw RuntimeException("Empty permission result")
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {  // 3
                    Log.d("tag", "permission granted")
                    setAdapter()

                } else {
                    if (shouldShowRequestPermissionRationale(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) { // 4
                        requestPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            0)
                    } else {
                        Log.d("tag", "User declined and i can't ask")
                    }
                }
            }
        }
    }

}