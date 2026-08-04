package com.roadscript.oss

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadscript.oss.data.repository.VehicleRepository
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.VehicleViewModelFactory
import com.roadscript.oss.ui.screens.*
import com.roadscript.oss.ui.theme.AppTheme
import com.roadscript.oss.ui.theme.SuiviVehiculeTheme

enum class Screen {
    Home,
    VehicleEdit,
    MechanicEdit,
    InsuranceEdit,
    ActionAddEdit,
    ActionDetail,
    Categories,
    ImportExport,
    About,
    MileageReadings,
    Expenses,
    FuelHistory,
    Settings,
    Print
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation de la couche données et du ViewModel
        val repository = VehicleRepository(applicationContext)
        val viewModelFactory = VehicleViewModelFactory(repository)

        setContent {
            val viewModel: VehicleViewModel by viewModels { viewModelFactory }
            val themeState by viewModel.currentTheme.collectAsState()
            val languageState by viewModel.currentLanguage.collectAsState()
            val isSetupComplete by viewModel.isSetupComplete.collectAsState()
            
            val appTheme = try { AppTheme.valueOf(themeState) } catch (e: Exception) { AppTheme.INDIGO }
            
            // Détection de la largeur d'écran pour l'adaptabilité (Compact vs Tablette)
            val screenWidthDp = LocalConfiguration.current.screenWidthDp
            val isTablet = screenWidthDp >= 600

            // États de navigation persistants lors du changement de langue ou pays
            var currentScreen by rememberSaveable { mutableStateOf(Screen.Home) }
            var selectedActionId by rememberSaveable { mutableStateOf<String?>(null) }
            
            // État temporaire pour masquer le dialogue de bienvenue lors de la configuration
            var isWelcomeDialogSuppressed by rememberSaveable { mutableStateOf(false) }

            // On force une recomposition lors du changement de langue
            key(languageState) {
                SuiviVehiculeTheme(appTheme = appTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val vehicleState by viewModel.vehicle.collectAsState()

                        // Réinitialiser la suppression du dialogue si on revient à l'accueil sans avoir fini
                        LaunchedEffect(currentScreen) {
                            if (currentScreen == Screen.Home) {
                                isWelcomeDialogSuppressed = false
                            }
                        }

                        // Navigation
                        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                            when (screen) {
                                Screen.Home -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        isTablet = isTablet,
                                        onNavigateToProfile = { currentScreen = Screen.VehicleEdit },
                                        onNavigateToCategories = { currentScreen = Screen.Categories },
                                        onNavigateToMechanic = { currentScreen = Screen.MechanicEdit },
                                        onNavigateToInsurance = { currentScreen = Screen.InsuranceEdit },
                                        onNavigateToAddAction = {
                                            selectedActionId = null
                                            currentScreen = Screen.ActionAddEdit
                                        },
                                        onNavigateToDetailAction = { actionId ->
                                            selectedActionId = actionId
                                            currentScreen = Screen.ActionDetail
                                        },
                                        onNavigateToImportExport = { currentScreen = Screen.ImportExport },
                                        onNavigateToAbout = { currentScreen = Screen.About },
                                        onNavigateToSettings = { currentScreen = Screen.Settings },
                                        onNavigateToMileageHistory = { currentScreen = Screen.MileageReadings },
                                        onNavigateToExpenses = { currentScreen = Screen.Expenses },
                                        onNavigateToFuelHistory = { currentScreen = Screen.FuelHistory },
                                        onNavigateToPrint = { currentScreen = Screen.Print }
                                    )
                                }
                                Screen.VehicleEdit -> {
                                    VehicleEditScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home },
                                        onSaveSuccess = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.MechanicEdit -> {
                                    MechanicEditScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home },
                                        onSaveSuccess = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.InsuranceEdit -> {
                                    InsuranceEditScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home },
                                        onSaveSuccess = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.ActionAddEdit -> {
                                    ActionEditScreen(
                                        viewModel = viewModel,
                                        actionId = selectedActionId,
                                        onNavigateBack = {
                                            if (selectedActionId != null) {
                                                currentScreen = Screen.ActionDetail
                                            } else {
                                                currentScreen = Screen.Home
                                            }
                                        },
                                        onSaveSuccess = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.ActionDetail -> {
                                    ActionDetailScreen(
                                        viewModel = viewModel,
                                        actionId = selectedActionId ?: "",
                                        isTablet = isTablet,
                                        onNavigateBack = { currentScreen = Screen.Home },
                                        onNavigateToEdit = { actionId ->
                                            selectedActionId = actionId
                                            currentScreen = Screen.ActionAddEdit
                                        },
                                        onDeleteSuccess = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.Categories -> {
                                    CategoriesScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.ImportExport -> {
                                    ImportExportScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home },
                                        onNavigateToAbout = { currentScreen = Screen.About }
                                    )
                                }
                                Screen.About -> {
                                    AboutScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.MileageReadings -> {
                                    MileageReadingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.Expenses -> {
                                    ExpensesScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.FuelHistory -> {
                                    FuelReadingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home }
                                    )
                                }
                                Screen.Settings -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home },
                                        onNavigateToExport = { currentScreen = Screen.ImportExport },
                                        onResetComplete = { currentScreen = Screen.Home } // Sera redirigé par LaunchedEffect
                                    )
                                }
                                Screen.Print -> {
                                    PrintScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.Home }
                                    )
                                }
                            }
                        }

                        // Fenêtre de Bienvenue & Configuration Initiale
                        if (!isSetupComplete && !isWelcomeDialogSuppressed) {
                            AlertDialog(
                                onDismissRequest = { /* Bloquant : pas de fermeture automatique */ },
                                title = { 
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(R.string.welcome_title), fontWeight = FontWeight.ExtraBold)
                                    }
                                },
                                text = { 
                                    Text(
                                        text = stringResource(R.string.welcome_msg),
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 22.sp
                                    ) 
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { 
                                            isWelcomeDialogSuppressed = true
                                            viewModel.completeSetup()
                                            currentScreen = Screen.VehicleEdit 
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_configure_country), fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
