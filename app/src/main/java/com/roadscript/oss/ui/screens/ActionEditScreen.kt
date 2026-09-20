package com.roadscript.oss.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.data.models.Attachment
import com.roadscript.oss.data.models.EvenementAction
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.DocumentThumbnail
import com.roadscript.oss.ui.components.PhotoSourceDialog
import com.roadscript.oss.ui.components.formatDateString
import com.roadscript.oss.ui.utils.TranslationHelper
import com.roadscript.oss.ui.utils.FileHelper
import com.roadscript.oss.ui.utils.CountryHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionEditScreen(
    viewModel: VehicleViewModel,
    actionId: String?, // null si ajout, ID si modification
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val evenements by viewModel.evenements.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()
    val currentCountryCode = vehicle?.countryCode ?: ""
    val activeVehicleId by viewModel.activeVehicleId.collectAsState()

    // États du formulaire
    var name by rememberSaveable { mutableStateOf("") }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var date by rememberSaveable { mutableStateOf(sdf.format(Date())) }
    var rootCategoryKey by rememberSaveable { mutableStateOf("") }
    var rootSubCategoryKey by rememberSaveable { mutableStateOf("") }
    var isMultiple by rememberSaveable { mutableStateOf(false) }
    var costStr by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var photosList by rememberSaveable { mutableStateOf<List<Attachment>>(emptyList()) }
    
    // Liste des actions internes avec Saver personnalisé
    val subActionsList = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<EvenementAction>().apply { addAll(it) } }
        )
    ) { mutableStateListOf<EvenementAction>() }
    
    var fileToDelete by remember { mutableStateOf<Attachment?>(null) }

    // Charger l'événement en cas d'édition
    LaunchedEffect(actionId, evenements) {
        if (actionId != null && subActionsList.isEmpty() && rootCategoryKey.isEmpty()) {
            val evt = evenements.find { it.id == actionId }
            if (evt != null) {
                name = evt.name
                date = evt.date
                rootCategoryKey = evt.categoryKey
                rootSubCategoryKey = evt.subCategoryKey
                isMultiple = evt.isMultiple
                costStr = if (evt.cost == 0.0) "" else evt.cost.toString()
                description = evt.description
                photosList = evt.photos
                subActionsList.clear()
                subActionsList.addAll(evt.actions)
            }
        } else if (actionId == null && rootCategoryKey.isEmpty() && categories.isNotEmpty()) {
            rootCategoryKey = categories.first().key
        }

        // Pour Entretien/Dépannage, s'assurer qu'on a au moins une action si la liste est vide
        if ((rootCategoryKey == "entretien" || rootCategoryKey == "depannage") && subActionsList.isEmpty()) {
            subActionsList.add(EvenementAction("", rootCategoryKey, ""))
        }
    }

    // Filtrer les sous-catégories pour la RACINE
    val filteredRootSubCategories = remember(rootCategoryKey, subCategories) {
        subCategories.filter { it.categoryKey == rootCategoryKey }
    }
    
    // Réinitialiser la sous-catégorie racine si la catégorie change
    LaunchedEffect(rootCategoryKey) {
        if (filteredRootSubCategories.isNotEmpty() && filteredRootSubCategories.none { it.key == rootSubCategoryKey }) {
            rootSubCategoryKey = filteredRootSubCategories.first().key
        }
        // Désactiver le mode multiple pour les catégories non éligibles
        if (rootCategoryKey == "obligation" || rootCategoryKey == "accessoire") {
            isMultiple = false
        }
    }

    // Launcher caméra et galerie (inchangés)
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var showPhotoSourceDialog by rememberSaveable { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoFile != null) {
            if (photosList.size < 8) {
                val vehicleFolder = FileHelper.getVehicleFolder(context, activeVehicleId)
                val destFile = File(vehicleFolder, "evt_${System.currentTimeMillis()}.jpg")
                tempPhotoFile!!.renameTo(destFile)
                photosList = photosList + Attachment(FileHelper.toRelativePath(context, destFile.absolutePath), destFile.name)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission accordée. Re-cliquez pour ouvrir l'appareil photo.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
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
                title = { Text(if (actionId == null) stringResource(R.string.action_add_title) else stringResource(R.string.action_edit_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.err_name_req), Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val isAutoMultiple = rootCategoryKey == "entretien" || rootCategoryKey == "depannage"
                            
                            if (isAutoMultiple && subActionsList.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.err_min_action), Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Déterminer la sous-catégorie racine pour l'affichage accueil
                            val finalSubKey = if (isAutoMultiple) {
                                subActionsList.firstOrNull()?.subCategoryKey ?: ""
                            } else {
                                rootSubCategoryKey
                            }

                            val cost = costStr.toDoubleOrNull() ?: 0.0
                            val success = { Toast.makeText(context, context.getString(R.string.msg_saved), Toast.LENGTH_SHORT).show(); onSaveSuccess() }
                            val error = { err: String -> 
                                if (err == "LIMIT_REACHED") {
                                    Toast.makeText(context, context.getString(R.string.err_freemium_limit), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            }

                            if (actionId == null) {
                                viewModel.addEvenement(name.trim(), date, rootCategoryKey, finalSubKey, cost, description.trim(), isAutoMultiple, subActionsList.toList(), photosList, success, error)
                            } else {
                                viewModel.updateEvenement(actionId, name.trim(), date, rootCategoryKey, finalSubKey, cost, description.trim(), isAutoMultiple, subActionsList.toList(), photosList, success, error)
                            }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.action_name_req)) }, singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = formatDateString(date), onValueChange = {}, label = { Text(stringResource(R.string.action_date_req)) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        try {
                            calendar.time = sdf.parse(date)!!
                        } catch (_: Exception) {
                        }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val cal = Calendar.getInstance()
                                cal.set(y, m, d)
                                date = sdf.format(cal.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) { Icon(Icons.Default.DateRange, null) }
                }
            )

            // Sélection de la Catégorie RACINE
            var rootCatExpanded by remember { mutableStateOf(false) }
            val currentRootCat = categories.find { it.key == rootCategoryKey }
            val currentRootCatName = currentRootCat?.let { TranslationHelper.getCategoryName(context, it.key, it.name) } ?: ""
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentRootCatName, onValueChange = {}, label = { Text(stringResource(R.string.action_root_cat_req)) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { rootCatExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                )
                DropdownMenu(expanded = rootCatExpanded, onDismissRequest = { rootCatExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    categories.forEach { cat -> 
                        val translatedCat = TranslationHelper.getCategoryName(context, cat.key, cat.name)
                        DropdownMenuItem(text = { Text(translatedCat) }, onClick = { rootCategoryKey = cat.key; rootCatExpanded = false }) 
                    }
                }
            }

            if (rootCategoryKey == "obligation" || rootCategoryKey == "accessoire") {
                var rootSubExpanded by remember { mutableStateOf(false) }
                val currentRootSub = filteredRootSubCategories.find { it.key == rootSubCategoryKey }
                
                // Filtrage intelligent : Uniquement les sous-catégories du pays actuel (ou sans pays)
                val displayRootSubs = remember(rootCategoryKey, subCategories, currentCountryCode) {
                    subCategories.filter { 
                        it.categoryKey == rootCategoryKey && 
                        (it.countryCode == null || it.countryCode == currentCountryCode) 
                    }
                }

                val currentRootSubName = currentRootSub?.let { TranslationHelper.getSubCategoryName(context, it.key, it.name) } ?: ""
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currentRootSubName, onValueChange = {}, label = { Text(stringResource(R.string.action_root_sub_req)) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { rootSubExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = rootSubExpanded, onDismissRequest = { rootSubExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                        displayRootSubs.forEach { sub -> 
                            val translatedSub = TranslationHelper.getSubCategoryName(context, sub.key, sub.name)
                            DropdownMenuItem(text = { Text(translatedSub) }, onClick = { rootSubCategoryKey = sub.key; rootSubExpanded = false }) 
                        }
                    }
                }
            }

            val currency = CountryHelper.getCurrencySymbol(vehicle?.countryCode ?: "")
            OutlinedTextField(
                value = costStr, 
                onValueChange = { input -> if (input.isEmpty() || input.all { it.isDigit() || it == '.' || it == ',' }) costStr = input.replace(',', '.') }, 
                label = { Text(stringResource(R.string.action_cost_total, currency)) }, 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next), 
                singleLine = true, 
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.action_notes)) }, modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 4)

            // Documents
            Text(stringResource(R.string.action_documents), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                item {
                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { if (photosList.size < 8) showPhotoSourceDialog = true }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary); Text("Ajouter", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) }
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

            // Section Actions (Pour Entretien et Dépannage)
            if (rootCategoryKey == "entretien" || rootCategoryKey == "depannage") {
                Text(stringResource(R.string.action_ops_detail), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                
                subActionsList.forEachIndexed { index, subAction ->
                    ActionRow(
                        index = index,
                        subAction = subAction,
                        fixedCategoryKey = rootCategoryKey,
                        subCategories = subCategories,
                        currentCountryCode = currentCountryCode,
                        onUpdate = { updated -> subActionsList[index] = updated },
                        onDelete = { subActionsList.removeAt(index) },
                        canDelete = subActionsList.size > 1
                    )
                }

                TextButton(onClick = { subActionsList.add(EvenementAction("", rootCategoryKey, "")) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Default.Add, null); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.btn_add_action))
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
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
                        val file = File(tempDir, "evt_${System.currentTimeMillis()}.jpg")
                        tempPhotoFile = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        Log.e("ActionEdit", "Camera launch error", e)
                        Toast.makeText(context, "Erreur appareil photo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else permissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = { showPhotoSourceDialog = false; galleryLauncher.launch(arrayOf("*/*")) }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Supprimer le document ?") },
            text = { Text("Voulez-vous vraiment retirer ce document ?") },
            confirmButton = {
                TextButton(onClick = { photosList = photosList.filter { it != fileToDelete }; fileToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { fileToDelete = null }) { Text("Annuler") } }
        )
    }
}

@Composable
fun ActionRow(
    index: Int,
    subAction: EvenementAction,
    fixedCategoryKey: String,
    subCategories: List<com.roadscript.oss.data.models.SubCategory>,
    currentCountryCode: String,
    onUpdate: (EvenementAction) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val context = LocalContext.current
    var subExpanded by remember { mutableStateOf(false) }
    
    // Filtrage intelligent : Uniquement les sous-catégories du pays actuel (ou sans pays)
    val displaySubs = remember(fixedCategoryKey, subCategories, currentCountryCode) {
        subCategories.filter { 
            it.categoryKey == fixedCategoryKey && 
            (it.countryCode == null || it.countryCode == currentCountryCode) 
        }
    }
    
    val currentSub = subCategories.find { it.key == subAction.subCategoryKey }
    val currentSubName = currentSub?.let { TranslationHelper.getSubCategoryName(context, subAction.subCategoryKey, it.name) } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${stringResource(R.string.action_label)} ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) }
                }
            }
            
            OutlinedTextField(
                value = subAction.name, onValueChange = { onUpdate(subAction.copy(name = it, categoryKey = fixedCategoryKey)) },
                label = { Text(stringResource(R.string.action_op_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentSubName, onValueChange = {}, label = { Text(stringResource(R.string.action_op_sub)) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { subExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                )
                DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }, modifier = Modifier.fillMaxWidth(0.8f)) {
                    displaySubs.forEach { sub -> 
                        val translatedSub = TranslationHelper.getSubCategoryName(context, sub.key, sub.name)
                        DropdownMenuItem(text = { Text(translatedSub) }, onClick = { onUpdate(subAction.copy(subCategoryKey = sub.key, categoryKey = fixedCategoryKey)); subExpanded = false }) 
                    }
                }
            }
        }
    }
}
