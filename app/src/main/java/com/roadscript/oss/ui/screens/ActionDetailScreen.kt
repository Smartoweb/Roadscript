package com.roadscript.oss.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.CategoryIconMapper
import com.roadscript.oss.ui.components.CountryBadge
import com.roadscript.oss.ui.components.formatDateString
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.data.models.Evenement
import com.roadscript.oss.data.models.Category
import com.roadscript.oss.data.models.SubCategory
import com.roadscript.oss.ui.utils.FileHelper
import com.roadscript.oss.ui.utils.TranslationHelper
import com.roadscript.oss.ui.utils.CountryHelper
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionDetailScreen(
    viewModel: VehicleViewModel,
    actionId: String,
    isTablet: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val evenements by viewModel.evenements.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()
    val currentCountryCode = vehicle?.countryCode ?: ""
    
    val evt = remember(actionId, evenements) { evenements.find { it.id == actionId } }

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (evt == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Événement introuvable.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                },
                actions = {
                    val eventCountry = evt.countryCode?.uppercase() ?: ""
                    val vehicleCountry = currentCountryCode.uppercase()
                    
                    val canEdit = eventCountry.isBlank() || eventCountry == vehicleCountry
                    
                    if (canEdit) {
                        IconButton(onClick = { onNavigateToEdit(evt.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.btn_edit))
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isTablet) {
            // Vue Tablette : 2 colonnes
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Colonne Gauche : Infos, Coût, Description
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val eventCountry = evt.countryCode?.uppercase() ?: ""
                    val vehicleCountry = currentCountryCode.uppercase()
                    val isLocked = eventCountry.isNotBlank() && eventCountry != vehicleCountry
                    if (isLocked) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Événement d'un pays différent de la sélection courante de pays pour le véhicule (Verrouillé).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    EventHeader(evt, currentCountryCode)
                    EventCostCard(evt, categories, subCategories, context, currentCountryCode)
                    if (evt.description.isNotEmpty()) {
                        EventDescription(evt)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
                
                // Colonne Droite : Sous-actions et Photos
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (evt.isMultiple && evt.actions.isNotEmpty()) {
                        EventActionsList(evt, categories, subCategories, context, currentCountryCode)
                    }
                    if (evt.photos.isNotEmpty()) {
                        EventPhotosList(evt, context)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        } else {
            // Vue Mobile : Liste verticale classique
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val isLocked = !evt.countryCode.isNullOrBlank() && evt.countryCode.lowercase() != currentCountryCode.lowercase()
                if (isLocked) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Événement d'un pays différent de la sélection courante de pays pour le véhicule. Modification impossible pour préserver l'intégrité des données.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                EventHeader(evt, currentCountryCode)
                EventCostCard(evt, categories, subCategories, context, currentCountryCode)
                if (evt.isMultiple && evt.actions.isNotEmpty()) {
                    EventActionsList(evt, categories, subCategories, context, currentCountryCode)
                }
                if (evt.description.isNotEmpty()) {
                    EventDescription(evt)
                }
                if (evt.photos.isNotEmpty()) {
                    EventPhotosList(evt, context)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Confirmation de suppression
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_action_title)) },
            text = { Text(stringResource(R.string.dialog_delete_action_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvenement(
                            evenementId = evt.id,
                            onSuccess = {
                                showDeleteDialog = false
                                Toast.makeText(context, context.getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
                                onDeleteSuccess()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Erreur : $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
private fun EventHeader(evt: Evenement, currentCountryCode: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!evt.countryCode.isNullOrBlank() && evt.countryCode.lowercase() != currentCountryCode.lowercase()) {
                CountryBadge(countryCode = evt.countryCode, modifier = Modifier.padding(end = 12.dp))
            }
            Text(text = evt.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = formatDateString(evt.date), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun EventCostCard(evt: Evenement, categories: List<Category>, subCategories: List<SubCategory>, context: Context, currentCountryCode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Coût total", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    val currency = CountryHelper.getCurrencySymbol(evt.countryCode ?: "")
                    Text(text = String.format(Locale.FRANCE, "%.2f $currency", evt.cost), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            val rootCat = categories.find { it.key == evt.categoryKey }
            val rootSub = subCategories.find { it.key == evt.subCategoryKey }
            val rootCatName = if (rootCat != null) TranslationHelper.getCategoryName(context, rootCat.key, rootCat.name) else evt.categoryKey
            val rootSubName = if (rootSub != null) TranslationHelper.getDecoratedSubCategoryName(context, rootSub.key, rootSub.name, currentCountryCode, evt.countryCode) else evt.subCategoryKey
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = CategoryIconMapper.getIcon(rootCat?.icon ?: "ic_maintenance"), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "$rootCatName • $rootSubName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EventDescription(evt: Evenement) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.action_notes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(text = evt.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}

@Composable
private fun EventActionsList(evt: Evenement, categories: List<Category>, subCategories: List<SubCategory>, context: Context, currentCountryCode: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.action_ops_detail), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        evt.actions.forEach { action ->
            val cat = categories.find { it.key == action.categoryKey }
            val sub = subCategories.find { it.key == action.subCategoryKey }
            val aCatName = if (cat != null) TranslationHelper.getCategoryName(context, cat.key, cat.name) else action.categoryKey
            val displayName = action.name.ifBlank { sub?.name ?: action.subCategoryKey }
            val aSubName = TranslationHelper.getDecoratedSubCategoryName(context, action.subCategoryKey, displayName, currentCountryCode, action.countryCode ?: evt.countryCode)
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                if (action.name.isNotBlank()) {
                    Text(text = aSubName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = CategoryIconMapper.getIcon(cat?.icon ?: "ic_maintenance"), contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    val catSubLabel = if (cat != null && sub != null) {
                        val tCat = TranslationHelper.getCategoryName(context, cat.key, cat.name)
                        val tSub = TranslationHelper.getSubCategoryName(context, sub.key, sub.name)
                        "$tCat • $tSub"
                    } else "$aCatName • ${action.subCategoryKey}"
                    Text(text = catSubLabel, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EventPhotosList(evt: Evenement, context: Context) {
    Column {
        Text(stringResource(R.string.action_assoc_docs), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.msg_tap_to_open), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(evt.photos) { attachment ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
                    val absolutePath = FileHelper.toAbsolutePath(context, attachment.path)
                    Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { FileHelper.openFile(context, attachment.path) }, contentAlignment = Alignment.Center) {
                        if (FileHelper.isImage(attachment.path)) {
                            AsyncImage(model = File(absolutePath), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        } else {
                            Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(imageVector = FileHelper.getIconForFile(attachment.path), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = File(attachment.path).name.substringAfterLast('_'), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    val displayLabel = attachment.label.ifBlank { File(attachment.path).name.substringAfterLast('_') }
                    Text(text = displayLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
