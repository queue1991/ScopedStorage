package com.hsj.scopedstorage

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import com.hsj.scopedstorage.imglist.ImageListViewActivity
import java.io.File
import java.io.FileOutputStream

/**
 * android 11을 대응하면서
 * MediaStore을 통해
 * 이미지를 저장하고 로드하는
 * 로직 정리
 *
 * 이미지 촬영 화면
 * 이미지 리뷰 화면
 * 이미지 리스트 화면 -> MediaStore 로드
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun btnClicked(v : View){
        when(v.id){
            R.id.btn_1 -> {
                val nextIntent = Intent(this, ImageListViewActivity::class.java)
                startActivity(nextIntent)
            }
            R.id.btn_2 -> {

            }
        }
    }

    /**
     * 이미지 파일 저장 function
     * 1. file = 이미지 파일
     * 2. imagePath = 이미지 파일의 절대 경로 (file.absolutePath)
     * 3. File은 -> getExternalFilesDir(Environment.DIRECTORY_PICTURES)로 얻은 경로
     */
    private fun insertMediaStore(file: File, imagePath : String) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val item: Uri = contentResolver.insert(collection, values)!!

            contentResolver.openFileDescriptor(item, "w", null).use {
                // write something to OutputStream
                FileOutputStream(it!!.fileDescriptor).use { outputStream ->
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(item, values, null, null)
                    outputStream.close()
                }
            }
        }else{
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        }
    }

}