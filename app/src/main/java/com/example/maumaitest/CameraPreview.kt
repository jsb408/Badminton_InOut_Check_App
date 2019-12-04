package com.example.maumaitest

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_camera_preview.*
import java.util.*
import java.util.concurrent.Semaphore

class CameraPreview : AppCompatActivity() {
    private lateinit var mPreview: Preview

    val permissionList = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        checkPermission()
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mPreview = Preview(this, cameraPreview)
    }

    override fun onResume() {
        super.onResume()
        mPreview.onResume()
    }

    override fun onPause() {
        super.onPause()
        mPreview.onPause()
    }

    private fun checkPermission() {
        for (pms: String in permissionList) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, pms)

            if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissionList, 0)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (idx in grantResults.indices) {
            if (grantResults[idx] != PackageManager.PERMISSION_GRANTED) finish()
        }
    }
}

class Preview(context: Context, textureView: TextureView) : Thread() {
    val TAG = "Preview"

    private var mPreviewSize: Size? = null
    var mContext = context
    var mCameraDevice: CameraDevice? = null
    private lateinit var mPreviewBuilder: CaptureRequest.Builder
    lateinit var mPreviewSession: CameraCaptureSession
    private var mTextureView = textureView

    private fun getBackFacingCameraId(cManager: CameraManager): String? {
        try {
            for (cameraId in cManager.cameraIdList) {
                val characteristics = cManager.getCameraCharacteristics(cameraId)
                val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) return cameraId
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }

    fun openCamera() {
        val manager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.e(TAG, "openCamera E")
        try {
            val cameraId = getBackFacingCameraId(manager)!!
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            mPreviewSize = map.getOutputSizes(SurfaceTexture::class.java)[0]
            manager.openCamera(cameraId, mStateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        Log.e(TAG, "openCamera X")
    }

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.e(TAG, "onSurfaceTextureAvailable, width=$width, height=$height")
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, eight: Int) {
            Log.e(TAG, "onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
    }

    private val mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.e(TAG, "onOpened")
            mCameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.e(TAG, "onDisconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "onError")
        }
    }

    protected fun startPreview() {
        if(mCameraDevice == null || !mTextureView.isAvailable || mPreviewSize == null) {
            Log.e(TAG, "startPreview FAIL")
        }

        val texture = mTextureView.surfaceTexture
        if (texture == null) {
            Log.e(TAG, "texture is null")
            return
        }

        texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        val surface = Surface(texture)

        try {
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        mPreviewBuilder.addTarget(surface)

        try {
            mCameraDevice!!.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mPreviewSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(mContext, "onConfigureFailed", Toast.LENGTH_SHORT).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    protected fun updatePreview() {
        if(mCameraDevice == null) {
            Log.e(TAG, "updatePreview error")
        }

        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        val thread = HandlerThread("CameraPreview")
        thread.start()
        val backgroundHandler = Handler(thread.looper)

        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    public fun setSurfaceTextureListener() {
        mTextureView.surfaceTextureListener = mSurfaceTextureListener
    }

    public fun onResume() {
        setSurfaceTextureListener()
    }

    val mCameraOpenCloseLock = Semaphore(1)

    public fun onPause() {
        try {
            mCameraOpenCloseLock.acquire()
            mCameraDevice?.let {
                it.close()
                mCameraDevice = null
                Log.d(TAG, "CameraDevice Close")
            }
        } catch(e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }
}