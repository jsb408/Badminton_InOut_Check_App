package com.example.inoutreader.tflite

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.min
import com.example.inoutreader.tflite.Classifier.Recognition

class TensorFlowImageClassifier private constructor() : Classifier {
    val TAG = "TensorFlowImageClassifier"

    private var interpreter: Interpreter? = null
    private var inputSize = 0
    private var labelList: List<String>? = null

    val outputLocations = Array(BATCH_SIZE) { Array(NUM_DETECTIONS) {FloatArray(4)} }
    val outputClasses = Array(BATCH_SIZE) { FloatArray(NUM_DETECTIONS) }
    val outputScores = Array(BATCH_SIZE) { FloatArray(NUM_DETECTIONS) }
    val numDetections = FloatArray(BATCH_SIZE)

    override fun recognizeImage(bitmap: Bitmap): List<Recognition> {
        val byteBuffer = convertBitmapToByteBuffer(bitmap)
        val inputArray = Array(BATCH_SIZE) { byteBuffer }
        val outputMap = mapOf(
            0 to outputLocations,
            1 to outputClasses,
            2 to outputScores,
            3 to numDetections
        )

        val startTime = SystemClock.uptimeMillis()
        interpreter?.runForMultipleInputsOutputs(inputArray, outputMap)
        val endTime = SystemClock.uptimeMillis()
        val runTime = (endTime - startTime).toString()
        Log.d(TAG, "recognizeImage: " + runTime + "ms")
        return getSortedResult()
    }

    override fun close() {
        interpreter!!.close()
        interpreter = null
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,declaredLength)
    }

    @Throws(IOException::class)
    private fun loadLabelList(assetManager: AssetManager, labelPath: String): List<String> {
        val labelList: MutableList<String> = ArrayList()
        val reader = BufferedReader(InputStreamReader(assetManager.open(labelPath)))
        var line: String?
        do {
            line = reader.readLine()
            line?.let {labelList.add(line)}
        } while (line != null)
        reader.close()
        return labelList
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues,0, bitmap.width,0,0, bitmap.width, bitmap.height)

        byteBuffer.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue = intValues[i * inputSize + j]
                byteBuffer.put((pixelValue shr 16 and 0xFF).toByte())
                byteBuffer.put((pixelValue shr 8 and 0xFF).toByte())
                byteBuffer.put((pixelValue and 0xFF).toByte())
                /*byteBuffer.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)*/
            }
        }
        return byteBuffer
    }

    @SuppressLint("DefaultLocale")
    private fun getSortedResult(): List<Recognition> {
        val pq: PriorityQueue<Recognition> = PriorityQueue(
            MAX_RESULTS,
            Comparator { lhs, rhs ->
                java.lang.Float.compare(rhs.confidence, lhs.confidence)
            })
        for (i in 0 until numDetections[0].toInt()) {
            val detection = RectF(
                outputLocations[0][i][1] * inputSize,
                outputLocations[0][i][0] * inputSize,
                outputLocations[0][i][3] * inputSize,
                outputLocations[0][i][2] * inputSize
            )
            val labelOffset = 0
            Log.d("LABEL", "${outputClasses[0][i] + labelOffset}")
            pq.add(
                Recognition(i.toString(), labelList!![(outputClasses[0][i] + labelOffset).toInt()],
                outputScores[0][i], detection)
            )
        }

        val recognitions: ArrayList<Recognition> = ArrayList()
            //val recognitionsSize = min(pq.size, MAX_RESULTS)
        if (numDetections[0] > 0) recognitions.add(pq.poll()!!)
        return recognitions
    }

    companion object {
        private const val MAX_RESULTS = 5
        private const val BATCH_SIZE = 1
        private const val PIXEL_SIZE = 3
        private const val NUM_DETECTIONS = 10

        private val modelPath = "detect-60000.tflite"
        private val labelPath = "label.txt"

        @Throws(IOException::class)
        fun create(assetManager: AssetManager, inputSize: Int): Classifier {
            val classifier = TensorFlowImageClassifier()
            classifier.interpreter = Interpreter(classifier.loadModelFile(assetManager, modelPath))
                .apply { setNumThreads(8) }
            classifier.labelList = classifier.loadLabelList(assetManager, labelPath)
            classifier.inputSize = inputSize
            return classifier
        }
    }
}