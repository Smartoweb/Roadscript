package com.roadscript.oss.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.ui.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // États de sélection
    var exportProfile by rememberSaveable { mutableStateOf<String?>(null) } // null, PERSONAL, THIRD_PARTY
    
    var includeAssurance by rememberSaveable { mutableStateOf(true) }
    var includeMechanic by rememberSaveable { mutableStateOf(true) }
    
    var mileageOption by rememberSaveable { mutableStateOf(0) } // 0: Dernier, 1: Historique + Graph
    var fuelOption by rememberSaveable { mutableStateOf(1) } // 0: Rien, 1: Conso moyenne, 2: Historique + Graph
    
    var includeEvents by rememberSaveable { mutableStateOf(true) }
    var includeAttachments by rememberSaveable { mutableStateOf(true) }
    var includeStats by rememberSaveable { mutableStateOf(true) }

    val events by viewModel.evenements.collectAsState()
    val fuelReadings by viewModel.fuelReadings.collectAsState()
    val mileageReadings by viewModel.mileageReadings.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val enableMultiVehicle by viewModel.enableMultiVehicle.collectAsState()

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Lancer le processus d'exportation
            com.roadscript.oss.ui.utils.ExportManager.startExport(
                context = context,
                targetTreeUri = uri,
                vehicle = vehicle,
                mileage = mileageReadings,
                fuel = fuelReadings,
                events = events,
                categories = categories,
                subCategories = subCategories,
                options = com.roadscript.oss.ui.utils.ExportOptions(
                    includeAssurance = includeAssurance,
                    includeMechanic = includeMechanic,
                    mileageOption = mileageOption,
                    fuelOption = fuelOption,
                    includeEvents = includeEvents,
                    includeAttachments = includeAttachments,
                    includeStats = includeStats,
                    isThirdParty = exportProfile == "THIRD_PARTY"
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_pack_title), fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            val introText = if (enableMultiVehicle) {
                stringResource(R.string.export_pack_intro_multi)
            } else {
                stringResource(R.string.export_pack_intro_single)
            }

            Text(
                text = introText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            // Choix du profil d'export
            Text(
                text = stringResource(R.string.export_profile_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bouton Personnel
                FilterChip(
                    selected = exportProfile == "PERSONAL",
                    onClick = { 
                        exportProfile = "PERSONAL"
                        // Reset à tout activé
                        includeAssurance = true
                        includeMechanic = true
                        includeEvents = true
                        includeAttachments = true
                        includeStats = true
                    },
                    label = { Text(stringResource(R.string.export_profile_personal)) },
                    modifier = Modifier.weight(1f)
                )
                // Bouton Tiers
                FilterChip(
                    selected = exportProfile == "THIRD_PARTY",
                    onClick = { 
                        exportProfile = "THIRD_PARTY"
                        // Désactivation automatique des options sensibles
                        includeAssurance = false 
                        includeMechanic = false
                        includeStats = false
                        includeAttachments = true // Inclus mais sera filtré par ExportManager
                    },
                    label = { Text(stringResource(R.string.export_profile_third_party)) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (exportProfile != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (exportProfile == "PERSONAL") stringResource(R.string.export_profile_personal_desc) else stringResource(R.string.export_profile_third_party_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Section Identité
                PrintSectionTitle(stringResource(R.string.export_pack_section_admin))
                PrintToggleItem(stringResource(R.string.export_pack_item_veh), true, enabled = false)
                PrintToggleItem(stringResource(R.string.export_pack_item_insur), includeAssurance) { includeAssurance = it }
                PrintToggleItem(stringResource(R.string.export_pack_item_mech), includeMechanic) { includeMechanic = it }

                // Section Technique
                PrintSectionTitle(stringResource(R.string.export_pack_section_tech))
                PrintOptionSelector(
                    stringResource(R.string.export_pack_item_mileage), 
                    listOf(stringResource(R.string.export_pack_opt_mileage_last), stringResource(R.string.export_pack_opt_mileage_full)), 
                    mileageHistoryOptionsMapping(mileageOption)
                ) { 
                    mileageOption = it 
                }
                
                PrintOptionSelector(
                    stringResource(R.string.export_pack_item_fuel), 
                    listOf(stringResource(R.string.export_pack_opt_fuel_none), stringResource(R.string.export_pack_opt_fuel_sum), stringResource(R.string.export_pack_opt_fuel_full)), 
                    fuelOption
                ) { 
                    fuelOption = it 
                }

                // Section Historique
                PrintSectionTitle(stringResource(R.string.export_pack_section_history))
                PrintToggleItem(stringResource(R.string.export_pack_item_events), includeEvents) { includeEvents = it }
                if (includeEvents) {
                    PrintToggleItem(stringResource(R.string.export_pack_item_docs), includeAttachments) { includeAttachments = it }
                }
                
                PrintToggleItem(stringResource(R.string.export_pack_item_stats), includeStats) { includeStats = it }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { folderLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Folder, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.export_pack_btn_generate), fontWeight = FontWeight.Bold)
                }
                
                Text(
                    stringResource(R.string.export_pack_disclaimer),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun PrintSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun PrintToggleItem(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = if (enabled) onCheckedChange else null, enabled = enabled)
    }
}

@Composable
fun PrintOptionSelector(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = options[selectedIndex],
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
                options.forEachIndexed { index, opt ->
                    DropdownMenuItem(text = { Text(opt, fontSize = 14.sp) }, onClick = { onSelect(index); expanded = false })
                }
            }
        }
    }
}

// Helpers
private fun mileageHistoryOptionsMapping(index: Int): Int = index
