package com.roadscript.oss.data.repository

import android.content.Context
import android.content.res.XmlResourceParser
import com.roadscript.oss.R
import com.roadscript.oss.data.models.*
import com.roadscript.oss.data.xml.GlobalBackup
import com.roadscript.oss.data.xml.XmlDataParser
import com.roadscript.oss.ui.utils.FileHelper
import com.roadscript.oss.ui.utils.ZipHelper
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class VehicleRepository(private val context: Context) {

    private val categoriesFile = File(context.filesDir, "categories.xml")
    private val prefs = context.getSharedPreferences("suivi_vehicule_prefs", Context.MODE_PRIVATE)

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _subCategories = MutableStateFlow<List<SubCategory>>(emptyList())
    val subCategories = _subCategories.asStateFlow()

    private val _garage = MutableStateFlow<List<Vehicle>>(emptyList())
    val garage = _garage.asStateFlow()

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle = _vehicle.asStateFlow()

    private val _mileageReadings = MutableStateFlow<List<MileageReading>>(emptyList())
    val mileageReadings = _mileageReadings.asStateFlow()

    private val _evenements = MutableStateFlow<List<Evenement>>(emptyList())
    val evenements = _evenements.asStateFlow()

    private val _fuelReadings = MutableStateFlow<List<FuelReading>>(emptyList())
    val fuelReadings = _fuelReadings.asStateFlow()

    private val _isPro = MutableStateFlow(false)
    val isPro = _isPro.asStateFlow()

    private val _enableMultiVehicle = MutableStateFlow(false)
    val enableMultiVehicle = _enableMultiVehicle.asStateFlow()

    private val _currentTheme = MutableStateFlow("INDIGO")
    val currentTheme = _currentTheme.asStateFlow()

    private val _activeVehicleId = MutableStateFlow("")
    val activeVehicleId = _activeVehicleId.asStateFlow()

    init {

        // Charger le statut Pro et Multi-véhicule
        _isPro.value = prefs.getBoolean("is_pro_version", true)
        _enableMultiVehicle.value = prefs.getBoolean("enable_multi_vehicle", true)
        
        val savedId = prefs.getString("active_vehicle_id", null)
        if (savedId == null) {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("active_vehicle_id", newId).apply()
            _activeVehicleId.value = newId
        } else {
            _activeVehicleId.value = savedId
        }

        // 1. Scanner le garage
        refreshGarage()

        // 3. Charger les données
        loadCategories()
        loadActiveVehicle()

    }

    private fun getVehicleFile(vehicleId: String): File {
        return File(context.filesDir, "vehicle_$vehicleId.xml")
    }

    fun refreshGarage() {
        val files = context.filesDir.listFiles { _, name -> name.startsWith("vehicle_") && name.endsWith(".xml") }
        val vehicleList = mutableListOf<Vehicle>()
        files?.forEach { file ->
            try {
                FileInputStream(file).use { fis ->
                    val (_, veh, _, _, _) = XmlDataParser.parseSuivi(fis)
                    if (veh != null) vehicleList.add(veh)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        _garage.value = vehicleList.sortedBy { it.model }
        
        // Si le véhicule actif n'existe plus ou si le garage est vide mais qu'on a un fichier par défaut
        if (_garage.value.isNotEmpty() && _garage.value.none { it.id == _activeVehicleId.value }) {
            val newActiveId = _garage.value.first().id
            _activeVehicleId.value = newActiveId
            prefs.edit().putString("active_vehicle_id", newActiveId).apply()
            loadActiveVehicle()
        }
    }

    fun switchVehicle(vehicleId: String) {
        if (_activeVehicleId.value == vehicleId) return
        _activeVehicleId.value = vehicleId
        prefs.edit().putString("active_vehicle_id", vehicleId).apply()
        loadActiveVehicle()
    }

    private fun loadActiveVehicle() {
        val bundle = getVehicleBundle(_activeVehicleId.value)
        _vehicle.value = bundle.first
        _currentTheme.value = bundle.first?.themeKey ?: "INDIGO"
        _mileageReadings.value = bundle.second.sortedByDescending { it.date }
        _evenements.value = bundle.third.sortedByDescending { it.date }
        _fuelReadings.value = bundle.fourth.sortedByDescending { it.date }
        
        // Synchroniser les obligations basées sur le pays du véhicule
        bundle.first?.countryCode?.let { syncJurisdictionObligations(it) }
    }

    private fun getVehicleBundle(vehicleId: String): com.roadscript.oss.data.xml.Tuple4<Vehicle?, List<MileageReading>, List<Evenement>, List<FuelReading>> {
        val file = getVehicleFile(vehicleId)
        if (!file.exists()) {
            return com.roadscript.oss.data.xml.Tuple4(null, emptyList(), emptyList(), emptyList())
        }
        return try {
            FileInputStream(file).use { fis ->
                val (_, veh, km, evts, fuel) = XmlDataParser.parseSuivi(fis)
                com.roadscript.oss.data.xml.Tuple4(veh, km, evts, fuel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            com.roadscript.oss.data.xml.Tuple4(null, emptyList(), emptyList(), emptyList())
        }
    }

    // ==========================================
    // APP SETTINGS
    // ==========================================

    fun getDefaultYearsFilter(): Int {
        return prefs.getInt("default_years_filter", 0) // 0 = Toutes par défaut
    }

    fun saveDefaultYearsFilter(value: Int) {
        prefs.edit { putInt("default_years_filter", value) }
    }

    fun getShowMileageZone(): Boolean {
        return prefs.getBoolean("show_mileage_zone", true)
    }

    fun saveShowMileageZone(value: Boolean) {
        prefs.edit { putBoolean("show_mileage_zone", value) }
    }

    fun getShowFuelZone(): Boolean {
        return prefs.getBoolean("show_fuel_zone", true)
    }

    fun saveShowFuelZone(value: Boolean) {
        prefs.edit { putBoolean("show_fuel_zone", value) }
    }

    fun getLanguage(): String {
        val saved = prefs.getString("app_language", "") ?: ""
        if (saved.isNotBlank()) return saved

        // Logique d'initialisation au premier lancement
        val systemLang = Locale.getDefault().language
        val supportedLangs = listOf("fr", "en", "es", "de", "it", "pt")
        val initialLang = if (systemLang in supportedLangs) systemLang else "en"
        
        saveLanguage(initialLang)
        return initialLang
    }

    fun saveLanguage(value: String) {
        prefs.edit { putString("app_language", value) }
    }


    fun isSetupComplete(): Boolean {
        return prefs.getBoolean("is_setup_complete", false)
    }

    fun setSetupComplete(value: Boolean) {
        prefs.edit { putBoolean("is_setup_complete", value) }
    }

    fun getString(resId: Int): String {
        return context.getString(resId)
    }

    // ==========================================
    // PRO VERSION MANAGEMENT
    // ==========================================

    fun isPro(): Boolean {
        return _isPro.value
    }

    fun setPro(value: Boolean) {
        prefs.edit { putBoolean("is_pro_version", value) }
        _isPro.value = value
    }

    fun getEnableMultiVehicle(): Boolean {
        return _enableMultiVehicle.value
    }

    fun setEnableMultiVehicle(value: Boolean) {
        prefs.edit { putBoolean("enable_multi_vehicle", value) }
        _enableMultiVehicle.value = value
    }

    /**
     * Crée un nouveau fichier véhicule avec un historique vide.
     */
    fun addVehicleInitial(vehicle: Vehicle) {
        saveSuiviAtomically(vehicle, emptyList(), emptyList(), emptyList())
    }

    /**
     * Parcourt jurisdictions.xml et remplace la liste des obligations par celles du pays sélectionné.
     */
    fun syncJurisdictionObligations(countryCode: String) {
        if (countryCode.isBlank()) return

        try {
            val parser = context.resources.getXml(R.xml.jurisdictions)
            var eventType = parser.eventType
            var currentCountryCode: String? = null
            val newObligations = mutableListOf<SubCategory>()
            
            val currentLang = getLanguage().ifEmpty { Locale.getDefault().language }

            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {
                    when (parser.name) {
                        "country" -> {
                            currentCountryCode = parser.getAttributeValue(null, "code")
                        }
                        "obligation" -> {
                            if (currentCountryCode == countryCode) {
                                val id = parser.getAttributeValue(null, "id")
                                val name = when (currentLang) {
                                    "fr" -> parser.getAttributeValue(null, "fr")
                                    "en" -> parser.getAttributeValue(null, "en")
                                    "es" -> parser.getAttributeValue(null, "es")
                                    "de" -> parser.getAttributeValue(null, "de")
                                    "it" -> parser.getAttributeValue(null, "it")
                                    "pt" -> parser.getAttributeValue(null, "pt")
                                    else -> parser.getAttributeValue(null, "en")
                                } ?: parser.getAttributeValue(null, "en") ?: parser.getAttributeValue(null, "fr") ?: ""
                                
                                newObligations.add(SubCategory(
                                    key = id,
                                    categoryKey = "obligation",
                                    name = name,
                                    description = "Obligation légale ($countryCode)",
                                    icon = "ic_inspection",
                                    isDefault = false,
                                    countryCode = countryCode
                                ))
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            // On remplace totalement la liste des obligations
            val currentSubs = _subCategories.value.toMutableList()
            currentSubs.removeAll { it.categoryKey == "obligation" }
            currentSubs.addAll(0, newObligations)
            saveCategoriesAtomically(_categories.value, currentSubs)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // CHARGEMENT & SAUVEGARDE ATOMIQUE
    // ==========================================

    private fun loadCategories() {
        if (!categoriesFile.exists()) {
            setupDefaultCategories()
            return
        }
        try {
            FileInputStream(categoriesFile).use { fis ->
                val (cats, subs) = XmlDataParser.parseCategories(fis)
                updateCategoriesInState(cats, subs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setupDefaultCategories()
        }
    }

    private fun saveCategoriesAtomically(cats: List<Category>, subs: List<SubCategory>) {
        val tempFile = File(context.filesDir, "categories.xml.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                XmlDataParser.writeCategories(fos, cats, subs)
            }
            if (tempFile.renameTo(categoriesFile)) {
                updateCategoriesInState(cats, subs)
            } else {
                throw Exception("Erreur lors du renommage du fichier temporaire des catégories.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    private fun saveSuiviAtomically(veh: Vehicle?, km: List<MileageReading>, evts: List<Evenement>, fuel: List<FuelReading>) {
        // Si le véhicule est nul, on utilise un objet par défaut avec l'ID actif
        val finalVeh = veh ?: Vehicle(id = _activeVehicleId.value)
        val vehicleId = finalVeh.id
        val targetFile = getVehicleFile(vehicleId)
        val tempFile = File(context.filesDir, "vehicle_$vehicleId.xml.tmp")
        
        try {
            FileOutputStream(tempFile).use { fos ->
                XmlDataParser.writeSuivi(fos, finalVeh, km, evts, fuel)
            }
            if (tempFile.renameTo(targetFile)) {
                // On ne met à jour l'état que si c'est le véhicule actif
                if (vehicleId == _activeVehicleId.value) {
                    _vehicle.value = finalVeh
                    _currentTheme.value = finalVeh.themeKey
                    _mileageReadings.value = km.sortedByDescending { it.date }
                    _evenements.value = evts.sortedByDescending { it.date }
                    _fuelReadings.value = fuel.sortedByDescending { it.date }
                    
                    // Synchroniser les obligations si le pays a changé
                    finalVeh.countryCode.let { syncJurisdictionObligations(it) }
                }
                // Dans tous les cas, on rafraîchit le garage pour les miniatures
                refreshGarage()
            } else {
                throw Exception("Erreur lors du renommage du fichier temporaire de suivi.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    private fun updateCategoriesInState(cats: List<Category>, subs: List<SubCategory>) {
        _categories.value = cats.sortedBy { it.name.lowercase() }
        _subCategories.value = subs.sortedBy { it.name.lowercase() }
    }

    private fun setupDefaultCategories() {
        val defaultCats = listOf(
            Category("entretien", context.getString(R.string.cat_service), context.getString(R.string.cat_service_desc), "ic_maintenance", true),
            Category("depannage", context.getString(R.string.cat_repair), context.getString(R.string.cat_repair_desc), "ic_repair", true),
            Category("obligation", context.getString(R.string.cat_legal), context.getString(R.string.cat_legal_desc), "ic_inspection", true),
            Category("accessoire", context.getString(R.string.cat_accessory), context.getString(R.string.cat_accessory_desc), "ic_accessories", true)
        )

        val sharedSubNames = listOf(
            "Carrosserie" to context.getString(R.string.sub_body),
            "Climatisation, chauffage et refroidissement" to context.getString(R.string.sub_air_con),
            "Électricité et batterie" to context.getString(R.string.sub_electric),
            "Freinage" to context.getString(R.string.sub_brakes),
            "Moteur" to context.getString(R.string.sub_engine),
            "Pneumatiques" to context.getString(R.string.sub_tires),
            "Signalisation et visibilité" to context.getString(R.string.sub_lights),
            "Suspension et amortissement" to context.getString(R.string.sub_suspension),
            "Transmission et direction" to context.getString(R.string.sub_transmission),
            "Main d'œuvre" to context.getString(R.string.sub_labor),
            "Consommables" to context.getString(R.string.sub_consumables),
            "Autre" to context.getString(R.string.sub_other)
        )

        val defaultSubs = mutableListOf<SubCategory>()

        for ((name, translatedName) in sharedSubNames) {
            val baseKey = name.lowercase().replace(Regex("[^a-z0-9]"), "_").replace(Regex("_+"), "_").trim('_')
            defaultSubs.add(SubCategory("ent_$baseKey", "entretien", translatedName, "", getIconForDefaultSub(baseKey), true))
            defaultSubs.add(SubCategory("dep_$baseKey", "depannage", translatedName, "", getIconForDefaultSub(baseKey), true))
        }
        
        val obligationSubs = listOf(
            context.getString(R.string.sub_inspection) to "obl_contr_le_technique",
            context.getString(R.string.sub_reinspection) to "obl_contre_visite_ct",
            context.getString(R.string.sub_insurance) to "obl_assurance",
            context.getString(R.string.sub_other) to "obl_autre"
        )
        for ((name, baseKey) in obligationSubs) {
            defaultSubs.add(SubCategory(baseKey, "obligation", name, "obligation légale", "ic_inspection", true))
        }

        val accSubs = listOf(
            context.getString(R.string.sub_audio) to "acc_audio",
            context.getString(R.string.sub_comfort) to "acc_confort",
            context.getString(R.string.sub_aesthetics) to "acc_esth_tique_nettoyage",
            context.getString(R.string.sub_equipment) to "acc_quipement",
            context.getString(R.string.sub_other) to "acc_autre"
        )
        for ((name, baseKey) in accSubs) {
            defaultSubs.add(SubCategory(baseKey, "accessoire", name, "Accessoire et agrément", "ic_accessories", true))
        }

        try {
            saveCategoriesAtomically(defaultCats, defaultSubs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getIconForDefaultSub(key: String): String {
        return when {
            key.contains("carburant") -> "ic_oil"
            key.contains("pneu") -> "ic_tire"
            key.contains("frein") -> "ic_brake"
            key.contains("moteur") -> "ic_engine"
            key.contains("elec") || key.contains("batte") -> "ic_battery"
            key.contains("clim") || key.contains("chau") -> "ic_filter"
            key.contains("signa") || key.contains("visi") -> "ic_calendar"
            else -> "ic_maintenance"
        }
    }

    // ==========================================
    // API D'INTÉGRITÉ & VÉRIFICATION
    // ==========================================

    fun isCategoryUsed(categoryKey: String): Boolean {
        return _evenements.value.any { evt -> evt.actions.any { it.categoryKey == categoryKey } }
    }

    fun isSubCategoryUsed(subCategoryKey: String): Boolean {
        return _evenements.value.any { evt -> 
            evt.subCategoryKey == subCategoryKey || evt.actions.any { it.subCategoryKey == subCategoryKey } 
        }
    }

    // ==========================================
    // CRUD CATÉGORIES & SOUS-CATÉGORIES
    // ==========================================

    fun addCategory(category: Category) {
        if (_categories.value.any { it.key == category.key }) {
            throw IllegalArgumentException("Une catégorie avec cette clé existe déjà.")
        }
        val newList = _categories.value + category
        saveCategoriesAtomically(newList, _subCategories.value)
    }

    fun updateCategory(category: Category) {
        val existing = _categories.value.find { it.key == category.key }
            ?: throw IllegalArgumentException("Catégorie introuvable.")
        
        if (existing.isDefault) {
            throw IllegalStateException("Les catégories par défaut ne peuvent pas être modifiées.")
        }

        if (isCategoryUsed(category.key)) {
            throw IllegalStateException("Cette catégorie est utilisée et ne peut être modifiée.")
        }

        val newList = _categories.value.map { if (it.key == category.key) category else it }
        saveCategoriesAtomically(newList, _subCategories.value)
    }

    fun deleteCategory(categoryKey: String) {
        val existing = _categories.value.find { it.key == categoryKey }
            ?: throw IllegalArgumentException("Catégorie introuvable.")

        if (existing.isDefault) {
            throw IllegalStateException("Les catégories par défaut ne peuvent pas être supprimées.")
        }

        if (isCategoryUsed(categoryKey)) {
            throw IllegalStateException("Cette catégorie est utilisée. Suppression impossible.")
        }
        
        val affectedSubs = _subCategories.value.filter { it.categoryKey == categoryKey }
        for (sub in affectedSubs) {
            if (isSubCategoryUsed(sub.key)) {
                throw IllegalStateException("Impossible de supprimer la catégorie car sa sous-catégorie '${sub.name}' est utilisée.")
            }
        }

        val newCats = _categories.value.filter { it.key != categoryKey }
        val newSubs = _subCategories.value.filter { it.categoryKey != categoryKey }
        saveCategoriesAtomically(newCats, newSubs)
    }

    fun addSubCategory(subCategory: SubCategory) {
        if (_subCategories.value.any { it.key == subCategory.key }) {
            throw IllegalArgumentException("Une sous-catégorie avec cette clé existe déjà.")
        }
        if (!_categories.value.any { it.key == subCategory.categoryKey }) {
            throw IllegalArgumentException("Catégorie parente introuvable.")
        }
        val newList = _subCategories.value + subCategory
        saveCategoriesAtomically(_categories.value, newList)
    }

    fun updateSubCategory(subCategory: SubCategory) {
        val existing = _subCategories.value.find { it.key == subCategory.key }
            ?: throw IllegalArgumentException("Sous-catégorie introuvable.")

        if (existing.isDefault) {
            throw IllegalStateException("Les sous-catégories par défaut ne peuvent pas être modifiées.")
        }

        if (isSubCategoryUsed(subCategory.key)) {
            throw IllegalStateException("Cette sous-catégorie est utilisée et ne peut être modifiée.")
        }
        val newList = _subCategories.value.map { if (it.key == subCategory.key) subCategory else it }
        saveCategoriesAtomically(_categories.value, newList)
    }

    fun deleteSubCategory(subCategoryKey: String) {
        val existing = _subCategories.value.find { it.key == subCategoryKey }
            ?: throw IllegalArgumentException("Sous-catégorie introuvable.")

        if (existing.isDefault) {
            throw IllegalStateException("Les sous-catégories par défaut ne peuvent pas être supprimées.")
        }

        if (isSubCategoryUsed(subCategoryKey)) {
            throw IllegalStateException("Cette sous-catégorie est utilisée. Suppression impossible.")
        }
        val newList = _subCategories.value.filter { it.key != subCategoryKey }
        saveCategoriesAtomically(_categories.value, newList)
    }

    // ==========================================
    // CRUD VEHICULE, KILOMETRES & ÉVÉNEMENTS
    // ==========================================

    fun saveVehicle(vehicle: Vehicle) {
        val oldPath = _vehicle.value?.photoPath ?: ""
        if (oldPath.isNotBlank() && oldPath != vehicle.photoPath) {
            FileHelper.safeDelete(context, oldPath)
        }
        saveSuiviAtomically(vehicle, _mileageReadings.value, _evenements.value, _fuelReadings.value)
    }

    fun saveMechanic(info: MechanicInfo) {
        val currentVehicle = _vehicle.value
        if (currentVehicle != null) {
            val oldPath = currentVehicle.mechanic.logoPath
            if (oldPath.isNotBlank() && oldPath != info.logoPath) {
                FileHelper.safeDelete(context, oldPath)
            }
            val updatedVehicle = currentVehicle.copy(mechanic = info)
            saveSuiviAtomically(updatedVehicle, _mileageReadings.value, _evenements.value, _fuelReadings.value)
        }
    }

    fun saveInsurance(info: InsuranceInfo) {
        val currentVehicle = _vehicle.value
        if (currentVehicle != null) {
            val oldPath = currentVehicle.insurance.logoPath
            if (oldPath.isNotBlank() && oldPath != info.logoPath) {
                FileHelper.safeDelete(context, oldPath)
            }
            val updatedVehicle = currentVehicle.copy(insurance = info)
            saveSuiviAtomically(updatedVehicle, _mileageReadings.value, _evenements.value, _fuelReadings.value)
        }
    }

    fun addMileageReading(reading: MileageReading) {
        val existingIndex = _mileageReadings.value.indexOfFirst { it.date == reading.date }
        val newList = if (existingIndex != -1) {
            _mileageReadings.value.mapIndexed { idx, it -> if (idx == existingIndex) reading else it }
        } else {
            _mileageReadings.value + reading
        }
        saveSuiviAtomically(_vehicle.value, newList, _evenements.value, _fuelReadings.value)
    }

    fun deleteMileageReading(date: String) {
        val newList = _mileageReadings.value.filter { it.date != date }
        saveSuiviAtomically(_vehicle.value, newList, _evenements.value, _fuelReadings.value)
    }

    fun updateMileageReading(oldDate: String, newReading: MileageReading) {
        val newList = _mileageReadings.value.filter { it.date != oldDate }.toMutableList()
        val existingIndex = newList.indexOfFirst { it.date == newReading.date }
        if (existingIndex != -1) {
            newList[existingIndex] = newReading
        } else {
            newList.add(newReading)
        }
        saveSuiviAtomically(_vehicle.value, newList, _evenements.value, _fuelReadings.value)
    }

    fun addEvenement(evenement: Evenement) {
        val newList = _evenements.value + evenement
        saveSuiviAtomically(_vehicle.value, _mileageReadings.value, newList, _fuelReadings.value)
    }

    fun updateEvenement(evenement: Evenement) {
        val existing = _evenements.value.find { it.id == evenement.id }
        if (existing == null) {
            throw IllegalArgumentException("Événement introuvable.")
        }
        val removedFiles = existing.photos.filter { it !in evenement.photos }
        removedFiles.forEach { FileHelper.safeDelete(context, it.path) }
        val newList = _evenements.value.map { if (it.id == evenement.id) evenement else it }
        saveSuiviAtomically(_vehicle.value, _mileageReadings.value, newList, _fuelReadings.value)
    }

    fun deleteEvenement(evenementId: String) {
        val evtToDelete = _evenements.value.find { it.id == evenementId }
        evtToDelete?.photos?.forEach { FileHelper.safeDelete(context, it.path) }
        val newList = _evenements.value.filter { it.id != evenementId }
        saveSuiviAtomically(_vehicle.value, _mileageReadings.value, newList, _fuelReadings.value)
    }

    fun addFuelReading(reading: FuelReading) {
        val newFuelList = _fuelReadings.value + reading
        var newMileageList = _mileageReadings.value
        
        // Synchronisation : si un kilométrage est renseigné, on l'ajoute aussi à la liste
        reading.odometer?.let { kmValue ->
            val existingIndex = newMileageList.indexOfFirst { it.date == reading.date }
            val mileageReading = MileageReading(reading.date, kmValue)
            newMileageList = if (existingIndex != -1) {
                newMileageList.mapIndexed { idx, it -> if (idx == existingIndex) mileageReading else it }
            } else {
                newMileageList + mileageReading
            }
        }
        
        // On sauvegarde tout en une seule opération atomique
        saveSuiviAtomically(_vehicle.value, newMileageList, _evenements.value, newFuelList)
    }

    fun deleteFuelReading(date: String, liters: Double) {
        val newList = _fuelReadings.value.filterNot { it.date == date && it.liters == liters }
        saveSuiviAtomically(_vehicle.value, _mileageReadings.value, _evenements.value, newList)
    }

    fun updateFuelReading(oldReading: FuelReading, newReading: FuelReading) {
        val newFuelList = _fuelReadings.value.map { if (it == oldReading) newReading else it }
        var newMileageList = _mileageReadings.value

        // Synchronisation lors de la mise à jour
        newReading.odometer?.let { kmValue ->
            val existingIndex = newMileageList.indexOfFirst { it.date == newReading.date }
            val mileageReading = MileageReading(newReading.date, kmValue)
            newMileageList = if (existingIndex != -1) {
                newMileageList.mapIndexed { idx, it -> if (idx == existingIndex) mileageReading else it }
            } else {
                newMileageList + mileageReading
            }
        }
        
        saveSuiviAtomically(_vehicle.value, newMileageList, _evenements.value, newFuelList)
    }

    fun deleteVehicle(vehicleId: String) {
        if (_garage.value.size <= 1 || _activeVehicleId.value == vehicleId) {
            // Ne pas supprimer le seul véhicule restant ou le véhicule actif
            return
        }
        
        // 1. Supprimer le dossier de documents du véhicule
        val vehicleFolder = FileHelper.getVehicleFolder(context, vehicleId)
        if (vehicleFolder.exists()) {
            vehicleFolder.deleteRecursively()
        }

        // 2. Supprimer le fichier XML
        val file = getVehicleFile(vehicleId)
        if (file.exists()) {
            file.delete()
        }
        
        refreshGarage()
    }

    // ==========================================
    // IMPORT / EXPORT ZIP
    // ==========================================

    fun importCategories(inputStream: InputStream) {
        try {
            val (cats, subs) = XmlDataParser.parseCategories(inputStream)
            saveCategoriesAtomically(cats, subs)
        } catch (e: Exception) {
            throw IllegalArgumentException("Échec de l'import des catégories : ${e.message}", e)
        }
    }

    fun exportCategories(outputStream: OutputStream) {
        try {
            XmlDataParser.writeCategories(outputStream, _categories.value, _subCategories.value)
        } catch (e: Exception) {
            throw e
        }
    }

    fun importSuivi(inputStream: InputStream) {
        try {
            val (_, veh, km, evts, fuel) = XmlDataParser.parseSuivi(inputStream)
            saveSuiviAtomically(veh, km, evts, fuel)
        } catch (e: Exception) {
            throw IllegalArgumentException("Échec de l'import du suivi : ${e.message}", e)
        }
    }

    fun exportSuivi(outputStream: OutputStream) {
        try {
            XmlDataParser.writeSuivi(outputStream, _vehicle.value, _mileageReadings.value, _evenements.value, _fuelReadings.value)
        } catch (e: Exception) {
            throw e
        }
    }

    fun exportGlobalBackup(outputStream: OutputStream, selectedVehicleIds: List<String>? = null) {
        val tempXml = File(context.cacheDir, "backup.xml")
        try {
            val vehiclesToExport = if (selectedVehicleIds == null) {
                _garage.value
            } else {
                _garage.value.filter { it.id in selectedVehicleIds }
            }

            FileOutputStream(tempXml).use { fos ->
                val allBundles = vehiclesToExport.map { v ->
                    val b = getVehicleBundle(v.id)
                    com.roadscript.oss.data.xml.VehicleBundle(v, b.second, b.third, b.fourth)
                }

                XmlDataParser.writeGlobal(
                    fos,
                    _categories.value,
                    _subCategories.value,
                    allBundles,
                    _vehicle.value?.countryCode ?: "",
                    getEnableMultiVehicle()
                )
            }
            
            // Collecter les fichiers utilisés par les véhicules sélectionnés
            val filesToInclude = getFilesToIncludeForVehicles(vehiclesToExport)
            
            ZipHelper.createZip(outputStream, tempXml, filesToInclude)
            
        } finally {
            if (tempXml.exists()) tempXml.delete()
        }
    }

    private fun getFilesToIncludeForVehicles(vehicles: List<Vehicle>): List<Pair<File, String>> {
        val result = mutableListOf<Pair<File, String>>()
        vehicles.forEach { v ->
            val vehicleId = v.id
            val zipDir = "garage/$vehicleId"
            
            fun addFile(path: String?) {
                if (path.isNullOrBlank()) return
                val absolutePath = FileHelper.toAbsolutePath(context, path)
                val file = File(absolutePath)
                if (file.exists()) {
                    result.add(file to "$zipDir/${file.name}")
                }
            }

            addFile(v.photoPath)
            addFile(v.mechanic.logoPath)
            addFile(v.insurance.logoPath)
            v.photos.forEach { addFile(it.path) }
            v.mechanic.photos.forEach { addFile(it.path) }
            v.insurance.photos.forEach { addFile(it.path) }
            
            // Événements
            val b = getVehicleBundle(vehicleId)
            b.third.forEach { evt ->
                evt.photos.forEach { addFile(it.path) }
            }
        }
        return result
    }

    /**
     * Analyse un fichier ZIP de sauvegarde sans l'importer.
     * Retourne la liste des véhicules trouvés dans la sauvegarde.
     */
    fun peekBackup(inputStream: InputStream): GlobalBackup {
        val tempDir = File(context.cacheDir, "peek_temp")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        return try {
            val (extractedXml, _) = ZipHelper.extractZip(inputStream, tempDir)
            if (extractedXml == null || !extractedXml.exists()) {
                throw IllegalArgumentException("Fichier de données manquant dans le ZIP.")
            }

            FileInputStream(extractedXml).use { fis ->
                val parsed = XmlDataParser.parseGlobal(fis)
                fixBackupPaths(parsed)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Importe sélectivement des véhicules depuis une sauvegarde.
     * selectedVehicleIds: IDs des véhicules à importer du ZIP.
     * duplicateStrategy: Map d'ID ZIP vers "REPLACE" ou "IMPORT_NEW".
     */
    fun importSelectedVehicles(
        inputStream: InputStream,
        selectedVehicleIds: List<String>,
        duplicateStrategy: Map<String, String>
    ): Boolean {
        val tempDir = File(context.cacheDir, "import_temp")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            val (extractedXml, extractedGarage) = ZipHelper.extractZip(inputStream, tempDir)
            if (extractedXml == null || !extractedXml.exists()) {
                throw IllegalArgumentException("Fichier de données manquant dans le ZIP.")
            }

            val fullBackup = FileInputStream(extractedXml).use { fis ->
                val parsed = XmlDataParser.parseGlobal(fis)
                fixBackupPaths(parsed)
            }

            val vehiclesToImport = fullBackup.vehicles.filter { it.vehicle.id in selectedVehicleIds }
            
            var truncated = false
            var importList = vehiclesToImport

            // Gestion Freemium
            if (!isPro()) {
                if (importList.size > 1) {
                    importList = importList.take(1)
                    truncated = true
                }
                val first = importList.firstOrNull()
                if (first != null && first.evenements.size > 10) {
                    importList = listOf(first.copy(evenements = first.evenements.take(10)))
                    truncated = true
                }
            }

            // Appliquer les catégories de la sauvegarde (on fusionne avec les existantes)
            // Pour simplifier, on garde le comportement actuel : on remplace les catégories 
            // mais on pourrait fusionner. Ici on remplace si le ZIP en contient.
            if (fullBackup.categories.isNotEmpty()) {
                saveCategoriesAtomically(fullBackup.categories, fullBackup.subCategories)
            }

            val garageDir = File(context.filesDir, "garage")
            if (!garageDir.exists()) garageDir.mkdirs()

            importList.forEach { bundle ->
                val zipId = bundle.vehicle.id
                val strategy = duplicateStrategy[zipId] ?: "IMPORT_NEW"
                
                var finalVehicle = bundle.vehicle
                var finalEvents = bundle.evenements
                var finalFuel = bundle.fuelReadings
                var finalKm = bundle.mileageReadings

                val alreadyExists = _garage.value.any { it.id == zipId }

                if (alreadyExists && strategy == "IMPORT_NEW") {
                    // Générer un nouvel ID pour éviter le conflit
                    val newId = UUID.randomUUID().toString()
                    finalVehicle = finalVehicle.copy(id = newId, model = "${finalVehicle.model} (Import)")
                    
                    // On doit aussi mettre à jour les chemins de fichiers dans les données XML pour ce véhicule
                    finalVehicle = finalVehicle.copy(
                        photoPath = finalVehicle.photoPath.replace(zipId, newId),
                        photos = finalVehicle.photos.map { it.copy(path = it.path.replace(zipId, newId)) },
                        mechanic = finalVehicle.mechanic.copy(
                            logoPath = finalVehicle.mechanic.logoPath.replace(zipId, newId),
                            photos = finalVehicle.mechanic.photos.map { it.copy(path = it.path.replace(zipId, newId)) }
                        ),
                        insurance = finalVehicle.insurance.copy(
                            logoPath = finalVehicle.insurance.logoPath.replace(zipId, newId),
                            photos = finalVehicle.insurance.photos.map { it.copy(path = it.path.replace(zipId, newId)) }
                        )
                    )
                    finalEvents = finalEvents.map { e -> 
                        e.copy(photos = e.photos.map { p -> p.copy(path = p.path.replace(zipId, newId)) }) 
                    }

                    // Copier le dossier de ressources vers le nouvel ID
                    val srcFolder = File(extractedGarage, zipId)
                    val dstFolder = File(garageDir, newId)
                    if (srcFolder.exists()) {
                        srcFolder.copyRecursively(dstFolder, overwrite = true)
                    }
                } else {
                    // Mode REPLACE ou Nouveau véhicule (pas de conflit)
                    // Supprimer l'ancien dossier si REPLACE
                    val targetFolder = File(garageDir, zipId)
                    if (targetFolder.exists()) targetFolder.deleteRecursively()
                    
                    val srcFolder = File(extractedGarage, zipId)
                    if (srcFolder.exists()) {
                        srcFolder.copyRecursively(targetFolder, overwrite = true)
                    }
                }

                saveSuiviAtomically(finalVehicle, finalKm, finalEvents, finalFuel)
            }

            refreshGarage()
            return truncated

        } finally {
            tempDir.deleteRecursively()
        }
    }

    fun importGlobalBackup(inputStream: InputStream): Boolean {
        // Pour un import global, on peeks d'abord pour avoir la liste des IDs
        val tempFile = File(context.cacheDir, "full_import_temp.zip")
        try {
            FileOutputStream(tempFile).use { fos ->
                inputStream.copyTo(fos)
            }
            
            val preview = FileInputStream(tempFile).use { peekBackup(it) }
            val ids = preview.vehicles.map { it.vehicle.id }
            val strategies = ids.associateWith { "REPLACE" }
            
            // --- NETTOYAGE INTÉGRAL AVANT RESTAURATION TOTALE ---
            // 1. Supprimer tous les fichiers XML de véhicules
            context.filesDir.listFiles { _, name -> name.startsWith("vehicle_") }?.forEach { it.delete() }
            
            // 2. Supprimer tout le dossier garage (photos/docs)
            val garageDir = File(context.filesDir, "garage")
            if (garageDir.exists()) {
                garageDir.deleteRecursively()
            }
            
            // 3. Réinitialiser les catégories (elles seront rechargées du ZIP ou par défaut)
            if (categoriesFile.exists()) categoriesFile.delete()
            // ----------------------------------------------------

            // Restaurer les réglages globaux du backup
            if (preview.multiVehicleEnabled) {
                setEnableMultiVehicle(true)
            }

            val result = FileInputStream(tempFile).use { importSelectedVehicles(it, ids, strategies) }
            
            // Forcer le rechargement du véhicule actif pour synchroniser le thème
            loadActiveVehicle()
            
            return result
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    fun resetAllData() {
        try {
            // 1. Supprimer les fichiers de données
            if (categoriesFile.exists()) categoriesFile.delete()
            context.filesDir.listFiles { _, name -> name.startsWith("vehicle_") }?.forEach { it.delete() }
            
            // 2. Supprimer les dossiers de documents
            val garageDir = File(context.filesDir, "garage")
            if (garageDir.exists()) garageDir.deleteRecursively()
            
            // 3. Effacer les préférences
            prefs.edit { clear() }
            
            // 4. Réinitialiser les StateFlows de réglages
            _isPro.value = false
            _enableMultiVehicle.value = false
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("active_vehicle_id", newId).apply()
            _activeVehicleId.value = newId

            // 5. Réinitialiser les StateFlows de données actives
            _vehicle.value = null
            _mileageReadings.value = emptyList()
            _evenements.value = emptyList()
            _fuelReadings.value = emptyList()
            
            // 6. Recharger les catégories par défaut et rafraîchir le garage
            setupDefaultCategories()
            loadCategories()
            refreshGarage()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Corrige les chemins absolus stockés dans la sauvegarde pour les adapter 
     * au répertoire interne de l'application actuelle.
     */
    private fun fixBackupPaths(backup: GlobalBackup): GlobalBackup {
        val garageDir = File(context.filesDir, "garage")
        
        val fixedVehicles = backup.vehicles.map { b ->
            val vehicleId = b.vehicle.id
            val vehicleFolder = File(garageDir, vehicleId)
            
            fun fixPath(oldPath: String?): String {
                if (oldPath.isNullOrBlank()) return ""
                val filename = File(oldPath).name
                return "garage/$vehicleId/$filename"
            }

            fun fixAttachments(list: List<Attachment>) = list.map { it.copy(path = fixPath(it.path)) }

            val v = b.vehicle
            val fixedVeh = v.copy(
                photoPath = fixPath(v.photoPath),
                photos = fixAttachments(v.photos),
                mechanic = v.mechanic.copy(
                    logoPath = fixPath(v.mechanic.logoPath),
                    photos = fixAttachments(v.mechanic.photos)
                ),
                insurance = v.insurance.copy(
                    logoPath = fixPath(v.insurance.logoPath),
                    photos = fixAttachments(v.insurance.photos)
                )
            )
            val fixedEvts = b.evenements.map { e -> e.copy(photos = fixAttachments(e.photos)) }
            b.copy(vehicle = fixedVeh, evenements = fixedEvts)
        }

        return backup.copy(vehicles = fixedVehicles)
    }
}
