package com.hsj.scopedstorage.saveimage

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.hsj.scopedstorage.R
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SaveImageActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private var saveFile: File? = null

    private var displayId: Int = -1
    private val lensFacing = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private var viewFinder : AspectRatioPreviewView? = null
    private var ibt_take_picture : ImageButton? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(newDisplayId: Int) {
            if (displayId == newDisplayId) {
                imageCapture?.targetRotation = viewFinder!!.display.rotation
            }
        }
    }

    private val onImageSavedListener: ImageCapture.OnImageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val file = outputFileResults.savedUri?.toFile() ?: saveFile ?: return
            insertMediaStore(file)

        }

        override fun onError(exception: ImageCaptureException) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saveimage)

        cameraExecutor = Executors.newSingleThreadExecutor()
        displayManager.registerDisplayListener(displayListener, null)

        viewFinder = findViewById(R.id.viewFinder)
        ibt_take_picture = findViewById(R.id.ibt_take_picture)

        viewFinder!!.setOnTouchListener { touchedView, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val factory = SurfaceOrientedMeteringPointFactory(viewFinder!!.width.toFloat(), viewFinder!!.height.toFloat())
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                camera?.cameraControl?.startFocusAndMetering(action)
                true
            } else
                touchedView.performClick()
        }

    }


    override fun onResume() {
        super.onResume()

        if (checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            setUpCamera()
            updateCameraUi()


        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA),0)
            // Permission is not granted

        }
    }

    private fun setUpCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val aspectRatio = AspectRatio.RATIO_4_3

        DisplayMetrics().also { viewFinder!!.display.getRealMetrics(it) }
        val rotation = viewFinder!!.display.rotation

        preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .build()
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .build()

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            preview?.setSurfaceProvider(viewFinder!!.createSurfaceProvider())
        } catch (e: Exception) {
        }
    }

    /**
     * getExternalFilesDir(Environment.DIRECTORY_PICTURES --> 외부 저장소의 Picture
     */
    private fun updateCameraUi() {
        ibt_take_picture!!.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                val metadata = ImageCapture.Metadata().apply {
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
                    .setMetadata(metadata)
                    .build()
                saveFile = file
                imageCapture?.takePicture(outputOptions, cameraExecutor, onImageSavedListener)
            }

        } )
    }

    /**
     * 사진 촬영 후 저장하는 로직
     * targetSdkVersion 30 대응으로 3.13.8 부터 삭제.
     *
     * MediaStore에 저장하는데에 READ_EXTERNAL_STRAGE / WRITE_EXTERNAL_STRAGE 필요 없음
     */
    private fun insertMediaStore(file : File) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                put(MediaStore.Images.Media.IS_PENDING, 1)
//                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val collection = MediaStore.Images.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val item: Uri = contentResolver.insert(collection, values)!!

            contentResolver.openFileDescriptor(item, "w", null).use {
                // write something to OutputStream
                FileOutputStream(it!!.fileDescriptor).use { outputStream ->
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
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

                } else {
                    if (shouldShowRequestPermissionRationale(
                            Manifest.permission.CAMERA)) { // 4
                        requestPermissions(
                            arrayOf(Manifest.permission.CAMERA),
                            0)
                    } else {
                        Log.d("tag", "User declined and i can't ask")
                    }
                }
            }
        }
    }
}