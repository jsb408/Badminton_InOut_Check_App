package com.example.inoutreader

import android.graphics.*
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.LongSparseArray
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.set
import com.example.inoutreader.customview.OverlayView
import com.example.inoutreader.tflite.Classifier
import com.example.inoutreader.tflite.TensorFlowImageClassifier
import com.example.inoutreader.tracking.MultiBoxTracker
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.fragment_video_tracking.*
import java.lang.Exception
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

    private val courtRectF = RectF(35f, 40f, 250f, 250f)

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
            val paint = Paint().apply {
                color = Color.RED
                strokeWidth = 2.0f
            }
            Log.d(TAG, LoadingActivity.trackedBox.toString())

            val frame = (selectVideoView.currentPosition).toLong()
            val convertedFrame = ((frame / LoadingActivity.CUT_FRAME) + if (frame % LoadingActivity.CUT_FRAME > LoadingActivity.CUT_FRAME * 0.7) 0 else 0) * LoadingActivity.CUT_FRAME

            if (!LoadingActivity.staticResult[convertedFrame].isNullOrEmpty()) {
                val result = LoadingActivity.staticResult[convertedFrame][0]
                runOnUiThread {
                    if (result.center.second > courtRectF.bottom
                        || result.center.second < courtRectF.top
                        || result.center.first > courtRectF.right
                        || result.center.first < courtRectF.left
                    ) {
                        changeJudge(false)
                    } else changeJudge(true)
                }
            }
            tracker.trackResults(LoadingActivity.staticResult[convertedFrame], convertedFrame)
            tracking_overlay.postInvalidate()

            Log.d(TAG, "FRAME IN $frame")
            /*var bitmap = mediaMetadataRetriever.getFrameAtTime(52800 * 1000)
            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
            val canvas = Canvas(bitmap)

            val mappedRecognition = mutableListOf<Classifier.Recognition>()
            val results = tflite.recognizeImage(bitmap)

            for (result in results) {
                val location = result.location
                location?.let {
                    result.location = it
                    canvas.drawRect(it, paint)
                    mappedRecognition.add(result)
                }
            }

            runOnUiThread {
                imageView4.setImageBitmap(bitmap)
            }*/

            recognizeImage()
                /*val transparent = Bitmap.createBitmap(tmpImg.width, tmpImg.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(transparent)
                Log.d(TAG, "FRAME IN $frame, CONVERTED : $convertedFrame")
                canvas.drawRect(LoadingActivity.trackedBox[convertedFrame], paint)
                Log.d(TAG, LoadingActivity.trackedBox[convertedFrame].toString())
                runOnUiThread {
                    imageView4.setImageBitmap(transparent)
                }*/

                /*if (trackedBox[frame] == null) {
                    var bitmap = mediaMetadataRetriever.getFrameAtTime(frame * 1000)
                    bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)

                    val results = tflite.recognizeImage(bitmap)
                    Log.d(TAG, results.joinToString())

                    if (results.size > 0) {
                        val mappedRecognition = mutableListOf<Classifier.Recognition>()

                        for (result in results) {
                            val location = result.location
                            location?.let {
                                result.location = location
                                mappedRecognition.add(result)

                                runOnUiThread {
                                    if (result.center.second > RectF.bottom
                                    || result.center.second < RectF.top
                                    || result.center.first > RectF.right
                                    || result.center.first < RectF.left) {
                                        changeJudge(false)
                                    } else changeJudge(true)
                                }
                            }
                        }

                        tracker.trackResults(mappedRecognition, ++timestamp)
                        trackedBox[frame] = tracker.screenRects[0].second
                        tracking_overlay.postInvalidate()
                    }
                } else {
                    val paint = Paint().apply {
                        color = Color.RED
                        strokeWidth = 2.0f
                    }
                    val canvas = Canvas()

                    canvas.drawRect(trackedBox[frame], paint)
                }*/
        })
    }

    private fun changeJudge(isIn: Boolean) {
        if (isIn) {
            resultLayout.setBackgroundColor(resources.getColor(R.color.colorIn))
            resultText.text = "IN"
        } else {
            resultLayout.setBackgroundColor(resources.getColor(R.color.colorOut))
            resultText.text = "OUT"
        }
    }
}
