package com.roadscript.oss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import androidx.compose.ui.draw.scale
import com.roadscript.oss.R
import com.roadscript.oss.data.models.Attachment
import com.roadscript.oss.data.models.Category
import com.roadscript.oss.data.models.FuelReading
import com.roadscript.oss.ui.theme.AppTheme
import com.roadscript.oss.ui.utils.FileHelper
import com.roadscript.oss.ui.utils.CountryHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DocumentThumbnail(
    attachment: Attachment, 
    onDelete: () -> Unit,
    onEditLabel: (String) -> Unit,
) {
    val context = LocalContext.current
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    
    // Tailles agrandies pour smartphones
    val boxSize = 180.dp
    val iconSize = 48.dp
    val titleFontSize = 14.sp
    val actionButtonSize = 32.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { FileHelper.openFile(context, attachment.path) }
        ) {
            val absolutePath = FileHelper.toAbsolutePath(context, attachment.path)
            if (FileHelper.isImage(attachment.path)) {
                AsyncImage(
                    model = File(absolutePath), 
                    contentDescription = null, 
                    modifier = Modifier.fillMaxSize(), 
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = FileHelper.getIconForFile(attachment.path), 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(iconSize)
                    )
                    Text(
                        text = File(attachment.path).name.substringAfterLast('_'), 
                        fontSize = 12.sp, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis, 
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Bouton éditer label (crayon)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(actionButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    .clickable { showEditDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }

            // Bouton supprimer (croix rouge)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(actionButtonSize)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.9f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        
        val displayLabel = attachment.label.ifBlank { 
            File(attachment.path).name.substringAfterLast('_') 
        }
        
        Text(
            text = displayLabel,
            fontSize = titleFontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.width(boxSize).padding(top = 8.dp)
        )
    }

    if (showEditDialog) {
        LabelEditDialog(
            currentLabel = attachment.label.ifBlank { File(attachment.path).name.substringAfterLast('_') },
            onDismiss = { showEditDialog = false },
            onConfirm = { 
                onEditLabel(it)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun LabelEditDialog(
    currentLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le libellé") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Description du document") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.trim()) }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun PhotoSourceDialog(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un document") },
        text = { Text("Source du fichier (max 8)") },
        confirmButton = { TextButton(onClick = onCamera) { Text("Appareil photo") } },
        dismissButton = { TextButton(onClick = onGallery) { Text("Galerie / Fichiers") } }
    )
}

@Composable
fun AddMileageDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    val context = LocalContext.current
    var mileageStr by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dateStr by remember { mutableStateOf(sdf.format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un relevé kilométrique") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = mileageStr,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) mileageStr = it },
                    label = { Text("Kilométrage (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formatDateString(dateStr),
                    onValueChange = {},
                    label = { Text("Date du relevé") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            val parts = dateStr.split("-")
                            if (parts.size == 3) {
                                calendar.set(Calendar.YEAR, parts[0].toInt())
                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                            }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, dayOfMonth)
                                    dateStr = sdf.format(newCal.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Choisir date")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mileage = mileageStr.toIntOrNull()
                    if (mileage != null && dateStr.isNotEmpty()) {
                        onConfirm(mileage, dateStr)
                    }
                },
                enabled = mileageStr.isNotEmpty()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditMileageDialog(
    initialValue: Int,
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    val context = LocalContext.current
    var mileageStr by remember { mutableStateOf(initialValue.toString()) }
    var dateStr by remember { mutableStateOf(initialDate) }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le relevé") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formatDateString(dateStr),
                    onValueChange = {},
                    label = { Text("Date du relevé") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            val parts = dateStr.split("-")
                            if (parts.size == 3) {
                                calendar.set(Calendar.YEAR, parts[0].toInt())
                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                            }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, dayOfMonth)
                                    dateStr = sdf.format(newCal.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Choisir date")
                        }
                    }
                )

                OutlinedTextField(
                    value = mileageStr,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) mileageStr = it },
                    label = { Text("Nouveau kilométrage") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = mileageStr.toIntOrNull()
                    if (v != null) onConfirm(v, dateStr)
                },
                enabled = mileageStr.isNotEmpty()
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun AddFuelDialog(
    countryCode: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit
) {
    val currency = CountryHelper.getCurrencySymbol(countryCode)
    var litersStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dateStr by remember { mutableStateOf(sdf.format(Date())) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un plein") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = litersStr,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' || c == ',' }) litersStr = it.replace(',', '.') },
                    label = { Text("Volume (Litres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' || c == ',' }) costStr = it.replace(',', '.') },
                    label = { Text(stringResource(R.string.fuel_cost_label, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formatDateString(dateStr),
                    onValueChange = {},
                    label = { Text("Date du plein") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            try { calendar.time = sdf.parse(dateStr)!! } catch (e: Exception) {}
                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                val cal = Calendar.getInstance(); cal.set(y, m, d)
                                dateStr = sdf.format(cal.time)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }) { Icon(Icons.Default.DateRange, null) }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val l = litersStr.toDoubleOrNull() ?: 0.0
                    val c = costStr.toDoubleOrNull() ?: 0.0
                    if (l > 0 && c > 0) onConfirm(dateStr, l, c)
                },
                enabled = litersStr.isNotEmpty() && costStr.isNotEmpty()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun EditFuelDialog(
    initialReading: FuelReading,
    countryCode: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit
) {
    val currency = CountryHelper.getCurrencySymbol(countryCode)
    var litersStr by remember { mutableStateOf(initialReading.liters.toString()) }
    var costStr by remember { mutableStateOf(initialReading.cost.toString()) }
    var dateStr by remember { mutableStateOf(initialReading.date) }
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le plein") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = litersStr,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' || c == ',' }) litersStr = it.replace(',', '.') },
                    label = { Text("Volume (Litres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' || c == ',' }) costStr = it.replace(',', '.') },
                    label = { Text(stringResource(R.string.fuel_cost_label, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formatDateString(dateStr),
                    onValueChange = {},
                    label = { Text("Date du plein") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            try { calendar.time = sdf.parse(dateStr)!! } catch (e: Exception) {}
                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                val cal = Calendar.getInstance(); cal.set(y, m, d)
                                dateStr = sdf.format(cal.time)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }) { Icon(Icons.Default.DateRange, null) }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val l = litersStr.toDoubleOrNull() ?: 0.0
                    val c = costStr.toDoubleOrNull() ?: 0.0
                    if (l > 0 && c > 0) onConfirm(dateStr, l, c)
                },
                enabled = litersStr.isNotEmpty() && costStr.isNotEmpty()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

fun formatDateString(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
        val date = parser.parse(dateStr)
        val finalFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        if (date != null) finalFormatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun HistoryFilterDialog(
    availableYears: List<Int>,
    categories: List<Category>,
    initialSelectedFilters: Set<String>,
    initialSelectedYears: Set<Int>,
    onDismiss: () -> Unit,
    onApply: (Set<String>, Set<Int>) -> Unit,
    onReset: () -> Pair<Set<String>, Set<Int>>
) {
    var tempFilters by remember { mutableStateOf(initialSelectedFilters) }
    var tempYears by remember { mutableStateOf(initialSelectedYears) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilterList, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Colonne Gauche : Années
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.filter_years), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (availableYears.isEmpty()) {
                            Text(stringResource(R.string.msg_no_data), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        } else {
                            availableYears.forEach { year ->
                                FilterCheckRow(
                                    label = year.toString(),
                                    isChecked = year in tempYears,
                                    onToggle = { 
                                        tempYears = if (year in tempYears) tempYears - year else tempYears + year 
                                    }
                                )
                            }
                        }
                    }

                    // Colonne Droite : Catégories
                    if (categories.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(stringResource(R.string.filter_categories), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            categories.forEach { cat ->
                                FilterCheckRow(
                                    label = cat.name,
                                    isChecked = cat.key in tempFilters,
                                    onToggle = { 
                                        tempFilters = if (cat.key in tempFilters) tempFilters - cat.key else tempFilters + cat.key 
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { 
                        val (filters, years) = onReset()
                        tempFilters = filters
                        tempYears = years
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_reset))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        confirmButton = {
            Button(onClick = { onApply(tempFilters, tempYears) }) {
                Text(stringResource(R.string.btn_validate))
            }
        }
    )
}

@Composable
fun FilterCheckRow(
    label: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked, 
            onCheckedChange = { onToggle() },
            modifier = Modifier.scale(0.85f)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IconSelectorDialog(
    selectedIconKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sélectionner une icône") },
        text = {
            Box(modifier = Modifier.size(300.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(CategoryIconMapper.availableIcons) { iconItem ->
                        val isSelected = iconItem.first == selectedIconKey
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { onSelect(iconItem.first) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(iconItem.first),
                                    contentDescription = iconItem.second,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = iconItem.second.split(" ")[0], // Afficher le premier mot
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (AppTheme) -> Unit
) {
    val themes = listOf(
        Triple(AppTheme.INDIGO, "Indigo", Color(0xFF6366F1)),
        Triple(AppTheme.BLUE, "Bleu Océan", Color(0xFF0EA5E9)),
        Triple(AppTheme.GREEN, "Vert Émeraude", Color(0xFF10B981)),
        Triple(AppTheme.RED, "Rouge Sport", Color(0xFFEF4444)),
        Triple(AppTheme.GREY, "Gris Ardoise", Color(0xFF64748B))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une couleur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (theme, name, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(name, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var brand by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var countryCode by rememberSaveable { mutableStateOf(CountryHelper.getDefaultCountryCode()) }
    var showCountryDialog by rememberSaveable { mutableStateOf(false) }

    val currentLocale = context.resources.configuration.locales[0]
    val countryOptions = remember(currentLocale) {
        CountryHelper.countryCodes.map { code ->
            code to java.util.Locale("", code).getDisplayCountry(currentLocale)
        }.sortedBy { it.second }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.veh_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Veuillez saisir les informations de base de votre nouveau véhicule. Tous les champs marqués d'une * sont obligatoires.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                val currentCountryName = countryOptions.find { it.first == countryCode }?.second ?: ""
                OutlinedTextField(
                    value = currentCountryName,
                    onValueChange = {},
                    label = { Text("Pays pour juridictions *") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showCountryDialog = true },
                    enabled = false,
                    trailingIcon = { IconButton(onClick = { showCountryDialog = true }) { Icon(Icons.Default.Public, null) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("${stringResource(R.string.veh_brand)} *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.veh_model_req)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(brand.trim(), model.trim(), countryCode) },
                enabled = countryCode.isNotBlank() && brand.isNotBlank() && model.isNotBlank()
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )

    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text("Choisir un pays") },
            text = {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(scrollState)) {
                    countryOptions.forEach { (code, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { countryCode = code; showCountryDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (code == countryCode), onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCountryDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}
