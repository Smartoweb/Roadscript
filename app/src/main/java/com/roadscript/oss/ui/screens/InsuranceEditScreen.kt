package com.roadscript.oss.ui.screens

import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.data.models.Attachment
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.DocumentThumbnail
import com.roadscript.oss.ui.components.PhotoSourceDialog
import com.roadscript.oss.ui.components.formatDateString
import com.roadscript.oss.ui.utils.FileHelper
import com.roadscript.oss.ui.utils.IntentHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceEditScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val insuranceState by viewModel.insuranceInfo.collectAsState()
    val activeVehicleId by viewModel.activeVehicleId.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var contractNumber by rememberSaveable { mutableStateOf("") }
    var customerNumber by rememberSaveable { mutableStateOf("") }
    var logoPath by rememberSaveable { mutableStateOf("") }
    var photosList by rememberSaveable { mutableStateOf<List<Attachment>>(emptyList()) }

    var fileToDelete by remember { mutableStateOf<Attachment?>(null) }

    LaunchedEffect(insuranceState) {
        if (insuranceState != null && name.isEmpty()) {
            insuranceState?.let { info ->
                name = info.name
                address = info.address
                phone = info.phone
                email = info.email
                website = info.website
                contractNumber = info.contractNumber
                customerNumber = info.customerNumber
                logoPath = info.logoPath
                photosList = info.photos
            }
        }
    }

    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var showLogoSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showGallerySourceDialog by rememberSaveable { mutableStateOf(false) }

    val logoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
            val destFile = File(vehicleFolder, "insurance_logo_${System.currentTimeMillis()}.jpg")
            tempPhotoFile!!.renameTo(destFile)
            logoPath = FileHelper.toRelativePath(context, destFile.absolutePath)
        }
    }

    val galleryCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            if (photosList.size < 8) {
                val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
                val destFile = File(vehicleFolder, "insur_gallery_${System.currentTimeMillis()}.jpg")
                tempPhotoFile!!.renameTo(destFile)
                photosList = photosList + Attachment(FileHelper.toRelativePath(context, destFile.absolutePath), destFile.name)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission accordée. Re-cliquez pour ouvrir l'appareil photo.", Toast.LENGTH_SHORT).show()
        }
    }

    val multiGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
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

    val singleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
            val localPath = FileHelper.copyUriToInternal(context, uri, vehicleFolder)
            if (localPath != null) {
                logoPath = localPath
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_insur_info), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveInsurance(
                                logoPath, name, address, phone, email, website, contractNumber, customerNumber, photosList,
                                onSuccess = {
                                    Toast.makeText(context, context.getString(R.string.msg_insur_saved), Toast.LENGTH_SHORT).show()
                                    onSaveSuccess()
                                },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold) }
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
            Box(
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { showLogoSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (logoPath.isNotEmpty()) {
                    val absolutePath = FileHelper.toAbsolutePath(context, logoPath)
                    AsyncImage(model = File(absolutePath), contentDescription = "Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(R.string.insur_logo_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.insur_name_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next), 
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contractNumber, onValueChange = { contractNumber = it }, label = { Text(stringResource(R.string.insur_cont_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next), 
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customerNumber, onValueChange = { customerNumber = it }, label = { Text(stringResource(R.string.insur_cust_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next), 
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it }, label = { Text(stringResource(R.string.mech_address_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next), 
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.mech_phone)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next), 
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (phone.isNotBlank()) {
                        IconButton(onClick = { IntentHelper.dialNumber(context, phone) }) {
                            Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.mech_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), 
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (email.isNotBlank()) {
                        IconButton(onClick = { IntentHelper.sendEmail(context, email) }) {
                            Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
            OutlinedTextField(
                value = website, onValueChange = { website = it }, label = { Text(stringResource(R.string.mech_web_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (website.isNotBlank()) {
                        IconButton(onClick = { IntentHelper.openWeb(context, website) }) {
                            Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )

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
                            Text(stringResource(R.string.btn_add), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                items(photosList) { attachment ->
                    DocumentThumbnail(
                        attachment = attachment, 
                        onDelete = { fileToDelete = attachment },
                        onEditLabel = { newLabel ->
                            photosList = photosList.map { if (it.path == attachment.path) it.copy(label = newLabel) else it }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(300.dp))
        }
    }

    if (showLogoSourceDialog) {
        PhotoSourceDialog(
            onDismiss = { showLogoSourceDialog = false },
            onCamera = {
                showLogoSourceDialog = false
                val check = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                if (check == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    try {
                        val tempDir = File(context.cacheDir, "temp_photos")
                        if (!tempDir.exists()) tempDir.mkdirs()
                        val file = File(tempDir, "insur_logo_${System.currentTimeMillis()}.jpg")
                        tempPhotoFile = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        logoCameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        Log.e("InsuranceEdit", "Logo camera launch error", e)
                        Toast.makeText(context, "Erreur appareil photo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else permissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                showLogoSourceDialog = false
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
                        val file = File(tempDir, "insur_gal_${System.currentTimeMillis()}.jpg")
                        tempPhotoFile = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        galleryCameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        Log.e("InsuranceEdit", "Gallery camera launch error", e)
                        Toast.makeText(context, "Erreur appareil photo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else permissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                showGallerySourceDialog = false
                multiGalleryLauncher.launch(arrayOf("*/*"))
            }
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
