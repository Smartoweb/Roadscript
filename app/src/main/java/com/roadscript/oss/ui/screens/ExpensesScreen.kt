package com.roadscript.oss.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.data.models.Evenement
import com.roadscript.oss.data.models.FuelReading
import com.roadscript.oss.data.models.MileageReading
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.CategoryIconMapper
import com.roadscript.oss.ui.components.HistoryFilterDialog
import com.roadscript.oss.ui.components.formatDateString
import com.roadscript.oss.ui.utils.TranslationHelper
import com.roadscript.oss.ui.utils.CountryHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    val evenements by viewModel.evenements.collectAsState()
    val fuelReadings by viewModel.fuelReadings.collectAsState()
    val mileageHistory by viewModel.mileageReadings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()

    val defaultYearsFilter by viewModel.defaultYearsFilter.collectAsState()

    // 1. Calcul des années disponibles
    val availableYears = remember(evenements, fuelReadings) {
        val years = mutableSetOf<Int>()
        evenements.forEach { it.date.take(4).toIntOrNull()?.let { y -> if (y > 0) years.add(y) } }
        fuelReadings.forEach { it.date.take(4).toIntOrNull()?.let { y -> if (y > 0) years.add(y) } }
        years.toList().sortedDescending()
    }

    // 2. Filtre par année
    var selectedYears by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    // Initialisation : toutes les années par défaut (liste vide = tout)
    val displayedYears = if (selectedYears.isEmpty()) availableYears.toSet() else selectedYears

    // 3. Filtrage des données
    val filteredEvts = remember(evenements, displayedYears) {
        evenements.filter { it.date.take(4).toIntOrNull() in displayedYears }
    }
    val filteredFuel = remember(fuelReadings, displayedYears) {
        fuelReadings.filter { it.date.take(4).toIntOrNull() in displayedYears }
    }
    val filteredKm = remember(mileageHistory, displayedYears) {
        mileageHistory.filter { it.date.take(4).toIntOrNull() in displayedYears }
    }

    // 4. Calculs de synthèse par devise
    val costsByCurrency = remember(filteredEvts, filteredFuel, vehicle) {
        val groups = mutableMapOf<String, Double>()
        
        fun addCost(countryCode: String?, cost: Double) {
            val symbol = CountryHelper.getCurrencySymbol(countryCode ?: vehicle?.countryCode ?: "")
            groups[symbol] = (groups[symbol] ?: 0.0) + cost
        }

        filteredEvts.forEach { addCost(it.countryCode, it.cost) }
        filteredFuel.forEach { addCost(it.countryCode, it.cost) }
        
        groups.toList().sortedByDescending { it.second }
    }

    val periodInfo = remember(filteredEvts, filteredFuel) {
        val allDates = (filteredEvts.map { it.date } + filteredFuel.map { it.date }).sorted()
        if (allDates.isEmpty()) ""
        else "${formatDateString(allDates.first())} - ${formatDateString(allDates.last())}"
    }

    val distanceTraveled = remember(filteredKm) {
        if (filteredKm.size < 2) 0
        else (filteredKm.maxOf { it.value } - filteredKm.minOf { it.value }).coerceAtLeast(0)
    }

    val rootCategories = listOf(
        "entretien" to stringResource(R.string.cat_service),
        "depannage" to stringResource(R.string.cat_repair),
        "obligation" to stringResource(R.string.cat_legal),
        "accessoire" to stringResource(R.string.cat_accessory)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = "Filtrer",
                            tint = if (selectedYears.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Synthèse
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.expenses_total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        
                        if (costsByCurrency.isEmpty()) {
                            Text(
                                text = String.format(Locale.FRANCE, "%,.2f %s", 0.0, CountryHelper.getCurrencySymbol(vehicle?.countryCode ?: "")),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            costsByCurrency.forEach { (symbol, total) ->
                                Text(
                                    text = String.format(Locale.FRANCE, "%,.2f %s", total, symbol).replace(',', ' '),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (periodInfo.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${stringResource(R.string.period_label)} $periodInfo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                        if (distanceTraveled > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Default.Speed, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${stringResource(R.string.dist_traveled)} ${String.format(Locale.FRANCE, "%,d km", distanceTraveled).replace(',', ' ')}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // Répartition par Catégorie
            item {
                Text(stringResource(R.string.split_by_post), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Carburant (Spécifique)
            if (filteredFuel.isNotEmpty()) {
                val fuelByCurrency = filteredFuel.groupBy { CountryHelper.getCurrencySymbol(it.countryCode ?: vehicle?.countryCode ?: "") }
                
                fuelByCurrency.forEach { (symbol, readings) ->
                    val fuelCost = readings.sumOf { it.cost }
                    if (fuelCost > 0) {
                        item {
                            val fuelLiters = readings.sumOf { it.liters }
                            val totalInCurrency = costsByCurrency.find { it.first == symbol }?.second ?: 1.0
                            val percentage = (fuelCost / totalInCurrency * 100).toInt()
                            
                            ExpenseCategoryCard(
                                title = "${stringResource(R.string.section_fuel)} ($symbol)",
                                icon = Icons.Default.LocalGasStation,
                                cost = fuelCost,
                                percentage = percentage,
                                color = Color(0xFFFB8C00), // Orange pétrole
                                currency = symbol
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DetailRow(stringResource(R.string.fuel_vol_total), String.format(Locale.FRANCE, "%,.1f L", fuelLiters))
                                    DetailRow(stringResource(R.string.fuel_avg_price), String.format(Locale.FRANCE, "%,3f $symbol/L", if (fuelLiters > 0) fuelCost / fuelLiters else 0.0))
                                    DetailRow(stringResource(R.string.fuel_count), readings.size.toString())
                                }
                            }
                        }
                    }
                }
            }

            // Autres catégories racines
            items(rootCategories) { (key, name) ->
                val evtsInCat = filteredEvts.filter { it.categoryKey == key }
                if (evtsInCat.isNotEmpty()) {
                    val catByCurrency = evtsInCat.groupBy { CountryHelper.getCurrencySymbol(it.countryCode ?: vehicle?.countryCode ?: "") }
                    
                    catByCurrency.forEach { (symbol, events) ->
                        val catCost = events.sumOf { it.cost }
                        if (catCost > 0) {
                            val totalInCurrency = costsByCurrency.find { it.first == symbol }?.second ?: 1.0
                            val percentage = (catCost / totalInCurrency * 100).toInt()
                            
                            ExpenseCategoryCard(
                                title = "$name ($symbol)",
                                icon = CategoryIconMapper.getIcon(getIconKeyForRoot(key)),
                                cost = catCost,
                                percentage = percentage,
                                color = getCategoryRootColor(key),
                                currency = symbol
                            ) {
                                // Détail par sous-catégories
                                val subGroups = events.groupBy { it.subCategoryKey }
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    subGroups.forEach { (subKey, evts) ->
                                        val subCost = evts.sumOf { it.cost }
                                        if (subCost > 0) {
                                            val subName = subCategories.find { it.key == subKey }?.name ?: subKey
                                            val translatedSub = TranslationHelper.getSubCategoryName(LocalContext.current, subKey, subName)
                                            DetailRow(translatedSub, String.format(Locale.FRANCE, "%,.2f $symbol", subCost))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showFilterDialog) {
        HistoryFilterDialog(
            availableYears = availableYears,
            categories = emptyList(), // On ne filtre que par année ici
            initialSelectedFilters = emptySet(),
            initialSelectedYears = selectedYears,
            onDismiss = { showFilterDialog = false },
            onApply = { _, years ->
                selectedYears = years
                showFilterDialog = false
            },
            onReset = {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val defaultYears = when (defaultYearsFilter) {
                    1 -> setOf(currentYear)
                    2 -> setOf(currentYear, currentYear - 1)
                    3 -> setOf(currentYear, currentYear - 1, currentYear - 2)
                    4 -> (0..3).map { currentYear - it }.toSet()
                    5 -> (0..4).map { currentYear - it }.toSet()
                    else -> emptySet()
                }
                Pair(emptySet<String>(), defaultYears)
            }
        )
    }
}

@Composable
fun ExpenseCategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cost: Double,
    percentage: Int,
    color: Color,
    currency: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { percentage.toFloat() / 100f },
                            modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = color.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$percentage%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Text(
                    text = String.format(Locale.FRANCE, "%,.2f $currency", cost).replace(',', ' '),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    content()
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

fun getIconKeyForRoot(key: String): String {
    return when(key) {
        "entretien" -> "ic_maintenance"
        "depannage" -> "ic_repair"
        "obligation" -> "ic_inspection"
        "accessoire" -> "ic_accessories"
        else -> "ic_maintenance"
    }
}

fun getCategoryRootColor(key: String): Color {
    return when(key) {
        "entretien" -> Color(0xFF3F51B5) // Indigo
        "depannage" -> Color(0xFFE91E63) // Pink
        "obligation" -> Color(0xFF4CAF50) // Green
        "accessoire" -> Color(0xFF9C27B0) // Purple
        else -> Color.Gray
    }
}
