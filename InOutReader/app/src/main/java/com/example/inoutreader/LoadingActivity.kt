package com.example.inoutreader

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.example.inoutreader.tflite.Classifier
import com.example.inoutreader.tflite.TensorFlowImageClassifier
import com.example.inoutreader.tracking.MultiBoxTracker
import kotlinx.android.synthetic.main.activity_loading.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class LoadingActivity : AppCompatActivity() {
    private val TAG = "LoadingActivity"
    private val INPUT_SIZE = 300

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val results: HashMap<Int, List<Classifier.Recognition>> = hashMapOf()
    private var frame = 0

    private lateinit var tflite: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val mediaPlayer = MediaPlayer.create(this, intent.data)
        val duration = mediaPlayer.duration
        loadingBar.max = duration

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(this, intent.data)

        tflite = TensorFlowImageClassifier.create(assets, INPUT_SIZE)

        handlerThread = HandlerThread("inference")
        handlerThread?.let {
            it.start()
            handler = Handler(it.looper)
        }

        runInBackground( Runnable {
            while (frame < duration) {
                loadingBar.progress = frame
                Log.d(TAG, "FRAME IN ${frame}ms")

                var bitmap = mediaMetadataRetriever.getFrameAtTime(frame * 1000L)
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
                results[frame] = tflite.recognizeImage(bitmap)
                frame += 5000
            }

            mediaMetadataRetriever.release()
            Log.d(TAG, "$results")
        })
    }

    override fun onResume() {
        super.onResume()

        handlerThread = HandlerThread("inference")
        handlerThread?.let {
            it.start()
            handler = Handler(it.looper)
        }
    }

    override fun onPause() {
        handlerThread?.quitSafely()
        try {
            handlerThread?.join()
            handlerThread = null
            handler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.execute { tflite.close() }
    }

    private fun runInBackground(r: Runnable) {
        handler?.post(r)
    }
}