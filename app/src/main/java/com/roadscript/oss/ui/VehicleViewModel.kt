package com.roadscript.oss.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roadscript.oss.R
import com.roadscript.oss.data.models.*
import com.roadscript.oss.data.xml.GlobalBackup
import com.roadscript.oss.data.repository.VehicleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class VehicleViewModel(val repository: VehicleRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.categories
    val subCategories: StateFlow<List<SubCategory>> = repository.subCategories
    val vehicle: StateFlow<Vehicle?> = repository.vehicle
    val mileageReadings: StateFlow<List<MileageReading>> = repository.mileageReadings
    val evenements: StateFlow<List<Evenement>> = repository.evenements
    val fuelReadings: StateFlow<List<FuelReading>> = repository.fuelReadings
    val garage: StateFlow<List<Vehicle>> = repository.garage
    val activeVehicleId: StateFlow<String> = repository.activeVehicleId

    // Consommation moyenne calculée (L/100km)
    val averageConsumption: StateFlow<Double> = fuelReadings
        .map { readings ->
            if (readings.size < 2) return@map 0.0
            
            val sorted = readings.filter { it.odometer != null }.sortedBy { it.date }
            if (sorted.size < 2) return@map 0.0
            
            val firstKm = sorted.first().odometer!!
            val lastKm = sorted.last().odometer!!
            val distance = lastKm - firstKm
            
            if (distance <= 0) return@map 0.0
            
            // On somme tous les litres SAUF le dernier (ou le premier selon la logique de plein complet)
            // Logique standard : Somme des litres des pleins qui ont permis de parcourir la distance.
            // Ici on prend la somme de tous les pleins sauf le tout premier du segment.
            val totalLiters = sorted.drop(1).sumOf { it.liters }
            
            (totalLiters / distance) * 100.0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Exposer le dernier relevé kilométrique (le premier de la liste triée DESC)
    val latestMileage: StateFlow<MileageReading?> = mileageReadings
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mechanicInfo: StateFlow<MechanicInfo?> = vehicle
        .map { it?.mechanic }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val insuranceInfo: StateFlow<InsuranceInfo?> = vehicle
        .map { it?.insurance }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _defaultYearsFilter = MutableStateFlow(repository.getDefaultYearsFilter())
    val defaultYearsFilter: StateFlow<Int> = _defaultYearsFilter.asStateFlow()

    private val _showMileageZone = MutableStateFlow(repository.getShowMileageZone())
    val showMileageZone: StateFlow<Boolean> = _showMileageZone.asStateFlow()

    private val _showFuelZone = MutableStateFlow(repository.getShowFuelZone())
    val showFuelZone: StateFlow<Boolean> = _showFuelZone.asStateFlow()

    val currentTheme: StateFlow<String> = repository.currentTheme

    private val _currentLanguage = MutableStateFlow(repository.getLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _isSetupComplete = MutableStateFlow(repository.isSetupComplete())
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    val isPro: StateFlow<Boolean> = repository.isPro
    val enableMultiVehicle: StateFlow<Boolean> = repository.enableMultiVehicle

    init {
        // Appliquer la langue sauvegardée au démarrage
        applyLanguage(_currentLanguage.value)
    }

    fun updateDefaultYearsFilter(value: Int) {
        repository.saveDefaultYearsFilter(value)
        _defaultYearsFilter.value = value
    }

    fun updateLanguage(languageCode: String) {
        repository.saveLanguage(languageCode)
        applyLanguage(languageCode)
        _currentLanguage.value = languageCode
    }

    fun completeSetup() {
        repository.setSetupComplete(true)
        _isSetupComplete.value = true
    }

    private fun applyLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun updateShowMileageZone(value: Boolean) {
        repository.saveShowMileageZone(value)
        _showMileageZone.value = value
    }

    fun updateShowFuelZone(value: Boolean) {
        repository.saveShowFuelZone(value)
        _showFuelZone.value = value
    }

    fun updateEnableMultiVehicle(value: Boolean) {
        repository.setEnableMultiVehicle(value)
    }

    fun switchVehicle(vehicleId: String) {
        repository.switchVehicle(vehicleId)
    }

    /**
     * Crée réellement un nouveau véhicule avec les valeurs fournies
     * et le sélectionne comme véhicule actif.
     */
    fun createNewVehicle(brand: String, model: String, countryCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val newId = UUID.randomUUID().toString()
                
                val newVeh = Vehicle(
                    id = newId,
                    model = model,
                    brand = brand,
                    countryCode = countryCode.uppercase()
                )
                
                // On enregistre le véhicule avec un historique vide
                repository.addVehicleInitial(newVeh)
                
                // On bascule dessus
                repository.switchVehicle(newId)
                
                onSuccess()
            } catch (e: Exception) {
                // Log ou gestion erreur
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        repository.deleteVehicle(vehicleId)
    }

    fun resetAllData() {
        repository.resetAllData()
        // Rafraîchir les flags après reset des prefs
        _currentLanguage.value = repository.getLanguage()
        _isSetupComplete.value = repository.isSetupComplete()
        _showMileageZone.value = repository.getShowMileageZone()
        _showFuelZone.value = repository.getShowFuelZone()
        _defaultYearsFilter.value = repository.getDefaultYearsFilter()
    }

    fun unlockPro() {
        repository.setPro(true)
    }

    // ==========================================
    // ACTIONS VÉHICULE & RELEVÉS
    // ==========================================

    fun saveVehicle(
        model: String,
        brand: String,
        plateNumber: String,
        firstRegistrationDate: String,
        vin: String,
        type: String,
        fiscalPower: Int?,
        fuelType: String,
        themeKey: String,
        photoPath: String,
        acquisitionDate: String,
        acquisitionMileage: Int,
        countryCode: String,
        photos: List<Attachment>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Valeurs par défaut si vides
                val finalModel = model.ifBlank { repository.getString(R.string.veh_model) }
                val finalBrand = brand.ifBlank { repository.getString(R.string.veh_brand) }

                val current = repository.vehicle.value
                val newVehicle = Vehicle(
                    id = activeVehicleId.value,
                    model = finalModel,
                    brand = finalBrand,
                    plateNumber = plateNumber,
                    firstRegistrationDate = firstRegistrationDate,
                    vin = vin,
                    type = type,
                    fiscalPower = fiscalPower,
                    fuelType = fuelType,
                    themeKey = themeKey,
                    photoPath = photoPath,
                    acquisitionDate = acquisitionDate,
                    acquisitionMileage = acquisitionMileage,
                    countryCode = countryCode.uppercase(),
                    photos = photos,
                    mechanic = current?.mechanic ?: MechanicInfo(),
                    insurance = current?.insurance ?: InsuranceInfo()
                )
                repository.saveVehicle(newVehicle)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue lors de la sauvegarde du véhicule.")
            }
        }
    }

    fun saveMechanic(
        logoPath: String,
        name: String,
        address: String,
        phone: String,
        email: String,
        website: String,
        customerNumber: String,
        contractNumber: String,
        photos: List<Attachment>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.saveMechanic(MechanicInfo(logoPath, name, address, phone, email, website, customerNumber, contractNumber, photos))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de la sauvegarde du garagiste.")
            }
        }
    }

    fun saveInsurance(
        logoPath: String,
        name: String,
        address: String,
        phone: String,
        email: String,
        website: String,
        contractNumber: String,
        customerNumber: String,
        photos: List<Attachment>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.saveInsurance(InsuranceInfo(logoPath, name, address, phone, email, website, contractNumber, customerNumber, photos))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de la sauvegarde de l'assurance.")
            }
        }
    }

    fun addMileageReading(value: Int, date: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (value < 0) {
                    onError("Le kilométrage doit être positif.")
                    return@launch
                }
                repository.addMileageReading(MileageReading(date, value))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue lors de la saisie kilométrique.")
            }
        }
    }

    fun deleteMileageReading(date: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteMileageReading(date)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de la suppression du relevé.")
            }
        }
    }

    fun updateMileageReading(oldDate: String, value: Int, newDate: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (value < 0) {
                    onError("Le kilométrage doit être positif.")
                    return@launch
                }
                repository.updateMileageReading(oldDate, MileageReading(newDate, value))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de la mise à jour du relevé.")
            }
        }
    }

    // ==========================================
    // CRUD ÉVÉNEMENTS D'ENTRETIEN
    // ==========================================

    fun addEvenement(
        name: String,
        date: String,
        categoryKey: String,
        subCategoryKey: String,
        cost: Double,
        description: String,
        isMultiple: Boolean,
        actions: List<EvenementAction>,
        photos: List<Attachment>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (!isPro.value && evenements.value.size >= 10) {
                    onError("LIMIT_REACHED")
                    return@launch
                }

                if (name.isBlank() || date.isBlank()) {
                    onError("Le nom et la date de l'événement sont obligatoires.")
                    return@launch
                }
                val currentCountry = repository.vehicle.value?.countryCode ?: ""
                val id = UUID.randomUUID().toString()
                
                // On injecte le code pays actuel dans l'événement et ses sous-actions
                val countryActions = actions.map { it.copy(countryCode = currentCountry) }
                val newEvt = Evenement(id, name, date, categoryKey, subCategoryKey, cost, description, isMultiple, countryActions, photos, currentCountry)
                
                repository.addEvenement(newEvt)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue lors de l'ajout de l'événement.")
            }
        }
    }

    fun updateEvenement(
        id: String,
        name: String,
        date: String,
        categoryKey: String,
        subCategoryKey: String,
        cost: Double,
        description: String,
        isMultiple: Boolean,
        actions: List<EvenementAction>,
        photos: List<Attachment>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (!isPro.value && evenements.value.size >= 10) {
                    onError("LIMIT_REACHED")
                    return@launch
                }

                if (name.isBlank() || date.isBlank()) {
                    onError("Le nom et la date sont obligatoires.")
                    return@launch
                }
                
                // Lors d'une mise à jour, on préserve ou on met à jour le pays
                val currentCountry = repository.vehicle.value?.countryCode ?: ""
                val countryActions = actions.map { 
                    if (it.countryCode.isNullOrBlank()) it.copy(countryCode = currentCountry) else it 
                }
                
                val updatedEvt = Evenement(id, name, date, categoryKey, subCategoryKey, cost, description, isMultiple, countryActions, photos, currentCountry)
                repository.updateEvenement(updatedEvt)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue lors de la modification.")
            }
        }
    }

    fun deleteEvenement(evenementId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteEvenement(evenementId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue lors de la suppression.")
            }
        }
    }

    // ==========================================
    // CRUD CARBURANT
    // ==========================================

    fun addFuelReading(date: String, liters: Double, cost: Double, odometer: Int?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentCountry = repository.vehicle.value?.countryCode
                repository.addFuelReading(FuelReading(date, liters, cost, odometer, currentCountry))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'ajout du plein.")
            }
        }
    }

    fun deleteFuelReading(date: String, liters: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteFuelReading(date, liters)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de la suppression du plein.")
            }
        }
    }

    fun updateFuelReading(oldReading: FuelReading, date: String, liters: Double, cost: Double, odometer: Int?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateFuelReading(oldReading, FuelReading(date, liters, cost, odometer, oldReading.countryCode))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de la modification du plein.")
            }
        }
    }

    // ==========================================
    // GÉNÉRATION DE CLÉS UNIQUES
    // ==========================================

    private fun cleanNameForKey(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
    }

    private fun generateUniqueCategoryKey(name: String): String {
        val baseKey = cleanNameForKey(name).ifBlank { "cat" }
        var key = baseKey
        var counter = 1
        val existingKeys = categories.value.map { it.key }
        
        while (existingKeys.contains(key)) {
            key = "${baseKey}_$counter"
            counter++
        }
        return key
    }

    private fun generateUniqueSubCategoryKey(parentKey: String, name: String): String {
        val prefix = if (parentKey.length >= 3) parentKey.take(3).lowercase() + "_" else parentKey.lowercase() + "_"
        val baseName = cleanNameForKey(name).ifBlank { "sub" }
        val baseKey = prefix + baseName
        
        var key = baseKey
        var counter = 1
        val existingKeys = subCategories.value.map { it.key }
        
        while (existingKeys.contains(key)) {
            key = "${baseKey}_$counter"
            counter++
        }
        return key
    }


    // ==========================================
    // CRUD CATÉGORIES & SOUS-CATÉGORIES
    // ==========================================

    fun addSharedSubCategory(name: String, description: String, icon: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Créer la sous-catégorie pour Entretien et Dépannage
                val entKey = generateUniqueSubCategoryKey("entretien", name)
                val depKey = generateUniqueSubCategoryKey("depannage", name)
                
                repository.addSubCategory(SubCategory(entKey, "entretien", name, description, icon))
                repository.addSubCategory(SubCategory(depKey, "depannage", name, description, icon))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'ajout.")
            }
        }
    }

    fun deleteSharedSubCategory(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val subs = subCategories.value.filter { it.name == name && (it.categoryKey == "entretien" || it.categoryKey == "depannage") }
                subs.forEach { sub ->
                    repository.deleteSubCategory(sub.key)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Suppression impossible (elle est peut-être utilisée).")
            }
        }
    }

    fun addCategory(name: String, description: String, icon: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val key = generateUniqueCategoryKey(name)
                val cat = Category(key, name, description, icon)
                repository.addCategory(cat)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'ajout.")
            }
        }
    }

    fun updateCategory(key: String, name: String, description: String, icon: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cat = Category(key, name, description, icon)
                repository.updateCategory(cat)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Modification impossible.")
            }
        }
    }

    fun deleteCategory(key: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(key)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Suppression impossible.")
            }
        }
    }

    fun addSubCategory(categoryKey: String, name: String, description: String, icon: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val key = generateUniqueSubCategoryKey(categoryKey, name)
                val sub = SubCategory(key, categoryKey, name, description, icon)
                repository.addSubCategory(sub)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'ajout.")
            }
        }
    }

    fun updateSubCategory(key: String, categoryKey: String, name: String, description: String, icon: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val sub = SubCategory(key, categoryKey, name, description, icon)
                repository.updateSubCategory(sub)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Modification impossible.")
            }
        }
    }

    fun deleteSubCategory(key: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteSubCategory(key)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Suppression impossible.")
            }
        }
    }

    // ==========================================
    // IMPORT & EXPORT DE FICHIERS ZIP
    // ==========================================

    fun importCategories(inputStream: InputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.importCategories(inputStream)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'importation.")
            }
        }
    }

    fun exportCategories(outputStream: OutputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.exportCategories(outputStream)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'exportation.")
            }
        }
    }

    fun importSuivi(inputStream: InputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.importSuivi(inputStream)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'importation.")
            }
        }
    }

    fun exportSuivi(outputStream: OutputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.exportSuivi(outputStream)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'exportation.")
            }
        }
    }

    fun exportGlobalBackup(outputStream: OutputStream, selectedIds: List<String>? = null, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.exportGlobalBackup(outputStream, selectedIds)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'exportation globale.")
            }
        }
    }

    fun peekBackup(inputStream: InputStream, onSuccess: (GlobalBackup) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backup = repository.peekBackup(inputStream)
                onSuccess(backup)
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'analyse de la sauvegarde.")
            }
        }
    }

    fun importSelectedVehicles(
        inputStream: InputStream, 
        selectedIds: List<String>, 
        strategies: Map<String, String>, 
        onSuccess: (Boolean) -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val truncated = repository.importSelectedVehicles(inputStream, selectedIds, strategies)
                onSuccess(truncated)
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'importation sélective.")
            }
        }
    }

    fun importGlobalBackup(inputStream: InputStream, onSuccess: (Boolean) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val truncated = repository.importGlobalBackup(inputStream)
                onSuccess(truncated)
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'importation globale.")
            }
        }
    }
}

class VehicleViewModelFactory(private val repository: VehicleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehicleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VehicleViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel inconnue")
    }
}
