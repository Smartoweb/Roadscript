package com.roadscript.oss.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.roadscript.oss.R
import com.roadscript.oss.data.models.Attachment
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.DocumentThumbnail
import com.roadscript.oss.ui.components.PhotoSourceDialog
import com.roadscript.oss.ui.components.formatDateString
import com.roadscript.oss.ui.utils.FileHelper
import com.roadscript.oss.ui.utils.CountryHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val vehicleState by viewModel.vehicle.collectAsState()
    val activeVehicleId by viewModel.activeVehicleId.collectAsState()

    // États du formulaire
    var model by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var plateNumber by rememberSaveable { mutableStateOf("") }
    var firstRegistrationDate by rememberSaveable { mutableStateOf("") }
    var vin by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var fiscalPower by rememberSaveable { mutableStateOf("") }
    var fuelType by rememberSaveable { mutableStateOf("") }
    var themeKey by rememberSaveable { mutableStateOf("INDIGO") }
    var photoPath by rememberSaveable { mutableStateOf("") }
    var acquisitionDate by rememberSaveable { mutableStateOf("") }
    var acquisitionMileage by rememberSaveable { mutableStateOf("") }
    var countryCode by rememberSaveable { mutableStateOf(CountryHelper.getDefaultCountryCode()) }
    var photosList by rememberSaveable { mutableStateOf<List<Attachment>>(emptyList()) }
    
    var showCountryDialog by rememberSaveable { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<Attachment?>(null) }

    // Charger les valeurs existantes si présentes
    LaunchedEffect(vehicleState) {
        if (vehicleState != null && model.isEmpty()) {
            vehicleState?.let { veh ->
                model = veh.model
                brand = veh.brand
                plateNumber = veh.plateNumber
                firstRegistrationDate = veh.firstRegistrationDate
                vin = veh.vin
                type = veh.type
                fiscalPower = veh.fiscalPower?.toString() ?: ""
                fuelType = veh.fuelType
                themeKey = veh.themeKey
                photoPath = veh.photoPath
                acquisitionDate = veh.acquisitionDate
                acquisitionMileage = veh.acquisitionMileage.toString()
                countryCode = veh.countryCode
                photosList = veh.photos
            }
        }
    }

    // Liste des pays (Identique à SettingsScreen)
    val countryCodes = listOf(
        "DZ", "DE", "AR", "AU", "AT", "BE", "BR", "BG", "CA", "CL",
        "CN", "CO", "KR", "CI", "HR", "DK", "EG", "AE", "ES", "EE",
        "US", "FI", "FR", "GR", "HU", "IN", "ID", "IE", "IS", "IL",
        "IT", "JP", "LV", "LT", "LU", "MA", "MX", "MC", "NO", "NZ",
        "NL", "PE", "PL", "PT", "QA", "CZ", "RO", "GB", "RU", "SA",
        "SN", "SK", "SI", "SE", "CH", "TH", "TN", "TR", "UA", "VN"
    )
    val currentLocale = LocalConfiguration.current.locales[0]
    val countryOptions = remember(currentLocale) {
        countryCodes.map { code ->
            code to java.util.Locale("", code).getDisplayCountry(currentLocale)
        }.sortedBy { it.second }
    }

    // Gérer l'emplacement de photo temporaire pour l'appareil photo
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var showPhotoSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showGallerySourceDialog by rememberSaveable { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoFile != null) {
            val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
            val destFile = File(vehicleFolder, "vehicle_profile_${System.currentTimeMillis()}.jpg")
            tempPhotoFile!!.renameTo(destFile)
            photoPath = FileHelper.toRelativePath(context, destFile.absolutePath)
        }
    }

    val cameraGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoFile != null) {
            if (photosList.size < 8) {
                val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
                val destFile = File(vehicleFolder, "vehicle_gallery_${System.currentTimeMillis()}.jpg")
                tempPhotoFile!!.renameTo(destFile)
                photosList = photosList + Attachment(FileHelper.toRelativePath(context, destFile.absolutePath), destFile.name)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Pour simplifier, on ne relance pas automatiquement ici, l'utilisateur recliquera
            Toast.makeText(context, "Permission accordée. Re-cliquez pour ouvrir l'appareil photo.", Toast.LENGTH_SHORT).show()
        }
    }

    val singleGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
            val localPath = FileHelper.copyUriToInternal(context, uri, vehicleFolder)
            if (localPath != null) {
                photoPath = localPath
            }
        }
    }

    val multiGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
            uris.forEach { uri ->
                if (photosList.size < 8) {
                    val localPath = FileHelper.copyUriToInternal(context, uri, vehicleFolder)
                    if (localPath != null) {
                        val originalName = FileHelper.getFileNameFromUri(context, uri)
                        photosList = photosList + Attachment(localPath, originalName)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.veh_profile),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveVehicle(
                                model = model.trim(),
                                brand = brand.trim(),
                                plateNumber = plateNumber.trim().uppercase(),
                                firstRegistrationDate = firstRegistrationDate,
                                vin = vin.trim().uppercase(),
                                type = type.trim().uppercase(),
                                fiscalPower = fiscalPower.toIntOrNull(),
                                fuelType = fuelType,
                                themeKey = themeKey,
                                photoPath = photoPath,
                                acquisitionDate = acquisitionDate,
                                acquisitionMileage = acquisitionMileage.toIntOrNull() ?: 0,
                                countryCode = countryCode,
                                photos = photosList,
                                onSuccess = {
                                    Toast.makeText(context, "Véhicule enregistré avec succès !", Toast.LENGTH_SHORT).show()
                                    onSaveSuccess()
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Erreur : $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = countryCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.btn_save_info), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // En-tête adaptatif (Photo + Thème)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isVertical = this.maxWidth < 400.dp
                
                if (isVertical) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfilePhotoBox(photoPath) { showPhotoSourceDialog = true }
                        ThemeSelector(themeKey) { themeKey = it }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ProfilePhotoBox(photoPath) { showPhotoSourceDialog = true }
                        ThemeSelector(themeKey, modifier = Modifier.weight(1f)) { themeKey = it }
                    }
                }
            }

            Text(stringResource(R.string.veh_admin_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

            // Sélecteur de Pays pour les juridictions
            val currentCountryName = countryOptions.find { it.first == countryCode }?.second ?: ""
            OutlinedTextField(
                value = currentCountryName,
                onValueChange = {},
                label = { Text("${stringResource(R.string.settings_country)} *") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { showCountryDialog = true },
                enabled = false, // On utilise clickable sur la Box ou le modifier
                trailingIcon = { IconButton(onClick = { showCountryDialog = true }) { Icon(Icons.Default.Public, null) } },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text(stringResource(R.string.veh_model)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text(stringResource(R.string.veh_brand)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = plateNumber, 
                onValueChange = { input -> 
                    // Autoriser alphanumérique, tirets et espaces pour l'international
                    plateNumber = input.uppercase().filter { it.isLetterOrDigit() || it == '-' || it == ' ' } 
                }, 
                label = { Text(stringResource(R.string.veh_plate)) }, 
                singleLine = true, 
                modifier = Modifier.fillMaxWidth()
            )

            // Sélecteur de date pour la première immatriculation
            OutlinedTextField(
                value = if (firstRegistrationDate.isNotEmpty()) formatDateString(firstRegistrationDate) else "", onValueChange = {},
                label = { Text(stringResource(R.string.veh_first_reg)) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try { if (firstRegistrationDate.isNotEmpty()) calendar.time = sdf.parse(firstRegistrationDate)!! } catch (e: Exception) {}
                        DatePickerDialog(context, { _, y, m, d ->
                            val cal = Calendar.getInstance(); cal.set(y, m, d)
                            firstRegistrationDate = sdf.format(cal.time)
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.DateRange, null) }
                }
            )

            OutlinedTextField(value = vin, onValueChange = { vin = it.uppercase() }, label = { Text(stringResource(R.string.veh_vin)) }, singleLine = true, modifier = Modifier.fillMaxWidth())

            // Genre et Puissance
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = type, onValueChange = { type = it.uppercase() }, label = { Text(stringResource(R.string.veh_type)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = fiscalPower, onValueChange = { if (it.all { c -> c.isDigit() }) fiscalPower = it }, label = { Text(stringResource(R.string.veh_power)) }, modifier = Modifier.weight(1f))
            }

            // Type de carburant
            var fuelExpanded by rememberSaveable { mutableStateOf(false) }
            val fuelOptions = listOf(
                stringResource(R.string.fuel_gasoline),
                stringResource(R.string.fuel_diesel),
                stringResource(R.string.fuel_electric),
                stringResource(R.string.fuel_hybrid),
                stringResource(R.string.fuel_lpg),
                stringResource(R.string.fuel_other)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = fuelType, onValueChange = { fuelType = it }, label = { Text(stringResource(R.string.veh_fuel_type)) }, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { fuelExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } })
                DropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    fuelOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { fuelType = option; fuelExpanded = false }) }
                }
            }

            Text(stringResource(R.string.veh_acquisition_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            OutlinedTextField(
                value = if (acquisitionDate.isNotEmpty()) formatDateString(acquisitionDate) else "", onValueChange = {},
                label = { Text(stringResource(R.string.veh_acquisition_date)) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try { if (acquisitionDate.isNotEmpty()) calendar.time = sdf.parse(acquisitionDate)!! } catch (e: Exception) {}
                        DatePickerDialog(context, { _, y, m, d ->
                            val cal = Calendar.getInstance(); cal.set(y, m, d)
                            acquisitionDate = sdf.format(cal.time)
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.DateRange, null) }
                }
            )
            OutlinedTextField(value = acquisitionMileage, onValueChange = { if (it.all { c -> c.isDigit() }) acquisitionMileage = it }, label = { Text(stringResource(R.string.veh_acquisition_km)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

            // Section Documents
            Text(stringResource(R.string.veh_documents), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                item {
                    Box(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { if (photosList.size < 8) showGallerySourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Ajouter", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                items(photosList) { attachment ->
                    DocumentThumbnail(
                        attachment = attachment, 
                        onDelete = { fileToDelete = attachment },
                        onEditLabel = { newLabel ->
                            photosList = photosList.map { 
                                if (it.path == attachment.path) it.copy(label = newLabel) else it 
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(300.dp))
        }
    }

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSourceDialog = false },
            onCamera = {
                showPhotoSourceDialog = false
                val check = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                if (check == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    try {
                        val tempDir = File(context.cacheDir, "temp_photos")
                        if (!tempDir.exists()) tempDir.mkdirs()
                        val file = File(tempDir, "profile_${System.currentTimeMillis()}.jpg")
                        tempPhotoFile = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {}
                } else permissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                showPhotoSourceDialog = false
                singleGalleryLauncher.launch("image/*")
            }
        )
    }

    if (showGallerySourceDialog) {
        PhotoSourceDialog(
            onDismiss = { showGallerySourceDialog = false },
            onCamera = {
                showGallerySourceDialog = false
                val check = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                if (check == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    try {
                        val tempDir = File(context.cacheDir, "temp_photos")
                        if (!tempDir.exists()) tempDir.mkdirs()
                        val file = File(tempDir, "gal_${System.currentTimeMillis()}.jpg")
                        tempPhotoFile = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraGalleryLauncher.launch(uri)
                    } catch (e: Exception) {}
                } else permissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                showGallerySourceDialog = false
                multiGalleryLauncher.launch(arrayOf("*/*"))
            }
        )
    }

    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text(stringResource(R.string.settings_country)) },
            text = {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(scrollState)) {
                    countryOptions.forEach { (code, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { countryCode = code; showCountryDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (code == countryCode), onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCountryDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_doc_title)) },
            text = { Text(stringResource(R.string.dialog_delete_doc_msg)) },
            confirmButton = {
                TextButton(onClick = { photosList = photosList.filter { it != fileToDelete }; fileToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = { TextButton(onClick = { fileToDelete = null }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}

@Composable
fun ProfilePhotoBox(photoPath: String, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (photoPath.isNotEmpty()) {
            val absolutePath = FileHelper.toAbsolutePath(context, photoPath)
            AsyncImage(model = File(absolutePath), contentDescription = "Photo véhicule", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                Text("Photo principale", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun ThemeSelector(selectedKey: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.veh_theme_label), 
            fontWeight = FontWeight.Bold, 
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("INDIGO", "BLUE", "GREEN", "RED", "GREY").forEach { key ->
                val color = when(key) {
                    "INDIGO" -> Color(0xFF6366F1)
                    "BLUE" -> Color(0xFF0EA5E9)
                    "GREEN" -> Color(0xFF10B981)
                    "RED" -> Color(0xFFEF4444)
                    else -> Color(0xFF64748B)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelect(key) }
                        .border(
                            width = 3.dp,
                            color = if (selectedKey == key) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
