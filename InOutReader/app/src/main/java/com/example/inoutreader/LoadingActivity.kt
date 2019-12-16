package com.example.inoutreader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.LongSparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.set
import com.example.inoutreader.tflite.Classifier
import com.example.inoutreader.tflite.TensorFlowImageClassifier
import com.example.inoutreader.tracking.MultiBoxTracker
import kotlinx.android.synthetic.main.activity_loading.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class LoadingActivity : AppCompatActivity() {
    companion object {
        val CUT_FRAME = 5000
        val trackedBox = LongSparseArray<RectF>()
        val staticResult = LongSparseArray<List<Classifier.Recognition>>()
    }

    private val TAG = "LoadingActivity"
    private val INPUT_SIZE = 300

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var frame = 0L

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
            val tracker = MultiBoxTracker(this)

            while (frame < duration) {
                loadingBar.progress = frame.toInt()
                Log.d(TAG, "FRAME IN ${frame}ms")

                var bitmap = mediaMetadataRetriever.getFrameAtTime(frame * 1000)
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)

                val mappedRecognition = mutableListOf<Classifier.Recognition>()
                val results = tflite.recognizeImage(bitmap)

                for (result in results) {
                    val location = result.location
                    location?.let {
                        result.location = it
                        mappedRecognition.add(result)
                    }
                }

                staticResult[frame] = mappedRecognition
                //tracker.trackResults(mappedRecognition, frame)
                //trackedBox[frame] = tracker.screenRects[0].second
                frame += CUT_FRAME
            }

            mediaMetadataRetriever.release()

            val intent = Intent(this, VideoActivity::class.java)
                .apply { data = intent.data }
            startActivity(intent)
            finish()
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