package com.example.shika

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.shika.ui.theme.ShikaTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShikaTheme {
                ImagePickerScreen()
            }
        }
    }
}

@Composable
fun ImagePickerScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var faces by remember { mutableStateOf<List<Rect>>(emptyList()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUri == null) {
            Button(onClick = { launcher.launch("image/*") }) {
                Text(text = "画像を選択")
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale *= zoom
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                launcher.launch("image/*")
                            }
                        )
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    )
            ) {
                imageUri?.let {
                    DisplayImageWithBoundingBox(imageUri = it)
                }
                //                Image(
//                    painter = rememberImagePainter(data = imageUri),
//                    contentDescription = null,
//                    modifier = Modifier.fillMaxSize(),
//                    contentScale = ContentScale.Crop
//                )
//            }
            }
        }
    }
}

@Composable
fun DisplayImageWithBoundingBox(imageUri: Uri) {
    // 顔の位置を保持する状態を定義
    var faces by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val metrics = LocalContext.current.resources.displayMetrics
    val screenWidth = metrics.widthPixels
    val screenHeight = metrics.heightPixels

    // 画像URIが変更されたときに実行される処理
    LaunchedEffect(imageUri) {
        // 画像をデコードしてビットマップに変換
        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
        val decodedBitmap = ImageDecoder.decodeBitmap(source)
        // 画面サイズに合わせてビットマップをスケーリング
        val scaledBitmap = Bitmap.createScaledBitmap(decodedBitmap, screenWidth, (decodedBitmap.height * screenWidth) / decodedBitmap.width, true)
        // 入力画像を作成
        val inputImage = InputImage.fromBitmap(scaledBitmap, 0)
        bitmap = scaledBitmap

        // 顔検出器を設定
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
        // 顔検出を実行
        detector.process(inputImage)
            .addOnSuccessListener { detectedFaces ->
                // 検出された顔の位置を状態に設定
                faces = detectedFaces.map { face ->
                    Rect(
                        face.boundingBox.left.toFloat(),
                        face.boundingBox.top.toFloat(),
                        face.boundingBox.right.toFloat(),
                        face.boundingBox.bottom.toFloat()
                    )
                }
            }
    }

    // 画像と検出された顔の位置を描画
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val imageWidth = constraints.maxWidth.toFloat()
        val imageHeight = constraints.maxHeight.toFloat()

        // 画像を表示
        bitmap?.let {
            val offsetPx = (imageHeight - it.height) / 2
            val offsetDp = with(LocalDensity.current) { offsetPx.toDp() }
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .offset(y = offsetDp),
//                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 顔の位置に赤い枠を描画
            DrawBoundingBoxes(faces = faces, offset = offsetDp)
        }
    }
}


@Composable
fun DrawBoundingBoxes(faces: List<Rect>, offset: Dp) {
    // キャンバスに顔の位置を描画
    Canvas(modifier = Modifier.fillMaxSize()) {
        faces.forEach { face ->
            drawRect(
                color = Color.Red,
                topLeft = androidx.compose.ui.geometry.Offset(face.left, face.top + offset.toPx()),
                size = androidx.compose.ui.geometry.Size(face.right - face.left, face.bottom - face.top),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}