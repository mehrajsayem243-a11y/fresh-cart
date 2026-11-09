package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.barcode.BarcodeProductService
import com.example.data.barcode.QrBarcodeMatrixGenerator
import com.example.data.barcode.ScannedProduct
import com.example.ui.theme.CardShape
import com.example.ui.theme.DialogShape
import com.example.ui.theme.PillShape
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class ScanTargetDestination {
    GROCERIES,
    PANTRY
}

enum class ScannerStudioMode(val title: String) {
    BARCODE_SCAN("Scan Barcode"),
    QR_SCAN("Scan QR Code"),
    GENERATE_QR("Generate QR")
}

@OptIn(ExperimentalGetImage::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onAddGrocery: (name: String, quantity: Double, unit: String, category: String, notes: String) -> Unit,
    onAddPantry: (name: String, quantity: Double, unit: String, category: String, days: Int, location: String) -> Unit,
    onImportChecklist: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val barcodeService = remember { BarcodeProductService() }

    var studioMode by remember { mutableStateOf(ScannerStudioMode.BARCODE_SCAN) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var scannedProduct by remember { mutableStateOf<ScannedProduct?>(null) }
    var isLookingUp by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf(ScanTargetDestination.GROCERIES) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControlRef by remember { mutableStateOf<Camera?>(null) }

    // QR scan state
    var qrDecodedContent by remember { mutableStateOf<String?>(null) }
    var qrConvertedBarcode by remember { mutableStateOf<String?>(null) }

    // Generator state
    var generatorText by remember { mutableStateOf("073762806450") }
    var generatorType by remember { mutableStateOf("QR") } // "QR" or "BARCODE"
    var qrGeneratedFromBarcode by remember { mutableStateOf("") }

    fun handleBarcodeDetected(rawCode: String) {
        if (isLookingUp || scannedProduct != null) return
        isLookingUp = true
        coroutineScope.launch {
            val product = barcodeService.lookupBarcode(rawCode)
            scannedProduct = product
            isLookingUp = false
        }
    }

    fun handleQrDetected(rawContent: String) {
        qrDecodedContent = rawContent
        // Check if QR content looks like a barcode (pure digits or contains digits)
        val extractedDigits = rawContent.filter { it.isDigit() }
        if (extractedDigits.length in 8..14) {
            qrConvertedBarcode = extractedDigits
            // Automatic QR to Barcode conversion lookup!
            coroutineScope.launch {
                val product = barcodeService.lookupBarcode(extractedDigits)
                scannedProduct = product
            }
        } else {
            qrConvertedBarcode = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (studioMode) {
                                        ScannerStudioMode.BARCODE_SCAN -> Icons.Rounded.QrCodeScanner
                                        ScannerStudioMode.QR_SCAN -> Icons.Rounded.QrCode2
                                        ScannerStudioMode.GENERATE_QR -> Icons.Rounded.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "QR & Barcode Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scan, convert & generate instantly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3-Way Segmented Studio Selector
                TabRow(
                    selectedTabIndex = studioMode.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[studioMode.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                ) {
                    ScannerStudioMode.entries.forEach { mode ->
                        Tab(
                            selected = studioMode == mode,
                            onClick = { studioMode = mode },
                            text = {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (studioMode == mode) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("scanner_tab_${mode.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (studioMode) {
                    ScannerStudioMode.BARCODE_SCAN -> {
                        // --- 1D BARCODE SCANNER ---
                        BarcodeScannerViewfinderSection(
                            hasCameraPermission = hasCameraPermission,
                            torchEnabled = torchEnabled,
                            isLookingUp = isLookingUp,
                            onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onToggleTorch = {
                                torchEnabled = !torchEnabled
                                cameraControlRef?.cameraControl?.enableTorch(torchEnabled)
                            },
                            onBarcodeScanned = { raw -> handleBarcodeDetected(raw) },
                            onBindCamera = { cam -> cameraControlRef = cam },
                            lifecycleOwner = lifecycleOwner,
                            isBarcodeMode = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Product Card if identified
                        val product = scannedProduct
                        if (product != null) {
                            ProductIdentifiedCard(
                                product = product,
                                destination = destination,
                                onDestinationChange = { destination = it },
                                onDismiss = { scannedProduct = null },
                                onAdd = {
                                    if (destination == ScanTargetDestination.GROCERIES) {
                                        onAddGrocery(
                                            product.name,
                                            product.quantity,
                                            product.unit,
                                            product.category,
                                            "Scanned Barcode #${product.barcode}"
                                        )
                                    } else {
                                        onAddPantry(
                                            product.name,
                                            product.quantity,
                                            product.unit,
                                            product.category,
                                            product.shelfLifeDays,
                                            product.storageLocation
                                        )
                                    }
                                    onDismiss()
                                },
                                onConvertToQr = {
                                    // Barcode to QR feature: set generator text and switch tab!
                                    generatorText = product.barcode
                                    generatorType = "QR"
                                    qrGeneratedFromBarcode = "Product: ${product.name} (UPC: ${product.barcode})"
                                    studioMode = ScannerStudioMode.GENERATE_QR
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick demo barcode test chips
                        Text(
                            text = "Tap to simulate retail barcode scan:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            barcodeService.sampleDemoBarcodes.take(6).forEach { demoItem ->
                                Surface(
                                    shape = PillShape,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.testTag("demo_barcode_${demoItem.barcode}")
                                ) {
                                    TextButton(
                                        onClick = { handleBarcodeDetected(demoItem.barcode) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(text = demoItem.name, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Manual Barcode Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualBarcodeInput,
                                onValueChange = { manualBarcodeInput = it },
                                placeholder = { Text("Or enter barcode digits...") },
                                singleLine = true,
                                shape = PillShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_barcode_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            PillButton(
                                text = "Look Up",
                                onClick = {
                                    if (manualBarcodeInput.isNotBlank()) {
                                        handleBarcodeDetected(manualBarcodeInput)
                                    }
                                },
                                testTag = "manual_barcode_lookup_button"
                            )
                        }
                    }

                    ScannerStudioMode.QR_SCAN -> {
                        // --- 2D QR CODE SCANNER ---
                        BarcodeScannerViewfinderSection(
                            hasCameraPermission = hasCameraPermission,
                            torchEnabled = torchEnabled,
                            isLookingUp = isLookingUp,
                            onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onToggleTorch = {
                                torchEnabled = !torchEnabled
                                cameraControlRef?.cameraControl?.enableTorch(torchEnabled)
                            },
                            onBarcodeScanned = { raw -> handleQrDetected(raw) },
                            onBindCamera = { cam -> cameraControlRef = cam },
                            lifecycleOwner = lifecycleOwner,
                            isBarcodeMode = false
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Decoded QR Results
                        if (qrDecodedContent != null) {
                            Surface(
                                shape = CardShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (qrConvertedBarcode != null) "QR to Barcode Converted!" else "QR Code Decoded",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = {
                                            qrDecodedContent = null
                                            qrConvertedBarcode = null
                                            scannedProduct = null
                                        }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = qrDecodedContent.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 4
                                    )

                                    if (qrConvertedBarcode != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Extracted Barcode: #${qrConvertedBarcode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        if (qrConvertedBarcode != null && scannedProduct != null) {
                                            PillButton(
                                                text = "Add Product to List",
                                                onClick = {
                                                    val prod = scannedProduct!!
                                                    onAddGrocery(
                                                        prod.name,
                                                        prod.quantity,
                                                        prod.unit,
                                                        prod.category,
                                                        "Imported via QR to Barcode #${prod.barcode}"
                                                    )
                                                    onDismiss()
                                                }
                                            )
                                        } else {
                                            PillButton(
                                                text = "Import as Checklist",
                                                onClick = {
                                                    qrDecodedContent?.let { content ->
                                                        onImportChecklist?.invoke(content)
                                                            ?: onAddGrocery(content.lines().firstOrNull() ?: content, 1.0, "pcs", "Other", "Imported from QR")
                                                    }
                                                    onDismiss()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // QR Demo Chips
                        Text(
                            text = "Test QR decoding (Simulate Scan):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                TextButton(onClick = { handleQrDetected("BARCODE:011110038493 (Organic Whole Milk)") }) {
                                    Text("QR: Milk Barcode", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                TextButton(onClick = { handleQrDetected("• Honeycrisp Apples\n• Greek Yogurt\n• Whole Wheat Bread") }) {
                                    Text("QR: 3-Item List", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                TextButton(onClick = { handleQrDetected("073762806450") }) {
                                    Text("QR: Pasta Barcode", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    ScannerStudioMode.GENERATE_QR -> {
                        // --- GENERATE QR & BARCODE TO QR CONVERTER ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FilterChip(
                                    selected = generatorType == "QR",
                                    onClick = { generatorType = "QR" },
                                    label = { Text("QR Code") },
                                    leadingIcon = { Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    shape = PillShape
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = generatorType == "BARCODE",
                                    onClick = { generatorType = "BARCODE" },
                                    label = { Text("1D Barcode") },
                                    leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    shape = PillShape
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Matrix Display Box
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                tonalElevation = 6.dp,
                                modifier = Modifier
                                    .size(if (generatorType == "QR") 230.dp else 260.dp, if (generatorType == "QR") 230.dp else 120.dp)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bitmap = if (generatorType == "QR") {
                                        QrBarcodeMatrixGenerator.generateQrCodeBitmap(generatorText.ifBlank { "FreshCart" }, 512)
                                    } else {
                                        QrBarcodeMatrixGenerator.generate1DBarcodeBitmap(generatorText.filter { it.isLetterOrDigit() }.ifBlank { "073762806450" }, 600, 200)
                                    }

                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Generated Code",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = "Enter content to generate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Code Content: $generatorText",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (qrGeneratedFromBarcode.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = qrGeneratedFromBarcode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = generatorText,
                                onValueChange = { generatorText = it },
                                label = { Text("Enter Barcode Digits or Item Text") },
                                singleLine = true,
                                shape = PillShape,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PillButton(
                                    text = "Preset: Pasta Barcode",
                                    onClick = { generatorText = "073762806450" }
                                )
                                PillButton(
                                    text = "Preset: Milk Barcode",
                                    onClick = { generatorText = "011110038493" }
                                )
                                PillButton(
                                    text = "Test Scan This Code",
                                    onClick = {
                                        // Feeds this code directly into the scanner handler!
                                        if (generatorType == "QR") {
                                            handleQrDetected(generatorText)
                                            studioMode = ScannerStudioMode.QR_SCAN
                                        } else {
                                            handleBarcodeDetected(generatorText)
                                            studioMode = ScannerStudioMode.BARCODE_SCAN
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun BarcodeScannerViewfinderSection(
    hasCameraPermission: Boolean,
    torchEnabled: Boolean,
    isLookingUp: Boolean,
    onGrantPermission: () -> Unit,
    onToggleTorch: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onBindCamera: (Camera) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    isBarcodeMode: Boolean
) {
    if (!hasCameraPermission) {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "To scan product barcodes and QR codes live with your camera",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                PillButton(
                    text = "Grant Camera Access",
                    onClick = onGrantPermission,
                    icon = Icons.Rounded.CameraAlt,
                    testTag = "grant_camera_permission_button"
                )
            }
        }
    } else {
        // Huge CameraX Viewfinder with Expanded Animated Reticle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .clip(CardShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val scanner = BarcodeScanning.getClient()
                        var isBusy = false

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isBusy) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                isBusy = true
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val raw = barcodes.firstOrNull()?.rawValue
                                        if (!raw.isNullOrBlank()) {
                                            onBarcodeScanned(raw)
                                        }
                                    }
                                    .addOnCompleteListener {
                                        isBusy = false
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            onBindCamera(camera)
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Reticle: Huge wide 1D barcode reticle (310x180) or Huge Square 2D QR reticle (270x270)
            Box(
                modifier = Modifier
                    .size(if (isBarcodeMode) 310.dp else 270.dp, if (isBarcodeMode) 180.dp else 270.dp)
                    .border(
                        3.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        RoundedCornerShape(20.dp)
                    )
            )

            // Animated Sweeping Laser Line across the large frame
            val infiniteTransition = rememberInfiniteTransition(label = "laser")
            val laserOffset by infiniteTransition.animateFloat(
                initialValue = if (isBarcodeMode) -70f else -105f,
                targetValue = if (isBarcodeMode) 70f else 105f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laserOffset"
            )

            Box(
                modifier = Modifier
                    .width(if (isBarcodeMode) 290.dp else 250.dp)
                    .height(3.dp)
                    .offset(y = laserOffset.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Mode indicator tag at top start
            Surface(
                shape = PillShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isBarcodeMode) Icons.Rounded.QrCodeScanner else Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBarcodeMode) "1D Barcode" else "2D QR Code",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Circular Flashlight Toggle
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(46.dp)
            ) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = "Torch",
                        tint = if (torchEnabled) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Scan Alignment Guidance Badge at bottom
            Surface(
                shape = PillShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = if (isBarcodeMode) "Align barcode inside the frame" else "Align QR code inside the frame",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isLookingUp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ProductIdentifiedCard(
    product: ScannedProduct,
    destination: ScanTargetDestination,
    onDestinationChange: (ScanTargetDestination) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onConvertToQr: () -> Unit
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Product Identified!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onConvertToQr) {
                    Icon(Icons.Rounded.Transform, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Barcode → QR", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Category: ${product.category} • Default: ${product.quantity} ${product.unit} • Code: #${product.barcode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Destination toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = destination == ScanTargetDestination.GROCERIES,
                    onClick = { onDestinationChange(ScanTargetDestination.GROCERIES) },
                    label = { Text("Grocery List") },
                    leadingIcon = {
                        Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = PillShape
                )

                FilterChip(
                    selected = destination == ScanTargetDestination.PANTRY,
                    onClick = { onDestinationChange(ScanTargetDestination.PANTRY) },
                    label = { Text("Pantry Stock") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Kitchen, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = PillShape
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Clear")
                }
                Spacer(modifier = Modifier.width(8.dp))
                PillButton(
                    text = if (destination == ScanTargetDestination.GROCERIES) "Add to Cart" else "Add to Pantry",
                    onClick = onAdd,
                    testTag = "confirm_add_scanned_product"
                )
            }
        }
    }
}
