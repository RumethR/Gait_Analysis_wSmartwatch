package data

import android.content.Context
import android.util.Log
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
        val inputShape = interpreter.getInputTensor(0).shape() //[1,200,6], FLOAT32
        val outputShape = interpreter.getOutputTensor(0).shape() //[1,1]

        val inputBuffer1 = TensorBuffer.createFixedSize(inputShape, interpreter.getInputTensor(0).dataType())
        val inputBuffer2 = TensorBuffer.createFixedSize(inputShape, interpreter.getInputTensor(1).dataType())
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, interpreter.getOutputTensor(0).dataType())

        inputBuffer1.loadArray(input1)
        inputBuffer2.loadArray(input2)

        // Run inference for both inputs
        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer1.buffer, inputBuffer2.buffer), mapOf(0 to outputBuffer.buffer))

        // Get the output
        val output = outputBuffer.getFloatValue(0)

        //Release the resources of the interpreter
        interpreter.close()

        return readOutputsFromInference(output)
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
    private fun readOutputsFromInference(outputResult: Float): String {
        // Read the output from the inference
        Log.d("ModelManager", "Similarity Output: $outputResult")
        return ""
    }
}