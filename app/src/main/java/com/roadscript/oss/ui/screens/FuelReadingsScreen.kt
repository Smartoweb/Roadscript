package com.roadscript.oss.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
    val avgConsumption by viewModel.averageConsumption.collectAsState()
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
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (avgConsumption > 0) String.format(Locale.FRANCE, "%.2f L/100km", avgConsumption) else "-- L/100km",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Consommation moyenne calculée sur ${fuelReadings.count { it.odometer != null }} relevés avec kilométrage.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                itemsIndexed(fuelReadings) { index, reading ->
                    // Calcul de la consommation du segment (entre ce plein et le précédent chronologiquement)
                    val segmentConsumption = if (reading.odometer != null && index < fuelReadings.size - 1) {
                        val previous = fuelReadings.subList(index + 1, fuelReadings.size).find { it.odometer != null }
                        if (previous != null) {
                            val dist = reading.odometer - previous.odometer!!
                            if (dist > 0) (reading.liters / dist) * 100.0 else null
                        } else null
                    } else null

                    FuelItemCard(
                        reading = reading,
                        segmentConsumption = segmentConsumption,
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
            onConfirm = { date, liters, cost, odometer ->
                viewModel.addFuelReading(date, liters, cost, odometer,
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
            onConfirm = { date, liters, cost, odometer ->
                viewModel.updateFuelReading(readingToEdit!!, date, liters, cost, odometer,
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
                Text(stringResource(R.string.fuel_delete_confirm, formatDateString(readingToDelete!!.date), String.format(Locale.FRANCE, "%.2f", readingToDelete!!.liters), String.format(Locale.FRANCE, "%.2f", readingToDelete!!.cost), currency)) 
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
    segmentConsumption: Double? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currency = CountryHelper.getCurrencySymbol(reading.countryCode ?: "")
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
                Text(
                    text = "${String.format(Locale.FRANCE, "%.2f", reading.liters)} L - ${String.format(Locale.FRANCE, "%.2f", reading.cost)} $currency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = buildString {
                        append(formatDateString(reading.date))
                        reading.odometer?.let { append(" • $it km") }
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (segmentConsumption != null) {
                    Text(
                        text = String.format(Locale.FRANCE, "📈 %.2f L/100km", segmentConsumption),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
