package com.roadscript.oss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.roadscript.oss.R
import com.roadscript.oss.data.models.Vehicle
import com.roadscript.oss.ui.utils.FileHelper
import java.io.File

@Composable
fun GarageTopSheet(
    garage: List<Vehicle>,
    activeVehicleId: String,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: () -> Unit,
    onDeleteVehicle: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.menu_my_vehicles),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null)
                        }
                    }

                    // Bouton d'action en HAUT (comme demandé)
                    Button(
                        onClick = {
                            if (isPro) {
                                onAddVehicle()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isPro
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ajouter un véhicule")
                        if (!isPro) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.alpha(0.3f))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(garage) { vehicle ->
                            VehicleListItem(
                                vehicle = vehicle,
                                isActive = vehicle.id == activeVehicleId,
                                canDelete = garage.size > 1,
                                onSelect = { 
                                    onSelectVehicle(vehicle.id)
                                    onDismiss()
                                },
                                onDelete = { 
                                    if (garage.size > 1) {
                                        vehicleToDelete = vehicle 
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(R.string.err_last_vehicle), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (vehicleToDelete != null) {
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_veh_title, vehicleToDelete!!.model)) },
            text = { Text(stringResource(R.string.dialog_delete_veh_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVehicle(vehicleToDelete!!.id)
                        vehicleToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun VehicleListItem(
    vehicle: Vehicle,
    isActive: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    
    // On récupère la couleur correspondant au thème du véhicule
    val themeColor = when(vehicle.themeKey) {
        "INDIGO" -> Color(0xFF6366F1)
        "BLUE" -> Color(0xFF0EA5E9)
        "GREEN" -> Color(0xFF10B981)
        "RED" -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }
    
    val backgroundColor = if (isActive) {
        themeColor.copy(alpha = 0.15f)
    } else {
        themeColor.copy(alpha = 0.08f)
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature
            if (vehicle.photoPath.isNotBlank()) {
                val absolutePath = FileHelper.toAbsolutePath(context, vehicle.photoPath)
                AsyncImage(
                    model = File(absolutePath),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.model, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (vehicle.plateNumber.isNotBlank()) {
                    Text(vehicle.plateNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
