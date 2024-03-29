package data

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

class ModelManager(private val context: Context) {
    private val MODEL_FILE = "siamese_model_quantized.tflite" // Replace with your model filename
    @Throws(IOException::class)
    private fun loadModelFile(): Interpreter {
        val model = FileUtil.loadMappedFile(context, MODEL_FILE)
        return Interpreter(model)
    }

    fun runInference(input1: FloatArray, input2: FloatArray): String{
        // Load the model file
        val interpreter = loadModelFile()

        // Allocate tensors for the inputs and outputs
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()

        val inputBuffer1 = TensorBuffer.createFixedSize(inputShape, interpreter.getInputTensor(0).dataType())
        val inputBuffer2 = TensorBuffer.createFixedSize(inputShape, interpreter.getInputTensor(1).dataType())
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, interpreter.getOutputTensor(0).dataType())

        return ""
    }

    fun prepareInputsForInference(map: MutableMap<Long, FloatArray>): FloatArray {
        // Get the size of the target array based on the first element's size
        val firstArray = map.values.first()
        val elementSize = firstArray.size

        // Create the target array
        val targetArray = FloatArray(1 * 200 * 6)

        // Iterate over the map and copy elements
        var index = 0
        map.forEach { (_, value) ->
            System.arraycopy(value, 0, targetArray, index, elementSize)
            index += elementSize
        }

        println("Target array size: ${targetArray.size}")

        return targetArray
    }
    fun readOutputsFromInference(): Any {
        // Prepare outputs for inference
        return Any()
    }
}