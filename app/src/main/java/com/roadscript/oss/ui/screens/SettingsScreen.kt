package com.roadscript.oss.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.roadscript.oss.R
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.utils.CountryHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToExport: () -> Unit,
    onResetComplete: () -> Unit
) {
    val context = LocalContext.current
    var showYearsFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    
    val defaultYearsFilter by viewModel.defaultYearsFilter.collectAsState()
    val showMileageZone by viewModel.showMileageZone.collectAsState()
    val showFuelZone by viewModel.showFuelZone.collectAsState()
    val enableMultiVehicle by viewModel.enableMultiVehicle.collectAsState()
    val garage by viewModel.garage.collectAsState()
    val currentLanguageCode by viewModel.currentLanguage.collectAsState()
    
    val yearsOptions = listOf(
        stringResource(R.string.years_all),
        stringResource(R.string.years_current),
        stringResource(R.string.years_2),
        stringResource(R.string.years_3),
        stringResource(R.string.years_4),
        stringResource(R.string.years_5)
    )

    val languageOptions = listOf(
        "fr" to stringResource(R.string.lang_fr),
        "en" to stringResource(R.string.lang_en),
        "es" to stringResource(R.string.lang_es),
        "de" to stringResource(R.string.lang_de),
        "it" to stringResource(R.string.lang_it),
        "pt" to stringResource(R.string.lang_pt)
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.btn_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Section Internationalisation
            SettingsHeader(title = stringResource(R.string.settings_international), icon = Icons.Default.Public)
            
            // Langue
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Medium) },
                supportingContent = { 
                    val currentLabel = languageOptions.find { 
                        currentLanguageCode.startsWith(it.first) 
                    }?.second ?: languageOptions[1].second // Fallback to English if not found
                    Text(currentLabel) 
                },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 2. Section Page principale
            SettingsHeader(title = stringResource(R.string.settings_main_page), icon = Icons.Default.Dashboard)
            
            // Carburant
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_fuel), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.settings_home_page)) },
                trailingContent = {
                    Checkbox(
                        checked = showFuelZone,
                        onCheckedChange = { viewModel.updateShowFuelZone(it) }
                    )
                },
                modifier = Modifier.clickable { viewModel.updateShowFuelZone(!showFuelZone) }
            )

            // Kilométrage
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_mileage), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.settings_home_page)) },
                trailingContent = {
                    Checkbox(
                        checked = showMileageZone,
                        onCheckedChange = { viewModel.updateShowMileageZone(it) }
                    )
                },
                modifier = Modifier.clickable { viewModel.updateShowMileageZone(!showMileageZone) }
            )

            // Événements / Historique
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_default_years), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(yearsOptions.getOrElse(defaultYearsFilter) { stringResource(R.string.years_all) }) },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, null)
                },
                modifier = Modifier.clickable { showYearsFilterDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 3. Section Divers
            SettingsHeader(title = stringResource(R.string.settings_misc), icon = Icons.Default.MoreHoriz)

            // Gestion Multi-véhicules
            ListItem(
                headlineContent = { 
                    Text(stringResource(R.string.settings_enable_multi_vehicle), fontWeight = FontWeight.Medium)
                },
                supportingContent = { Text(stringResource(R.string.settings_multi_vehicle_desc)) },
                trailingContent = {
                    Checkbox(
                        checked = enableMultiVehicle,
                        onCheckedChange = { checked ->
                            if (!checked && garage.size > 1) {
                                android.widget.Toast.makeText(context, context.getString(R.string.err_cannot_disable_multi_vehicle), android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.updateEnableMultiVehicle(checked)
                            }
                        }
                    )
                },
                modifier = Modifier.clickable { 
                    if (enableMultiVehicle && garage.size > 1) {
                        android.widget.Toast.makeText(context, context.getString(R.string.err_cannot_disable_multi_vehicle), android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.updateEnableMultiVehicle(!enableMultiVehicle) 
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp, end = 16.dp))

            // Bouton de suppression des données
            ListItem(
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.btn_delete_all), 
                        fontWeight = FontWeight.Medium,
                        color = Color.Red 
                    ) 
                },
                supportingContent = { Text(stringResource(R.string.settings_danger_zone)) },
                trailingContent = { Icon(Icons.Default.DeleteForever, null, tint = Color.Red.copy(alpha = 0.7f)) },
                modifier = Modifier.clickable { showDeleteAllDialog = true }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showYearsFilterDialog) {
        AlertDialog(
            onDismissRequest = { showYearsFilterDialog = false },
            title = { Text(stringResource(R.string.settings_default_years)) },
            text = {
                Column {
                    yearsOptions.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateDefaultYearsFilter(index)
                                    showYearsFilterDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (index == defaultYearsFilter),
                                onClick = {
                                    viewModel.updateDefaultYearsFilter(index)
                                    showYearsFilterDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showYearsFilterDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    languageOptions.forEach { (code, label) ->
                        val isSelected = if (code.isEmpty()) currentLanguageCode.isEmpty() 
                                         else currentLanguageCode.startsWith(code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_all_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.dialog_delete_all_msg))
                    TextButton(
                        onClick = {
                            showDeleteAllDialog = false
                            onNavigateToExport()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_go_to_export))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showDeleteAllDialog = false
                        onResetComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
