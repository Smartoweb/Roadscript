package com.roadscript.oss.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import android.net.Uri
import com.roadscript.oss.R
import com.roadscript.oss.ui.VehicleViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    var showImportWarning by rememberSaveable { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showFreemiumWarning by rememberSaveable { mutableStateOf(false) }
    
    // Pour différencier l'importation TOTALE de la SÉLECTIVE
    var isFullImportMode by rememberSaveable { mutableStateOf(false) }
    
    // États pour l'exportation sélective
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var selectedExportIds by remember { mutableStateOf(setOf<String>()) }
    
    // États pour l'importation sélective
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var backupPreview by remember { mutableStateOf<com.roadscript.oss.data.xml.GlobalBackup?>(null) }
    var selectedImportIds by remember { mutableStateOf(setOf<String>()) }
    
    val garage by viewModel.garage.collectAsState()
    val isPro by viewModel.isPro.collectAsState()

    // Launcher pour l'exportation globale (ZIP)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    // Si selectedExportIds est vide, c'est un export TOTAL (Section 1)
                    // Sinon c'est un export SÉLECTIF (Section 2)
                    val idsList = if (selectedExportIds.isNotEmpty()) selectedExportIds.toList() else null
                    
                    viewModel.exportGlobalBackup(os, idsList,
                        onSuccess = { 
                            showExportDialog = false
                            Toast.makeText(context, context.getString(R.string.msg_export_success), Toast.LENGTH_SHORT).show() 
                        },
                        onError = { err -> Toast.makeText(context, "${context.getString(R.string.msg_error)}: $err", Toast.LENGTH_LONG).show() }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${context.getString(R.string.msg_write_error)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher pour l'importation globale (ZIP)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Dans tous les cas, on "peek" le contenu pour l'importation sélective
            try {
                context.contentResolver.openInputStream(uri)?.use { isStream ->
                    viewModel.peekBackup(isStream,
                        onSuccess = { preview ->
                            backupPreview = preview
                            pendingImportUri = uri
                            selectedImportIds = preview.vehicles.map { it.vehicle.id }.toSet()
                            
                            if (isFullImportMode) {
                                showImportWarning = true
                            } else {
                                showImportDialog = true
                            }
                        },
                        onError = { err -> Toast.makeText(context, "Erreur lecture ZIP: $err", Toast.LENGTH_LONG).show() }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ie_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = stringResource(R.string.ie_header),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = stringResource(R.string.ie_desc),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // SECTION 1 : SAUVEGARDE GLOBALE
            Text(
                "SAUVEGARDE TOTALE (Système)", 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
                            val filename = "RS_${sdf.format(Date())}.zip"
                            // null = export total (véhicules + réglages)
                            selectedExportIds = emptySet() 
                            exportLauncher.launch(filename)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Exporter Tout", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { 
                            isFullImportMode = true
                            importLauncher.launch(arrayOf("application/zip")) 
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Restore, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Restaurer Tout", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Remplace l'intégralité des données et réglages de l'application.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SECTION 2 : GESTION DES VÉHICULES
            Text(
                "GESTION DES VÉHICULES", 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            selectedExportIds = garage.map { it.id }.toSet()
                            showExportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.DirectionsCar, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Exporter des véhicules", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { 
                            isFullImportMode = false
                            importLauncher.launch(arrayOf("application/zip"))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Importer des véhicules", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Vos données seront préservées, seuls les véhicules sélectionnés seront ajoutés ou mis à jour.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = { showImportWarning = false },
            title = { Text(stringResource(R.string.ie_warning_title)) },
            text = { 
                Text(stringResource(R.string.ie_warning_msg)) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportWarning = false
                        // On force l'importation TOTALE d'origine
                        pendingImportUri?.let { uri ->
                            performImport(context, viewModel, uri)
                        } ?: run {
                            importLauncher.launch(arrayOf("application/zip"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.ie_btn_confirm_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarning = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showFreemiumWarning) {
        AlertDialog(
            onDismissRequest = { 
                showFreemiumWarning = false
                pendingImportUri = null 
            },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.ie_truncated_warning_title), fontWeight = FontWeight.Bold)
                }
            },
            text = { 
                Column {
                    Text(stringResource(R.string.ie_truncated_warning_msg))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.btn_upgrade_pro),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            showFreemiumWarning = false
                            onNavigateToAbout()
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        val uri = pendingImportUri
                        showFreemiumWarning = false
                        pendingImportUri = null
                        if (uri != null) {
                            performImport(context, viewModel, uri)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.btn_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showFreemiumWarning = false
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Sélectionner les véhicules à exporter") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    garage.forEach { v ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedExportIds = if (v.id in selectedExportIds) selectedExportIds - v.id else selectedExportIds + v.id
                            }
                        ) {
                            Checkbox(checked = v.id in selectedExportIds, onCheckedChange = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(v.model, fontWeight = FontWeight.Bold)
                                Text("${v.brand} - ${v.plateNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
                        val filename = "VH_${sdf.format(Date())}.zip"
                        exportLauncher.launch(filename)
                    },
                    enabled = selectedExportIds.isNotEmpty()
                ) { Text("Exporter (${selectedExportIds.size})") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showImportDialog && backupPreview != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Contenu de la sauvegarde") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choisissez les véhicules à importer :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    backupPreview!!.vehicles.forEach { bundle ->
                        val v = bundle.vehicle
                        val alreadyExists = garage.any { it.id == v.id }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = v.id in selectedImportIds,
                                    onCheckedChange = { checked ->
                                        if (!isPro && checked) {
                                            selectedImportIds = setOf(v.id)
                                        } else {
                                            selectedImportIds = if (checked) selectedImportIds + v.id else selectedImportIds - v.id
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(v.model, fontWeight = FontWeight.Bold)
                                    Text("${v.brand} - ${v.plateNumber}", fontSize = 11.sp)
                                    if (alreadyExists) {
                                        Text("⚠️ Existe déjà sur ce téléphone", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!isPro && selectedImportIds.size > 0) {
                        Text("Version gratuite : 1 seul véhicule sera importé.", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri
                        if (uri != null) {
                            // On définit la stratégie : REPLACE si déjà existant, IMPORT_NEW par défaut
                            val strategies = backupPreview!!.vehicles.associate { 
                                it.vehicle.id to "REPLACE" 
                            }
                            performSelectiveImport(context, viewModel, uri, selectedImportIds.toList(), strategies)
                            showImportDialog = false
                        }
                    },
                    enabled = selectedImportIds.isNotEmpty()
                ) { Text("Importer") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

/**
 * Helper pour l'importation sélective.
 */
private fun performSelectiveImport(
    context: android.content.Context, 
    viewModel: VehicleViewModel, 
    uri: Uri, 
    selectedIds: List<String>,
    strategies: Map<String, String>
) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            viewModel.importSelectedVehicles(inputStream, selectedIds, strategies,
                onSuccess = { truncated -> 
                    val msg = if (truncated) "Importation réussie (limitée par la version gratuite)" else "Importation réussie !"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
                onError = { err -> Toast.makeText(context, "Erreur import: $err", Toast.LENGTH_LONG).show() }
            )
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur lecture: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Helper pour factoriser l'appel à l'importation réelle.
 */
private fun performImport(context: android.content.Context, viewModel: VehicleViewModel, uri: Uri) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            viewModel.importGlobalBackup(inputStream,
                onSuccess = { _ -> 
                    // Note: Le paramètre 'truncated' est ignoré ici car l'utilisateur a déjà été prévenu avant.
                    Toast.makeText(context, context.getString(R.string.msg_import_success), Toast.LENGTH_SHORT).show()
                },
                onError = { err -> Toast.makeText(context, "${context.getString(R.string.msg_error)}: $err", Toast.LENGTH_LONG).show() }
            )
        }
    } catch (e: Exception) {
        Toast.makeText(context, "${context.getString(R.string.msg_read_error)}: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
