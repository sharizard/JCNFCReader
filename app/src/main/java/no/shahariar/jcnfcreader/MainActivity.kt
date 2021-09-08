package no.shahariar.jcnfcreader

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import no.shahariar.jcnfcreader.ui.theme.JCNFCReaderTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JCNFCReaderTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    TextRecognizer()
                }
            }
        }
    }
}

@Preview
@Composable
fun ComposablePreview() {
    TextRecognizer()
}

@Composable
fun TextRecognizer() {
    val extractedText = remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CameraPreview(extractedText)
        Box(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(40.dp)
            .background(color = Color.White)
            .shadow(
                4.dp,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                clip = true
            )) {
                Text(
                    text = extractedText.value, color = Color.Black)
            }
    }
}

@Composable
fun CameraPreview(extractedText: MutableState<String>) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val previewView = remember {
        PreviewView(context).apply { 
            id = R.id.camera_preview_view
        }
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    print("Nå skal detter rendres")
    
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val textAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, TextAnalyzer(textRecognizer, extractedText))
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, textAnalysis)
            } catch (exc: Exception) {
                Log.e("Exc", "CameraX ${exc.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

private class TextAnalyzer(
    private val textRecognizer: TextRecognizer,
    private val extractedText: MutableState<String>
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        Log.e("MNediaImg", "Hentet img")


        mediaImage?.let { r ->
            Log.e("MediaImgNotNull", "Ikke null")
            val image = InputImage.fromMediaImage(r, imageProxy.imageInfo.rotationDegrees)

            textRecognizer.process(image)
                .addOnCompleteListener {
                    Log.e("Processerer", "Prøver å prosessere")
                    Log.e("Processerer", it.isSuccessful.toString())


                    if (it.isSuccessful) {
                        Log.e("Processerer", "Suksess")

                        extractedText.value = it.result.text
                        Log.e("TextResult", it.result.text)
                    }

                    imageProxy.close()
                }
                .addOnFailureListener {e ->                 Log.e("Exc", "TextRecognozier ${e.localizedMessage}")
                }.addOnCompleteListener {
                    mediaImage.close()
                    imageProxy.close()
                }
        }

    }

}

