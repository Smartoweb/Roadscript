package com.roadscript.oss.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.data.models.FuelReading
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.AddFuelDialog
import com.roadscript.oss.ui.components.EditFuelDialog
import com.roadscript.oss.ui.components.formatDateString
import com.roadscript.oss.ui.utils.CountryHelper
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelReadingsScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val fuelReadings by viewModel.fuelReadings.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()
    val currency = CountryHelper.getCurrencySymbol(vehicle?.countryCode ?: "")
    
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var readingToEdit by remember { mutableStateOf<FuelReading?>(null) }
    var readingToDelete by remember { mutableStateOf<FuelReading?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fuel_history_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fuel_add_title))
            }
        }
    ) { paddingValues ->
        if (fuelReadings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_no_fuel), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Les statistiques sont calculées sur l'intervalle compris entre la date du 1er relevé et la date du dernier. La consommation moyenne ne tient pas compte du volume du dernier plein.",
                                fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(fuelReadings) { reading ->
                    FuelItemCard(
                        reading = reading,
                        onEdit = { readingToEdit = reading },
                        onDelete = { readingToDelete = reading }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddFuelDialog(
            countryCode = vehicle?.countryCode ?: "",
            onDismiss = { showAddDialog = false },
            onConfirm = { date, liters, cost ->
                viewModel.addFuelReading(date, liters, cost,
                    onSuccess = { showAddDialog = false },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (readingToEdit != null) {
        EditFuelDialog(
            initialReading = readingToEdit!!,
            countryCode = vehicle?.countryCode ?: "",
            onDismiss = { readingToEdit = null },
            onConfirm = { date, liters, cost ->
                viewModel.updateFuelReading(readingToEdit!!, date, liters, cost,
                    onSuccess = { readingToEdit = null },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (readingToDelete != null) {
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            title = { Text(stringResource(R.string.fuel_delete_title)) },
            text = { 
                Text(stringResource(R.string.fuel_delete_confirm, formatDateString(readingToDelete!!.date), String.format("%.2f", readingToDelete!!.liters), String.format("%.2f", readingToDelete!!.cost), currency)) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFuelReading(readingToDelete!!.date, readingToDelete!!.liters,
                            onSuccess = {
                                readingToDelete = null
                                Toast.makeText(context, context.getString(R.string.msg_fuel_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { readingToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun FuelItemCard(
    reading: FuelReading,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val itemCurrency = CountryHelper.getCurrencySymbol(reading.countryCode ?: "")
                Text(
                    text = "${String.format(Locale.FRANCE, "%.2f", reading.liters)} L - ${String.format(Locale.FRANCE, "%.2f", reading.cost)} $itemCurrency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${formatDateString(reading.date)} (${String.format(Locale.FRANCE, "%.3f", reading.cost / reading.liters)} $itemCurrency/L)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, stringResource(R.string.btn_edit), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.btn_delete), tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }
    }
}
