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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.data.models.MileageReading
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.AddMileageDialog
import com.roadscript.oss.ui.components.EditMileageDialog
import com.roadscript.oss.ui.components.formatDateString
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageReadingsScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val mileageHistory by viewModel.mileageReadings.collectAsState()

    var readingToDelete by remember { mutableStateOf<MileageReading?>(null) }
    var readingToEdit by remember { mutableStateOf<MileageReading?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mileage_readings_title), fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, stringResource(R.string.mileage_add_title))
            }
        }
    ) { paddingValues ->
        if (mileageHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_no_mileage_recorded), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
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
                                text = stringResource(R.string.mileage_info_banner),
                                fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                items(mileageHistory) { reading ->
                    MileageItemCard(
                        reading = reading,
                        onEdit = { readingToEdit = reading },
                        onDelete = { readingToDelete = reading }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (readingToDelete != null) {
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            title = { Text(stringResource(R.string.mileage_delete_title)) },
            text = { 
                Text(stringResource(R.string.mileage_delete_confirm, formatDateString(readingToDelete!!.date), readingToDelete!!.value)) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMileageReading(readingToDelete!!.date,
                            onSuccess = {
                                Toast.makeText(context, context.getString(R.string.msg_mileage_deleted), Toast.LENGTH_SHORT).show()
                                readingToDelete = null
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

    if (readingToEdit != null) {
        EditMileageDialog(
            initialValue = readingToEdit!!.value,
            initialDate = readingToEdit!!.date,
            onDismiss = { readingToEdit = null },
            onConfirm = { value, date ->
                viewModel.updateMileageReading(readingToEdit!!.date, value, date,
                    onSuccess = { 
                        Toast.makeText(context, context.getString(R.string.msg_mileage_updated), Toast.LENGTH_SHORT).show()
                        readingToEdit = null
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (showAddDialog) {
        AddMileageDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { value, date ->
                viewModel.addMileageReading(value, date,
                    onSuccess = {
                        Toast.makeText(context, context.getString(R.string.msg_mileage_added), Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }
}

@Composable
fun MileageItemCard(
    reading: MileageReading,
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
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${String.format(Locale.FRANCE, "%,d", reading.value).replace(',', ' ')} km",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = formatDateString(reading.date),
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
