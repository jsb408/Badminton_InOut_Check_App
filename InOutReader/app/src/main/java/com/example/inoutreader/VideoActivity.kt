package com.example.inoutreader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.example.inoutreader.customview.OverlayView
import com.example.inoutreader.tflite.Classifier
import com.example.inoutreader.tflite.TensorFlowImageClassifier
import com.example.inoutreader.tracking.MultiBoxTracker
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.fragment_video_tracking.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class VideoActivity : AppCompatActivity() {
    val TAG = "VideoActivity"

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val mediaMetadataRetriever = MediaMetadataRetriever()

    private val INPUT_SIZE = 300
    private var timestamp = 0L
    private var target: Classifier.Recognition? = null

    private lateinit var tflite: Classifier
    private lateinit var tracker: MultiBoxTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        supportActionBar?.hide()
        supportFragmentManager.beginTransaction().replace(R.id.container, VideoTrackingFragment(), "videoFragment").commit()

        selectVideoView.setVideoURI(intent.data)
        selectVideoView.start()
        selectVideoView.setOnPreparedListener { mp ->
            mp.setOnVideoSizeChangedListener { _, _, _ ->
                val mediaController = object: MediaController(this) {
                    override fun show(timeout: Int) {
                        super.show(0)
                    }
                }
                selectVideoView.setMediaController(mediaController)
                mediaController.setAnchorView(videoBackground)
            }
        }

        tflite = TensorFlowImageClassifier.create(assets, INPUT_SIZE)
        tracker = MultiBoxTracker(this)

        handlerThread = HandlerThread("inference")
        handlerThread?.let {
            it.start()
            handler = Handler(it.looper)
        }
    }

    override fun onStart() {
        super.onStart()

        mediaMetadataRetriever.setDataSource(this, intent.data)

        val bitmap = mediaMetadataRetriever.getFrameAtTime(0)
        tracker.setFrameConfiguration(bitmap.width, bitmap.height, INPUT_SIZE, 0)

        tracking_overlay.addCallback(
            object: OverlayView.DrawCallback {
                override fun drawCallback(canvas: Canvas) {
                    tracker.draw(canvas)
                }
            }
        )

        recognizeImage()
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

    private fun recognizeImage() {
        runInBackground(Runnable {
            do {
                val frame = (selectVideoView.currentPosition).toLong()
                Log.d(TAG, "FRAME IN $frame")

                var bitmap = mediaMetadataRetriever.getFrameAtTime(frame * 1000)
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)

                val results = tflite.recognizeImage(bitmap)
                Log.d(TAG, results.joinToString())

                if (results.size > 0) {
                    val mappedRecognition = mutableListOf<Classifier.Recognition>()

                    for (result in results) {
                        val location = result.location
                        location?.let {
                            if (result.title == "people") {
                                target?.let {
                                    if (it.confidence < result.confidence) target = result
                                } ?: result
                            }
                            result.location = location
                            mappedRecognition.add(result)
                        }
                    }

                    tracker.trackResults(mappedRecognition, ++timestamp)
                    tracking_overlay.postInvalidate()
                }
            } while (true)
        })
    }
}
