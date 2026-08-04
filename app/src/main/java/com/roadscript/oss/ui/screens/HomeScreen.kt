package com.roadscript.oss.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.roadscript.oss.R
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.roadscript.oss.data.models.Evenement
import com.roadscript.oss.data.models.MileageReading
import com.roadscript.oss.data.models.Vehicle
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.components.*
import com.roadscript.oss.ui.theme.AppTheme
import com.roadscript.oss.ui.theme.OrangeAccent
import com.roadscript.oss.ui.utils.FileHelper
import com.roadscript.oss.ui.utils.TranslationHelper
import com.roadscript.oss.ui.utils.CountryHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VehicleViewModel,
    isTablet: Boolean = false,
    onNavigateToProfile: () -> Unit,
    onNavigateToMechanic: () -> Unit,
    onNavigateToInsurance: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAddAction: () -> Unit,
    onNavigateToDetailAction: (String) -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToMileageHistory: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToFuelHistory: () -> Unit,
    onNavigateToPrint: () -> Unit,
) {
    val context = LocalContext.current
    val vehicle by viewModel.vehicle.collectAsState()
    val evenements by viewModel.evenements.collectAsState()
    val mileageHistory by viewModel.mileageReadings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val latestMileage by viewModel.latestMileage.collectAsState()
    val fuelReadings by viewModel.fuelReadings.collectAsState()
    val garage by viewModel.garage.collectAsState()
    val activeVehicleId by viewModel.activeVehicleId.collectAsState()
    val defaultYearsFilter by viewModel.defaultYearsFilter.collectAsState()
    val showMileageZone by viewModel.showMileageZone.collectAsState()
    val showFuelZone by viewModel.showFuelZone.collectAsState()
    val currentCountryCode = vehicle?.countryCode ?: ""
    val isPro by viewModel.isPro.collectAsState()
    val enableMultiVehicle by viewModel.enableMultiVehicle.collectAsState()

    var showMileageDialog by rememberSaveable { mutableStateOf(false) }
    var showFuelDialog by rememberSaveable { mutableStateOf(false) }
    var showGarageSheet by rememberSaveable { mutableStateOf(false) }
    var showAddVehicleDialog by rememberSaveable { mutableStateOf(false) }
    var showNewVehicleSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    // États de filtrage
    var selectedFilters by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedYears by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var currentSessionDefaultYears by rememberSaveable { mutableStateOf(-1) }

    // Initialisation des filtres au lancement ou changement de réglage
    LaunchedEffect(defaultYearsFilter) {
        if (currentSessionDefaultYears != defaultYearsFilter) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            selectedYears = when (defaultYearsFilter) {
                1 -> setOf(currentYear)
                2 -> setOf(currentYear, currentYear - 1)
                3 -> setOf(currentYear, currentYear - 1, currentYear - 2)
                4 -> (0..3).map { currentYear - it }.toSet()
                5 -> (0..4).map { currentYear - it }.toSet()
                else -> emptySet()
            }
            currentSessionDefaultYears = defaultYearsFilter
        }
    }

    val availableYears = remember(evenements) {
        evenements.map { it.date.take(4).toIntOrNull() ?: 0 }
            .filter { it > 0 }
            .distinct()
            .sortedDescending()
    }

    val filteredEvenements = remember(evenements, selectedFilters, selectedYears) {
        evenements.filter { evt ->
            val matchesCategory = selectedFilters.isEmpty() || evt.categoryKey in selectedFilters || evt.actions.any { it.categoryKey in selectedFilters }
            val matchesYear = selectedYears.isEmpty() || (evt.date.take(4).toIntOrNull() in selectedYears)
            matchesCategory && matchesYear
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_veh_info)) },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_mech_info)) },
                            leadingIcon = { Icon(Icons.Default.Build, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToMechanic()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_insur_info)) },
                            leadingIcon = { Icon(Icons.Default.Shield, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToInsurance()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_task_list)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToCategories()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_export_pack)) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToPrint()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_import_export)) },
                            leadingIcon = { Icon(Icons.Default.ImportExport, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToImportExport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_settings)) },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_about)) },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToAbout()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val context = LocalContext.current
                FloatingActionButton(
                    onClick = {
                        if (!isPro && evenements.size >= 10) {
                            android.widget.Toast.makeText(context, context.getString(R.string.err_freemium_limit), android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            onNavigateToAddAction()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter une action")
                }
            }
        }
    ) { paddingValues ->
        val contentPadding = if (isTablet) 24.dp else 16.dp
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                VehicleHeaderCard(
                    vehicle = vehicle,
                    latestMileage = latestMileage,
                    latestFuel = fuelReadings.firstOrNull(),
                    isTablet = isTablet,
                    enableSelector = enableMultiVehicle,
                    onClickEdit = onNavigateToProfile,
                    onOpenGarage = { showGarageSheet = true }
                )
            }

            if (isTablet) {
                // Sur tablette, on met les sections techniques côte à côte
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (showMileageZone) {
                                MileageSection(
                                    latestMileage = latestMileage,
                                    mileageHistory = mileageHistory,
                                    vehicle = vehicle,
                                    isTablet = true,
                                    onNavigateToMileageHistory = onNavigateToMileageHistory,
                                    onAddReading = { showMileageDialog = true }
                                )
                            } else {
                                SimplifiedAddMileageRow(onAddReading = { showMileageDialog = true })
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (showFuelZone) {
                                FuelSection(
                                    fuelReadings = fuelReadings,
                                    vehicle = vehicle,
                                    isTablet = true,
                                    onNavigateToFuelHistory = onNavigateToFuelHistory,
                                    onAddReading = { showFuelDialog = true }
                                )
                            } else {
                                SimplifiedAddFuelRow(onAddReading = { showFuelDialog = true })
                            }
                        }
                    }
                }
            } else {
                // Sur mobile, on garde l'empilement vertical
                item {
                    if (showMileageZone) {
                        MileageSection(
                            latestMileage = latestMileage,
                            mileageHistory = mileageHistory,
                            vehicle = vehicle,
                            isTablet = false,
                            onNavigateToMileageHistory = onNavigateToMileageHistory,
                            onAddReading = { showMileageDialog = true }
                        )
                    } else {
                        SimplifiedAddMileageRow(onAddReading = { showMileageDialog = true })
                    }
                }

                item {
                    if (showFuelZone) {
                        FuelSection(
                            fuelReadings = fuelReadings,
                            vehicle = vehicle,
                            isTablet = false,
                            onNavigateToFuelHistory = onNavigateToFuelHistory,
                            onAddReading = { showFuelDialog = true }
                        )
                    } else {
                        SimplifiedAddFuelRow(onAddReading = { showFuelDialog = true })
                    }
                }
            }

            item {
                ExpensesSection(
                    evenements = evenements,
                    fuelReadings = fuelReadings,
                    vehicle = vehicle,
                    onNavigateToExpenses = onNavigateToExpenses
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.section_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                Icons.Default.FilterList, 
                                contentDescription = "Filtrer",
                                tint = if (selectedFilters.isNotEmpty() || selectedYears.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            if (filteredEvenements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Aucun événement enregistré",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                val itemsPerRow = if (isTablet) 2 else 1
                val chunkedEvents = filteredEvenements.chunked(itemsPerRow)
                
                items(chunkedEvents) { rowEvents ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowEvents.forEach { evt ->
                            val context = LocalContext.current
                            val category = categories.find { it.key == evt.categoryKey }
                            val subCategory = subCategories.find { it.key == evt.subCategoryKey }
                            
                            val categoryName = if (category != null) {
                                TranslationHelper.getCategoryName(context, category.key, category.name)
                            } else evt.categoryKey

                            val actionSummary = if (evt.isMultiple && evt.actions.isNotEmpty()) {
                                evt.actions.joinToString("\n") { action ->
                                    val translatedSub = TranslationHelper.getDecoratedSubCategoryName(context, action.subCategoryKey, action.name, currentCountryCode, action.countryCode ?: evt.countryCode)
                                    "- $translatedSub" 
                                }
                            } else {
                                val translatedSub = TranslationHelper.getDecoratedSubCategoryName(context, evt.subCategoryKey, subCategory?.name ?: evt.subCategoryKey, currentCountryCode, evt.countryCode)
                                "- $translatedSub"
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                ActionItemCard(
                                    action = evt,
                                    categoryName = categoryName,
                                    subCategoryName = actionSummary,
                                    categoryIcon = category?.icon ?: "ic_maintenance",
                                    categoryKey = evt.categoryKey,
                                    currentCountryCode = currentCountryCode,
                                    onClick = { onNavigateToDetailAction(evt.id) }
                                )
                            }
                        }
                        if (isTablet && rowEvents.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showFilterDialog) {
        HistoryFilterDialog(
            availableYears = availableYears,
            categories = categories,
            initialSelectedFilters = selectedFilters,
            initialSelectedYears = selectedYears,
            onDismiss = { showFilterDialog = false },
            onApply = { filters, years ->
                selectedFilters = filters
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

    if (showMileageDialog) {
        AddMileageDialog(
            onDismiss = { showMileageDialog = false },
            onConfirm = { value, date ->
                    viewModel.addMileageReading(
                        value = value,
                        date = date,
                        onSuccess = { showMileageDialog = false },
                        onError = { /* Gérer l'erreur */ }
                    )
            }
        )
    }

    if (showFuelDialog) {
        AddFuelDialog(
            countryCode = vehicle?.countryCode ?: "",
            onDismiss = { showFuelDialog = false },
            onConfirm = { date, liters, cost ->
                viewModel.addFuelReading(date, liters, cost,
                    onSuccess = { showFuelDialog = false },
                    onError = { /* Gérer l'erreur */ }
                )
            }
        )
    }

    if (showGarageSheet) {
        GarageTopSheet(
            garage = garage,
            activeVehicleId = activeVehicleId,
            isPro = isPro,
            onDismiss = { showGarageSheet = false },
            onSelectVehicle = { id -> viewModel.switchVehicle(id) },
            onAddVehicle = { 
                showAddVehicleDialog = true
            },
            onDeleteVehicle = { id -> viewModel.deleteVehicle(id) }
        )
    }

    if (showAddVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { brand, model, countryCode ->
                showAddVehicleDialog = false
                viewModel.createNewVehicle(brand, model, countryCode) {
                    showNewVehicleSuccessDialog = true
                }
            }
        )
    }

    if (showNewVehicleSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Bloquant */ },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.msg_new_vehicle_success_title), fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(stringResource(R.string.msg_new_vehicle_success_body)) },
            confirmButton = {
                Button(
                    onClick = { showNewVehicleSuccessDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun VehicleHeaderCard(
    vehicle: Vehicle?,
    latestMileage: MileageReading?,
    latestFuel: com.roadscript.oss.data.models.FuelReading?,
    isTablet: Boolean = false,
    enableSelector: Boolean = false,
    onClickEdit: () -> Unit,
    onOpenGarage: () -> Unit
) {
    val context = LocalContext.current
    val modelFontSize = if (isTablet) 20.sp else 18.sp
    val subFontSize = if (isTablet) 14.sp else 13.sp
    
    val age = remember(vehicle?.firstRegistrationDate) {
        val regDate = vehicle?.firstRegistrationDate
        if (!regDate.isNullOrBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                val birthDate = sdf.parse(regDate)
                if (birthDate != null) {
                    val birthCalendar = Calendar.getInstance().apply { time = birthDate }
                    val today = Calendar.getInstance()
                    var ageYears = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                        ageYears--
                    }
                    if (ageYears >= 0) "$ageYears ans" else null
                } else null
            } catch (_: Exception) { null }
        } else null
    }

    val kmFormatted = remember(latestMileage) {
        latestMileage?.let {
            val valueStr = String.format(Locale.FRANCE, "%,d", it.value).replace(',', ' ')
            val dateShort = try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                val d = parser.parse(it.date)
                if (d != null) {
                    SimpleDateFormat("dd/MM/yy", Locale.FRANCE).format(d)
                } else it.date
            } catch (_: Exception) { it.date }
            "$valueStr km - $dateShort"
        }
    }

    val fuelFormatted = remember(latestFuel, vehicle?.countryCode) {
        latestFuel?.let {
            val dateShort = try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                val d = parser.parse(it.date)
                if (d != null) {
                    SimpleDateFormat("dd/MM/yy", Locale.FRANCE).format(d)
                } else it.date
            } catch (_: Exception) { it.date }
            val currency = CountryHelper.getCurrencySymbol(vehicle?.countryCode ?: "")
            "$dateShort - ${String.format(Locale.FRANCE, "%.0f", it.liters)} L / ${String.format(Locale.FRANCE, "%.2f", it.cost)} $currency"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (enableSelector) onOpenGarage() else onClickEdit() 
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                                OrangeAccent.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasPhoto = vehicle?.photoPath != null && vehicle.photoPath.isNotEmpty()
                if (hasPhoto) {
                    val absolutePath = FileHelper.toAbsolutePath(context, vehicle!!.photoPath)
                    val file = File(absolutePath)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Photo véhicule",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = vehicle?.model ?: "Nouveau véhicule",
                            fontSize = modelFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (enableSelector) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (!vehicle?.brand.isNullOrBlank()) {
                        Text(
                            text = vehicle?.brand ?: "",
                            fontSize = subFontSize,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ligne 1 : Age et Badge (Age à gauche)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (age != null) {
                            Text(
                                text = age,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        if (!vehicle?.fuelType.isNullOrBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = vehicle!!.fuelType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    // Ligne 2 : Kilomètres
                    if (kmFormatted != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = kmFormatted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Ligne 3 : Carburant
                    if (fuelFormatted != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = fuelFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesSection(
    evenements: List<com.roadscript.oss.data.models.Evenement>,
    fuelReadings: List<com.roadscript.oss.data.models.FuelReading>,
    vehicle: Vehicle?,
    onNavigateToExpenses: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_expenses),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.btn_stats), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                IconButton(
                    onClick = onNavigateToExpenses,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Stats Dépenses",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val statsText = remember(evenements, fuelReadings, vehicle) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val allDates = (evenements.map { it.date } + fuelReadings.map { it.date })
                .mapNotNull { try { sdf.parse(it)?.time } catch(e: Exception) { null } }
            
            val originTime = allDates.minOrNull()

            if (vehicle != null && originTime != null) {
                try {
                    val diffMs = System.currentTimeMillis() - originTime
                    val days = (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                    val years = days.toDouble() / 365.25

                    val groups = mutableMapOf<String, Double>()
                    fun add(c: String?, v: Double) {
                        val s = CountryHelper.getCurrencySymbol(c ?: vehicle.countryCode)
                        groups[s] = (groups[s] ?: 0.0) + v
                    }
                    evenements.forEach { add(it.countryCode, it.cost) }
                    fuelReadings.forEach { add(it.countryCode, it.cost) }

                    val totalParts = groups.map { (s, v) -> "${String.format(Locale.FRANCE, "%,.0f", v).replace(',', ' ')} $s" }
                    val totalStr = totalParts.joinToString(" + ")

                    // Moyenne basée sur la devise principale du véhicule pour la simplicité
                    val mainCurrency = CountryHelper.getCurrencySymbol(vehicle.countryCode)
                    val mainTotal = groups[mainCurrency] ?: 0.0
                    val annualAvg = (mainTotal / years).toInt().coerceAtLeast(0)
                    
                    "Total : $totalStr (Moyenne : ${String.format(Locale.FRANCE, "%,d", annualAvg).replace(',', ' ')} $mainCurrency/an)"
                } catch (e: Exception) { "Erreur de calcul" }
            } else {
                "aucune dépense enregistrée"
            }
        }

        Text(
            text = statsText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun FuelSection(
    fuelReadings: List<com.roadscript.oss.data.models.FuelReading>,
    vehicle: Vehicle?,
    isTablet: Boolean = false,
    onNavigateToFuelHistory: () -> Unit,
    onAddReading: () -> Unit
) {
    var isChartView by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_fuel),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
// ... (rest stays similar, passing isTablet to chart)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bouton Carte / Stats (Désactivé si moins de 2 pleins)
                val canShowStats = fuelReadings.size >= 2
                val toggleLabel = if (isChartView) stringResource(R.string.btn_stats) else stringResource(R.string.btn_chart)
                val toggleIcon = if (isChartView) Icons.Default.BarChart else Icons.AutoMirrored.Filled.ShowChart

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = canShowStats) { isChartView = !isChartView }
                ) {
                    Text(
                        text = toggleLabel, 
                        fontSize = 12.sp, 
                        color = if (canShowStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    IconButton(
                        onClick = { isChartView = !isChartView },
                        modifier = Modifier.size(24.dp),
                        enabled = canShowStats
                    ) {
                        Icon(
                            imageVector = toggleIcon,
                            contentDescription = toggleLabel,
                            tint = if (canShowStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToFuelHistory() }
                ) {
                    Text(stringResource(R.string.btn_list), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(
                        onClick = onNavigateToFuelHistory,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.btn_list),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAddReading() }
                ) {
                    Text(stringResource(R.string.btn_add), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(
                        onClick = onAddReading,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = stringResource(R.string.btn_add),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = isChartView,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "FuelTransition"
        ) { chartVisible ->
            if (chartVisible) {
                FuelChart(fuelReadings, vehicle?.countryCode ?: "", isTablet)
            } else {
                val statsText = remember(fuelReadings, vehicle) {
                    val oldestFuel = fuelReadings.lastOrNull()
                    if (vehicle != null && oldestFuel != null) {
                        val totalLiters = fuelReadings.sumOf { it.liters }
                        val totalCost = fuelReadings.sumOf { it.cost }
                        
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                        val annualAvgText = try {
                            val originDate = sdf.parse(oldestFuel.date)
                            if (originDate != null) {
                                val diffMs = System.currentTimeMillis() - originDate.time
                                val days = (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                                val years = days.toDouble() / 365.25
                                
                                // Règle "Plein à plein" : le dernier montant n'est pas encore consommé
                                val consumedCost = if (fuelReadings.size > 1) {
                                    fuelReadings.drop(1).sumOf { it.cost }
                                } else {
                                    0.0
                                }
                                
                                val avg = (consumedCost / years).toInt().coerceAtLeast(0)
                                val currency = CountryHelper.getCurrencySymbol(vehicle?.countryCode ?: "")
                                " (Moyenne : ${String.format(Locale.FRANCE, "%,d", avg).replace(',', ' ')} $currency/an)"
                            } else ""
                        } catch (_: Exception) { "" }

                        val currency = CountryHelper.getCurrencySymbol(vehicle?.countryCode ?: "")
                        "Volume total : ${String.format(Locale.FRANCE, "%,.1f", totalLiters)} L - ${String.format(Locale.FRANCE, "%,.2f", totalCost)} $currency$annualAvgText"
                    } else {
                        "aucun plein enregistré"
                    }
                }

                Text(
                    text = statsText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FuelChart(history: List<com.roadscript.oss.data.models.FuelReading>, countryCode: String, isTablet: Boolean = false) {
    val oldestFuel = history.lastOrNull()
    if (oldestFuel == null) return

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
    
    // 1. Préparer les données (Jours, Prix au litre)
    val points = remember(history) {
        try {
            val oldestDate = sdf.parse(oldestFuel.date)
            val originTime = oldestDate?.time ?: 0L
            val list = mutableListOf<Offset>()
            history.forEach { reading ->
                val time = sdf.parse(reading.date)?.time ?: originTime
                val days = ((time - originTime) / (1000 * 60 * 60 * 24)).toFloat()
                val pricePerLiter = if (reading.liters > 0) reading.cost / reading.liters else 0.0
                list.add(Offset(days, pricePerLiter.toFloat()))
            }
            list.sortedBy { it.x }
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (points.size < 2) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Données insuffisantes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // 2. Échelle adaptative
    val totalDays = points.last().x
    val dpPerDay = if (isTablet) 28.8.dp else 14.4.dp // On double l'échelle sur tablette
    val chartWidthDp = (totalDays * dpPerDay.value).dp + 64.dp

    val scrollState = rememberScrollState()
    LaunchedEffect(points) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidthDp)
                    .fillMaxHeight()
                    .padding(top = 40.dp, bottom = 24.dp, start = 32.dp, end = 32.dp)
            ) {
                val height = size.height
                val minPrice = points.minOf { it.y } * 0.95f
                val maxPrice = points.maxOf { it.y } * 1.05f
                val rangeY = (maxPrice - minPrice).coerceAtLeast(0.1f)
                
                val scaleY = height / rangeY
                val scaleX = with(density) { dpPerDay.toPx() }

                fun mapToPx(point: Offset): Offset {
                    val x = point.x * scaleX
                    val y = height - ((point.y - minPrice) * scaleY)
                    return Offset(x, y)
                }

                // Axes
                drawLine(outlineColor, Offset(0f, height), Offset(size.width, height), strokeWidth = 1.dp.toPx())
                drawLine(outlineColor, Offset(0f, 0f), Offset(0f, height), strokeWidth = 1.dp.toPx())

                // Repères Mensuels (AAMM)
                try {
                    val originDate = sdf.parse(oldestFuel.date)!!
                    val calendar = Calendar.getInstance().apply { time = originDate }
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    
                    val lastPointDays = points.last().x.toLong()
                    val endTimeMs = originDate.time + (lastPointDays * 24 * 60 * 60 * 1000)
                    
                    while (calendar.timeInMillis <= endTimeMs) {
                        val daysSinceOrigin = (calendar.timeInMillis - originDate.time) / (1000 * 60 * 60 * 24)
                        val tickPx = daysSinceOrigin * scaleX
                        if (tickPx >= 0) {
                            drawLine(outlineColor, Offset(tickPx, height), Offset(tickPx, height + 6.dp.toPx()), 1.dp.toPx())
                            val year = calendar.get(Calendar.YEAR) % 100
                            val month = calendar.get(Calendar.MONTH) + 1
                            val label = String.format(Locale.FRANCE, "%02d/%02d", year, month)
                            val textLayout = textMeasurer.measure(label, TextStyle(fontSize = 11.sp, color = onSurfaceColor.copy(alpha = 0.5f)))
                            drawText(textLayout, topLeft = Offset(tickPx - textLayout.size.width / 2, height + 8.dp.toPx()))
                        }
                        calendar.add(Calendar.MONTH, 1)
                    }
                } catch (e: Exception) {}

                // Ligne
                val path = Path()
                points.forEachIndexed { index, p ->
                    val px = mapToPx(p)
                    if (index == 0) path.moveTo(px.x, px.y) else path.lineTo(px.x, px.y)
                }
                drawPath(path, primaryColor, style = Stroke(width = 2.dp.toPx()))

                // Points et Bulles
                points.forEach { p ->
                    val px = mapToPx(p)
                    drawCircle(primaryColor, radius = 4.dp.toPx(), center = px)
                    drawCircle(surfaceColor, radius = 2.dp.toPx(), center = px)

                    val dateStr = try {
                        val timeMs = sdf.parse(oldestFuel.date)!!.time + (p.x.toLong() * 24 * 60 * 60 * 1000)
                        SimpleDateFormat("dd/MM/yy", Locale.FRANCE).format(Date(timeMs))
                    } catch(e: Exception) { "" }
                    
                    val currency = CountryHelper.getCurrencySymbol(countryCode)
                    val fullLabel = "$dateStr\n${String.format(Locale.FRANCE, "%,.3f $currency/L", p.y)}"
                    val textLayout = textMeasurer.measure(fullLabel, TextStyle(fontSize = 12.sp, color = onSurfaceColor, fontWeight = FontWeight.Bold))
                    
                    val bubbleWidth = textLayout.size.width + 12.dp.toPx()
                    val bubbleHeight = textLayout.size.height + 6.dp.toPx()
                    val bubbleOffset = Offset(px.x - bubbleWidth / 2, px.y - bubbleHeight - 8.dp.toPx())

                    drawRoundRect(
                        color = surfaceColor,
                        topLeft = bubbleOffset,
                        size = Size(bubbleWidth, bubbleHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Fill
                    )
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.3f),
                        topLeft = bubbleOffset,
                        size = Size(bubbleWidth, bubbleHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawText(textLayout, topLeft = Offset(bubbleOffset.x + 6.dp.toPx(), bubbleOffset.y + 3.dp.toPx()))
                }
            }
        }

        // Scrollbar
        if (scrollState.maxValue > 0) {
            val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue
            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp).padding(horizontal = 16.dp)) {
                val width = size.width
                val thumbWidth = width * 0.2f
                val xPos = (width - thumbWidth) * scrollRatio
                drawRoundRect(
                    color = outlineColor.copy(alpha = 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height/2),
                    style = Fill
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(xPos, 0f),
                    size = Size(thumbWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height/2),
                    style = Fill
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SimplifiedAddFuelRow(onAddReading: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddReading() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Ajouter un plein de carburant",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SimplifiedAddMileageRow(onAddReading: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddReading() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Ajouter un relevé kilométrique",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MileageSection(
    latestMileage: MileageReading?,
    mileageHistory: List<MileageReading>,
    vehicle: Vehicle?,
    isTablet: Boolean = false,
    onNavigateToMileageHistory: () -> Unit,
    onAddReading: () -> Unit
) {
    var isChartView by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_mileage),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val canShowStats = mileageHistory.size >= 2
                val toggleLabel = if (isChartView) stringResource(R.string.btn_stats) else stringResource(R.string.btn_chart)
                val toggleIcon = if (isChartView) Icons.Default.BarChart else Icons.AutoMirrored.Filled.ShowChart

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = canShowStats) { isChartView = !isChartView }
                ) {
                    if (!isTablet) {
                        Text(text = toggleLabel, fontSize = 12.sp, color = if (canShowStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                    IconButton(
                        onClick = { isChartView = !isChartView },
                        modifier = Modifier.size(24.dp),
                        enabled = canShowStats
                    ) {
                        Icon(
                            imageVector = toggleIcon,
                            contentDescription = toggleLabel,
                            tint = if (canShowStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToMileageHistory() }
                ) {
                    if (!isTablet) {
                        Text(stringResource(R.string.btn_list), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onNavigateToMileageHistory,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.btn_list),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAddReading() }
                ) {
                    if (!isTablet) {
                        Text(stringResource(R.string.btn_add), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onAddReading,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = stringResource(R.string.btn_add),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = isChartView,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "MileageTransition"
        ) { chartVisible ->
            if (chartVisible) {
                MileageChart(mileageHistory, vehicle, isTablet)
            } else {
                val statsText = remember(latestMileage, mileageHistory) {
                    val oldestMileage = mileageHistory.lastOrNull()
                    if (latestMileage != null && oldestMileage != null) {
                        val totalDiff = latestMileage.value - oldestMileage.value
                        
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                        val originDateStr = oldestMileage.date
                        
                        val annualAvgText = try {
                            val originDate = sdf.parse(originDateStr)
                            if (originDate != null) {
                                val diffMs = System.currentTimeMillis() - originDate.time
                                val days = (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                                val years = days.toDouble() / 365.25
                                val avg = (totalDiff / years).toInt().coerceAtLeast(0)
                                " (Moyenne : ${String.format(Locale.FRANCE, "%,d", avg).replace(',', ' ')} km/an)"
                            } else ""
                        } catch (_: Exception) { "" }

                        "Parcourus : ${String.format(Locale.FRANCE, "%,d", totalDiff).replace(',', ' ')} km$annualAvgText"
                    } else {
                        "aucun relevé enregistré"
                    }
                }

                Text(
                    text = statsText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MileageChart(history: List<MileageReading>, vehicle: Vehicle?, isTablet: Boolean = false) {
    val registrationDateStr = vehicle?.firstRegistrationDate ?: ""
    val acquisitionDateStr = vehicle?.acquisitionDate ?: ""
    val acquisitionMileage = vehicle?.acquisitionMileage ?: 0
    
    val originDateStr = acquisitionDateStr.ifBlank { registrationDateStr }

    if (originDateStr.isBlank()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Date d'achat ou d'immat. requise pour le graphique", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
    
    val points = remember(history, originDateStr, acquisitionMileage) {
        try {
            val originTime = sdf.parse(originDateStr)?.time ?: 0L
            val list = mutableListOf(Offset(0f, acquisitionMileage.toFloat()))
            history.forEach { reading ->
                val time = sdf.parse(reading.date)?.time ?: originTime
                val days = ((time - originTime) / (1000 * 60 * 60 * 24)).toFloat()
                if (days >= 0) {
                    list.add(Offset(days, reading.value.toFloat()))
                }
            }
            list.distinctBy { it.x }.sortedBy { it.x }
        } catch (e: Exception) {
            listOf(Offset(0f, acquisitionMileage.toFloat()))
        }
    }

    if (points.size < 2) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Données insuffisantes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val dpPerDay = if (isTablet) 7.2.dp else 3.6.dp // On double l'échelle sur tablette
    val totalDays = points.last().x
    val chartWidthDp = (totalDays * dpPerDay.value).dp + 64.dp

    val scrollState = rememberScrollState()
    
    LaunchedEffect(points) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Zone du Graphique Défilante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp) // Un peu plus haut pour les labels AAMM
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidthDp)
                    .fillMaxHeight()
                    .padding(top = 40.dp, bottom = 24.dp, start = 32.dp, end = 32.dp)
            ) {
                val height = size.height
                val maxKm = points.maxOf { it.y }.coerceAtLeast(100f)
                val scaleY = height / (maxKm * 1.1f)
                val scaleX = with(density) { dpPerDay.toPx() }

                fun mapToPx(point: Offset): Offset {
                    val x = point.x * scaleX
                    val y = height - (point.y * scaleY)
                    return Offset(x, y)
                }

                // --- Dessin des Axes ---
                drawLine(outlineColor, Offset(0f, height), Offset(size.width, height), strokeWidth = 1.dp.toPx())
                drawLine(outlineColor, Offset(0f, 0f), Offset(0f, height), strokeWidth = 1.dp.toPx())

                // --- Dessin des Repères Mensuels (AAMM) ---
                try {
                    val originDate = sdf.parse(originDateStr)!!
                    val calendar = Calendar.getInstance().apply { time = originDate }
                    // Commencer au début du mois suivant ou du mois actuel
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    
                    val lastPointDays = points.last().x.toLong()
                    val endTimeMs = originDate.time + (lastPointDays * 24 * 60 * 60 * 1000)
                    
                    while (calendar.timeInMillis <= endTimeMs) {
                        val daysSinceOrigin = (calendar.timeInMillis - originDate.time) / (1000 * 60 * 60 * 24)
                        val tickPx = daysSinceOrigin * scaleX
                        
                        if (tickPx >= 0) {
                            // Petit trait vertical
                            drawLine(
                                color = outlineColor,
                                start = Offset(tickPx, height),
                                end = Offset(tickPx, height + 6.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            // Label AA / MM
                            val year = calendar.get(Calendar.YEAR) % 100
                            val month = calendar.get(Calendar.MONTH) + 1
                            val label = String.format(Locale.FRANCE, "%02d / %02d", year, month)
                            
                            val textLayout = textMeasurer.measure(
                                label,
                                style = TextStyle(fontSize = 12.8.sp, color = onSurfaceColor.copy(alpha = 0.5f))
                            )
                            drawText(
                                textLayout,
                                topLeft = Offset(tickPx - textLayout.size.width / 2, height + 8.dp.toPx())
                            )
                        }
                        calendar.add(Calendar.MONTH, 1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // --- Dessin de la Ligne ---
                val path = Path()
                points.forEachIndexed { index, p ->
                    val px = mapToPx(p)
                    if (index == 0) path.moveTo(px.x, px.y) else path.lineTo(px.x, px.y)
                }
                drawPath(path, primaryColor, style = Stroke(width = 2.dp.toPx()))

                // --- Dessin des Points et Bulles ---
                points.forEach { p ->
                    val px = mapToPx(p)
                    
                    drawCircle(primaryColor, radius = 4.dp.toPx(), center = px)
                    drawCircle(surfaceColor, radius = 2.dp.toPx(), center = px)

                    // Bulles d'infos
                    val dateStr = try {
                        val timeMs = sdf.parse(originDateStr)!!.time + (p.x.toLong() * 24 * 60 * 60 * 1000)
                        SimpleDateFormat("dd/MM/yy", Locale.FRANCE).format(Date(timeMs))
                    } catch(e: Exception) { "" }
                    
                    val kmLabel = "${p.y.toInt()} km"
                    val isStart = p.x == 0f
                    val fullLabel = if (isStart) "Achat\n$kmLabel" else "$dateStr\n$kmLabel"
                    
                    val textLayout = textMeasurer.measure(
                        fullLabel, 
                        style = TextStyle(fontSize = 14.4.sp, color = onSurfaceColor, fontWeight = FontWeight.Bold)
                    )
                    
                    val bubbleWidth = textLayout.size.width + 12.dp.toPx()
                    val bubbleHeight = textLayout.size.height + 6.dp.toPx()
                    val bubbleOffset = Offset(px.x - bubbleWidth / 2, px.y - bubbleHeight - 8.dp.toPx())

                    drawRoundRect(
                        color = surfaceColor,
                        topLeft = bubbleOffset,
                        size = Size(bubbleWidth, bubbleHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Fill
                    )
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.3f),
                        topLeft = bubbleOffset,
                        size = Size(bubbleWidth, bubbleHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    drawText(
                        textLayout,
                        topLeft = Offset(bubbleOffset.x + 6.dp.toPx(), bubbleOffset.y + 3.dp.toPx())
                    )
                }
            }
        }

        // Barre de défilement visuelle personnalisée
        if (scrollState.maxValue > 0) {
            val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue
            val thumbWidthRatio = 0.2f
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                // Utilisation de weight ou offset pour positionner le curseur
                // On va utiliser un Canvas simple pour dessiner le curseur afin d'éviter BoxWithConstraints
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val thumbWidth = width * thumbWidthRatio
                    val xPos = (width - thumbWidth) * scrollRatio
                    
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(xPos, 0f),
                        size = Size(thumbWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ActionItemCard(
    action: Evenement,
    categoryName: String,
    subCategoryName: String, // Contient maintenant la liste avec préfixes "- "
    categoryIcon: String,
    categoryKey: String,
    currentCountryCode: String,
    onClick: () -> Unit
) {
    val baseColor = MaterialTheme.colorScheme.primary
    val bgColor = remember(categoryKey, baseColor) { getCategoryColor(categoryKey, baseColor) }
    val contentColor = if (categoryKey == "depannage") Color.White else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 1. Partie Haute : Icône, Date, Nom, Prix
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône adaptée à la ligne
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (categoryKey == "depannage") Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconMapper.getIcon(categoryIcon),
                        contentDescription = null,
                        tint = if (categoryKey == "depannage") Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Date, Nom et Catégorie
                Column(modifier = Modifier.weight(1f)) {
                    // Ligne 1 : Date, Badge (si besoin) et Nom
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatDateString(action.date),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = contentColor
                        )
                        
                        // Badge Pays entre Date et Nom
                        val itemCountry = action.countryCode?.uppercase() ?: ""
                        val vehicleCountry = currentCountryCode.uppercase()
                        
                        if (itemCountry.isNotBlank() && itemCountry != vehicleCountry) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CountryBadge(countryCode = itemCountry)
                        }

                        Text(
                            text = " - ${action.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = contentColor
                        )
                    }
                    // Ligne 2 : Catégorie (plus petit)
                    Text(
                        text = categoryName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Prix
                val currency = CountryHelper.getCurrencySymbol(action.countryCode ?: "")
                Text(
                    text = String.format(Locale.FRANCE, "%.2f $currency", action.cost),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (categoryKey == "depannage") Color.White else MaterialTheme.colorScheme.primary
                )
            }

            // 2. Partie Basse : Libellés des actions (pleine largeur)
            if (subCategoryName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subCategoryName,
                    fontSize = 13.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

fun getCategoryColor(categoryKey: String, baseColor: Color): Color {
    return when (categoryKey) {
        "accessoire" -> baseColor.copy(alpha = 0.12f)
        "obligation" -> baseColor.copy(alpha = 0.40f)
        "entretien" -> baseColor.copy(alpha = 0.70f)
        "depannage" -> baseColor // Vif (Pleine couleur)
        else -> baseColor.copy(alpha = 0.70f) // Utilisateur normal
    }
}
