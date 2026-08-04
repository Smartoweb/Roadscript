package com.roadscript.oss.data.xml

import com.roadscript.oss.data.models.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

data class GlobalBackup(
    val categories: List<Category>,
    val subCategories: List<SubCategory>,
    val vehicles: List<VehicleBundle>,
    val countryCode: String = "",
    val multiVehicleEnabled: Boolean = false
)

data class VehicleBundle(
    val vehicle: Vehicle,
    val mileageReadings: List<MileageReading>,
    val evenements: List<Evenement>,
    val fuelReadings: List<FuelReading>
)

object XmlDataParser {

    /**
     * Parse un flux XML global et retourne tous les objets de l'application.
     */
    fun parseGlobal(inputStream: InputStream): GlobalBackup {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        doc.documentElement.normalize()

        if (doc.documentElement.nodeName != "backup") {
            throw IllegalArgumentException("Format de sauvegarde global invalide.")
        }

        val countryCode = doc.documentElement.getAttribute("country") ?: ""
        val multiVehicleEnabled = doc.documentElement.getAttribute("multi_vehicle_enabled").toBoolean()

        // 1. Parser Catégories
        val categoriesElem = doc.getElementsByTagName("categories").item(0) as? Element
            ?: throw IllegalArgumentException("Section categories manquante.")
        val (cats, subs) = parseCategoriesFromElement(categoriesElem)

        // 2. Parser Suivi (Multi-véhicules supportés)
        val vehicleBundles = mutableListOf<VehicleBundle>()
        val suiviNodes = doc.getElementsByTagName("suivi")
        for (i in 0 until suiviNodes.length) {
            val element = suiviNodes.item(i) as Element
            val (_, veh, km, evts, fuel) = parseSuiviFromElement(element)
            if (veh != null) {
                vehicleBundles.add(VehicleBundle(veh, km, evts, fuel))
            }
        }

        return GlobalBackup(cats, subs, vehicleBundles, countryCode, multiVehicleEnabled)
    }

    /**
     * Sérialise toutes les données de l'application dans un seul flux XML.
     */
    fun writeGlobal(
        outputStream: OutputStream,
        categories: List<Category>,
        subCategories: List<SubCategory>,
        vehicles: List<VehicleBundle>,
        countryCode: String = "",
        multiVehicleEnabled: Boolean = false
    ) {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.newDocument()

        val root = doc.createElement("backup")
        if (countryCode.isNotBlank()) {
            root.setAttribute("country", countryCode)
        }
        root.setAttribute("multi_vehicle_enabled", multiVehicleEnabled.toString())
        doc.appendChild(root)

        // 1. Ajouter Catégories
        val catsRoot = doc.createElement("categories")
        root.appendChild(catsRoot)
        writeCategoriesIntoElement(doc, catsRoot, categories, subCategories)

        // 2. Ajouter les véhicules
        for (bundle in vehicles) {
            val suiviRoot = doc.createElement("suivi")
            suiviRoot.setAttribute("vehicle_id", bundle.vehicle.id)
            root.appendChild(suiviRoot)
            writeSuiviIntoElement(doc, suiviRoot, bundle.vehicle, bundle.mileageReadings, bundle.evenements, bundle.fuelReadings)
        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

        transformer.transform(DOMSource(doc), StreamResult(outputStream))
    }

    // --- Helpers pour factoriser le code existant ---

    private fun parseCategoriesFromElement(root: Element): Pair<List<Category>, List<SubCategory>> {
        val categories = mutableListOf<Category>()
        val subCategories = mutableListOf<SubCategory>()

        val nodeList = root.childNodes
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                when (element.nodeName) {
                    "category" -> {
                        val key = element.getAttribute("key")
                        val icon = element.getAttribute("icon")
                        val isDefault = element.getAttribute("is_default").toBoolean()
                        val name = element.getElementsByTagName("name").item(0)?.textContent ?: ""
                        val description = element.getElementsByTagName("description").item(0)?.textContent ?: ""
                        if (key.isNotBlank() && name.isNotBlank()) {
                            categories.add(Category(key, name, description, icon, isDefault))
                        }
                    }
                    "subcategory" -> {
                        val key = element.getAttribute("key")
                        val categoryKey = element.getAttribute("categoryKey")
                        val icon = element.getAttribute("icon")
                        val isDefault = element.getAttribute("is_default").toBoolean()
                        val countryCode = element.getAttribute("country").takeIf { it.isNotBlank() }
                        val name = element.getElementsByTagName("name").item(0)?.textContent ?: ""
                        val description = element.getElementsByTagName("description").item(0)?.textContent ?: ""
                        if (key.isNotBlank() && categoryKey.isNotBlank() && name.isNotBlank()) {
                            subCategories.add(SubCategory(key, categoryKey, name, description, icon, isDefault, countryCode))
                        }
                    }
                }
            }
        }
        return Pair(categories, subCategories)
    }

    private fun parseSuiviFromElement(root: Element): Tuple5<String, Vehicle?, List<MileageReading>, List<Evenement>, List<FuelReading>> {
        val vehicleId = root.getAttribute("vehicle_id")
        if (vehicleId.isBlank()) {
            throw IllegalArgumentException("Attribut 'vehicle_id' manquant ou vide dans le fichier XML.")
        }
        var vehicle: Vehicle? = null
        val mileageList = mutableListOf<MileageReading>()
        val evenementList = mutableListOf<Evenement>()
        val fuelList = mutableListOf<FuelReading>()

        // Véhicule
        val vehNodes = root.getElementsByTagName("vehicule")
        if (vehNodes.length > 0) {
            val element = vehNodes.item(0) as Element
            val model = element.getElementsByTagName("modele").item(0)?.textContent ?: ""
            if (model.isNotBlank()) {
                val brand = element.getElementsByTagName("marque").item(0)?.textContent ?: ""
                val plateNumber = element.getElementsByTagName("immatriculation").item(0)?.textContent ?: ""
                val firstRegistrationDate = element.getElementsByTagName("date_premiere_immat").item(0)?.textContent ?: ""
                val vin = element.getElementsByTagName("vin").item(0)?.textContent ?: ""
                val type = element.getElementsByTagName("genre").item(0)?.textContent ?: ""
                val fiscalPower = element.getElementsByTagName("puissance_fiscale").item(0)?.textContent?.toIntOrNull()
                val fuelType = element.getElementsByTagName("carburant").item(0)?.textContent ?: ""
                val themeKey = element.getAttribute("theme_key").ifBlank { "INDIGO" }
                val photoPath = element.getElementsByTagName("photo_path").item(0)?.textContent ?: ""
                val acquisitionDate = element.getElementsByTagName("date_acquisition").item(0)?.textContent ?: ""
                val acquisitionMileage = element.getElementsByTagName("km_acquisition").item(0)?.textContent?.toIntOrNull() ?: 0
                val countryCode = element.getElementsByTagName("country_code").item(0)?.textContent ?: ""

                val vPhotos = mutableListOf<Attachment>()
                val vPhotoNodes = element.getElementsByTagName("photo_vehicule")
                for (j in 0 until vPhotoNodes.length) {
                    val node = vPhotoNodes.item(j) as Element
                    val path = node.textContent ?: ""
                    val label = node.getAttribute("label")
                    if (path.isNotBlank()) vPhotos.add(Attachment(path, label))
                }

                val mechanicElem = element.getElementsByTagName("garagiste").item(0) as? Element
                val mechanic = if (mechanicElem != null) {
                    val mPhotos = mutableListOf<Attachment>()
                    val mPhotoNodes = mechanicElem.getElementsByTagName("photo_garagiste")
                    for (j in 0 until mPhotoNodes.length) {
                        val node = mPhotoNodes.item(j) as Element
                        val path = node.textContent ?: ""
                        val label = node.getAttribute("label")
                        if (path.isNotBlank()) mPhotos.add(Attachment(path, label))
                    }

                    MechanicInfo(
                        logoPath = mechanicElem.getElementsByTagName("logo").item(0)?.textContent ?: "",
                        name = mechanicElem.getElementsByTagName("nom").item(0)?.textContent ?: "",
                        address = mechanicElem.getElementsByTagName("adresse").item(0)?.textContent ?: "",
                        phone = mechanicElem.getElementsByTagName("tel").item(0)?.textContent ?: "",
                        email = mechanicElem.getElementsByTagName("email").item(0)?.textContent ?: "",
                        website = mechanicElem.getElementsByTagName("web").item(0)?.textContent ?: "",
                        customerNumber = mechanicElem.getElementsByTagName("num_client").item(0)?.textContent ?: "",
                        contractNumber = mechanicElem.getElementsByTagName("num_contrat").item(0)?.textContent ?: "",
                        photos = mPhotos
                    )
                } else MechanicInfo()

                val insuranceElem = element.getElementsByTagName("assurance").item(0) as? Element
                val insurance = if (insuranceElem != null) {
                    val iPhotos = mutableListOf<Attachment>()
                    val iPhotoNodes = insuranceElem.getElementsByTagName("photo_assurance")
                    for (j in 0 until iPhotoNodes.length) {
                        val node = iPhotoNodes.item(j) as Element
                        val path = node.textContent ?: ""
                        val label = node.getAttribute("label")
                        if (path.isNotBlank()) iPhotos.add(Attachment(path, label))
                    }

                    InsuranceInfo(
                        logoPath = insuranceElem.getElementsByTagName("logo").item(0)?.textContent ?: "",
                        name = insuranceElem.getElementsByTagName("nom").item(0)?.textContent ?: "",
                        address = insuranceElem.getElementsByTagName("adresse").item(0)?.textContent ?: "",
                        phone = insuranceElem.getElementsByTagName("tel").item(0)?.textContent ?: "",
                        email = insuranceElem.getElementsByTagName("email").item(0)?.textContent ?: "",
                        website = insuranceElem.getElementsByTagName("web").item(0)?.textContent ?: "",
                        contractNumber = insuranceElem.getElementsByTagName("contrat").item(0)?.textContent ?: "",
                        customerNumber = insuranceElem.getElementsByTagName("num_client").item(0)?.textContent ?: "",
                        photos = iPhotos
                    )
                } else InsuranceInfo()

                vehicle = Vehicle(
                    vehicleId, model, brand, plateNumber, firstRegistrationDate, vin, type, 
                    fiscalPower, fuelType, themeKey, photoPath, acquisitionDate, acquisitionMileage,
                    countryCode, vPhotos, mechanic, insurance
                )
            }
        }

        // Kilomètres
        val kmContainer = root.getElementsByTagName("kilometres").item(0) as? Element
        if (kmContainer != null) {
            val releveNodes = kmContainer.getElementsByTagName("releve")
            for (i in 0 until releveNodes.length) {
                val element = releveNodes.item(i) as Element
                val date = element.getAttribute("date")
                val value = element.textContent?.toIntOrNull()
                if (date.isNotBlank() && value != null) {
                    mileageList.add(MileageReading(date, value))
                }
            }
        }

        // Carburant
        val fuelContainer = root.getElementsByTagName("carburant").item(0) as? Element
        if (fuelContainer != null) {
            val pleinNodes = fuelContainer.getElementsByTagName("plein")
            for (i in 0 until pleinNodes.length) {
                val element = pleinNodes.item(i) as Element
                val date = element.getAttribute("date")
                val litres = element.getAttribute("litres").toDoubleOrNull() ?: 0.0
                val cout = element.getAttribute("cout").toDoubleOrNull() ?: 0.0
                val fCountry = element.getAttribute("country").takeIf { it.isNotBlank() }
                if (date.isNotBlank()) {
                    fuelList.add(FuelReading(date, litres, cout, fCountry))
                }
            }
        }

        // Événements (Nouveau format)
        val evenementNodes = root.getElementsByTagName("evenement")
        for (i in 0 until evenementNodes.length) {
            val element = evenementNodes.item(i) as Element
            val id = element.getAttribute("id")
            val eCountry = element.getAttribute("country").takeIf { it.isNotBlank() }
            val name = element.getElementsByTagName("nom").item(0)?.textContent ?: ""
            val date = element.getElementsByTagName("date").item(0)?.textContent ?: ""
            val catKey = element.getElementsByTagName("category_key").item(0)?.textContent ?: ""
            val subCatKey = element.getElementsByTagName("subcategory_key").item(0)?.textContent ?: ""
            val cost = element.getElementsByTagName("cout").item(0)?.textContent?.toDoubleOrNull() ?: 0.0
            val desc = element.getElementsByTagName("description").item(0)?.textContent ?: ""
            val isMultiple = element.getAttribute("is_multiple").toBoolean()

            // Actions (anciennement Tâches)
            val actions = mutableListOf<EvenementAction>()
            val actionNodes = element.getElementsByTagName("action_interne") // Nom différent pour éviter confusion avec racine
            for (j in 0 until actionNodes.length) {
                val actionElem = actionNodes.item(j) as Element
                val aName = actionElem.getElementsByTagName("nom_interne").item(0)?.textContent ?: ""
                val catKey = actionElem.getAttribute("category_key")
                val subCatKey = actionElem.getAttribute("subcategory_key")
                val aCountry = actionElem.getAttribute("country").takeIf { it.isNotBlank() }
                if (catKey.isNotBlank()) {
                    actions.add(EvenementAction(aName, catKey, subCatKey, aCountry))
                }
            }

            // Photos/Docs
            val photos = mutableListOf<Attachment>()
            val photoNodes = element.getElementsByTagName("photo")
            for (j in 0 until photoNodes.length) {
                val node = photoNodes.item(j) as Element
                val path = node.textContent ?: ""
                val label = node.getAttribute("label")
                if (path.isNotBlank()) photos.add(Attachment(path, label))
            }

            if (id.isNotBlank() && name.isNotBlank() && date.isNotBlank()) {
                evenementList.add(Evenement(id, name, date, catKey, subCatKey, cost, desc, isMultiple, actions, photos, eCountry))
            }
        }

        return Tuple5(vehicleId, vehicle, mileageList, evenementList, fuelList)
    }

    private fun writeCategoriesIntoElement(doc: org.w3c.dom.Document, root: Element, categories: List<Category>, subCategories: List<SubCategory>) {
        for (cat in categories) {
            val catElem = doc.createElement("category")
            catElem.setAttribute("key", cat.key)
            catElem.setAttribute("icon", cat.icon)
            catElem.setAttribute("is_default", cat.isDefault.toString())
            val nameElem = doc.createElement("name"); nameElem.textContent = cat.name; catElem.appendChild(nameElem)
            val descElem = doc.createElement("description"); descElem.textContent = cat.description; catElem.appendChild(descElem)
            root.appendChild(catElem)
        }
        for (sub in subCategories) {
            val subElem = doc.createElement("subcategory")
            subElem.setAttribute("key", sub.key)
            subElem.setAttribute("categoryKey", sub.categoryKey)
            subElem.setAttribute("icon", sub.icon)
            subElem.setAttribute("is_default", sub.isDefault.toString())
            if (!sub.countryCode.isNullOrBlank()) {
                subElem.setAttribute("country", sub.countryCode)
            }
            val nameElem = doc.createElement("name"); nameElem.textContent = sub.name; subElem.appendChild(nameElem)
            val descElem = doc.createElement("description"); descElem.textContent = sub.description; subElem.appendChild(descElem)
            root.appendChild(subElem)
        }
    }

    private fun writeSuiviIntoElement(doc: org.w3c.dom.Document, root: Element, vehicle: Vehicle?, km: List<MileageReading>, evts: List<Evenement>, fuel: List<FuelReading>) {
        if (vehicle != null) {
            val vehElem = doc.createElement("vehicule")
            vehElem.setAttribute("theme_key", vehicle.themeKey)
            doc.createElement("modele").apply { textContent = vehicle.model; vehElem.appendChild(this) }
            doc.createElement("marque").apply { textContent = vehicle.brand; vehElem.appendChild(this) }
            doc.createElement("immatriculation").apply { textContent = vehicle.plateNumber; vehElem.appendChild(this) }
            doc.createElement("date_premiere_immat").apply { textContent = vehicle.firstRegistrationDate; vehElem.appendChild(this) }
            doc.createElement("vin").apply { textContent = vehicle.vin; vehElem.appendChild(this) }
            doc.createElement("genre").apply { textContent = vehicle.type; vehElem.appendChild(this) }
            doc.createElement("puissance_fiscale").apply { textContent = vehicle.fiscalPower?.toString() ?: ""; vehElem.appendChild(this) }
            doc.createElement("carburant").apply { textContent = vehicle.fuelType; vehElem.appendChild(this) }
            doc.createElement("photo_path").apply { textContent = vehicle.photoPath; vehElem.appendChild(this) }
            doc.createElement("date_acquisition").apply { textContent = vehicle.acquisitionDate; vehElem.appendChild(this) }
            doc.createElement("km_acquisition").apply { textContent = vehicle.acquisitionMileage.toString(); vehElem.appendChild(this) }
            doc.createElement("country_code").apply { textContent = vehicle.countryCode; vehElem.appendChild(this) }

            // Photos véhicule
            for (p in vehicle.photos) {
                doc.createElement("photo_vehicule").apply { 
                    textContent = p.path
                    setAttribute("label", p.label)
                    vehElem.appendChild(this) 
                }
            }

            val garElem = doc.createElement("garagiste")
            doc.createElement("logo").apply { textContent = vehicle.mechanic.logoPath; garElem.appendChild(this) }
            doc.createElement("nom").apply { textContent = vehicle.mechanic.name; garElem.appendChild(this) }
            doc.createElement("adresse").apply { textContent = vehicle.mechanic.address; garElem.appendChild(this) }
            doc.createElement("tel").apply { textContent = vehicle.mechanic.phone; garElem.appendChild(this) }
            doc.createElement("email").apply { textContent = vehicle.mechanic.email; garElem.appendChild(this) }
            doc.createElement("web").apply { textContent = vehicle.mechanic.website; garElem.appendChild(this) }
            doc.createElement("num_client").apply { textContent = vehicle.mechanic.customerNumber; garElem.appendChild(this) }
            doc.createElement("num_contrat").apply { textContent = vehicle.mechanic.contractNumber; garElem.appendChild(this) }
            
            // Photos garagiste
            for (p in vehicle.mechanic.photos) {
                doc.createElement("photo_garagiste").apply { 
                    textContent = p.path
                    setAttribute("label", p.label)
                    garElem.appendChild(this) 
                }
            }
            vehElem.appendChild(garElem)

            val assElem = doc.createElement("assurance")
            doc.createElement("logo").apply { textContent = vehicle.insurance.logoPath; assElem.appendChild(this) }
            doc.createElement("nom").apply { textContent = vehicle.insurance.name; assElem.appendChild(this) }
            doc.createElement("adresse").apply { textContent = vehicle.insurance.address; assElem.appendChild(this) }
            doc.createElement("tel").apply { textContent = vehicle.insurance.phone; assElem.appendChild(this) }
            doc.createElement("email").apply { textContent = vehicle.insurance.email; assElem.appendChild(this) }
            doc.createElement("web").apply { textContent = vehicle.insurance.website; assElem.appendChild(this) }
            doc.createElement("contrat").apply { textContent = vehicle.insurance.contractNumber; assElem.appendChild(this) }
            doc.createElement("num_client").apply { textContent = vehicle.insurance.customerNumber; assElem.appendChild(this) }
            
            // Photos assurance
            for (p in vehicle.insurance.photos) {
                doc.createElement("photo_assurance").apply { 
                    textContent = p.path
                    setAttribute("label", p.label)
                    assElem.appendChild(this) 
                }
            }
            vehElem.appendChild(assElem)

            root.appendChild(vehElem)
        }

        val kmRoot = doc.createElement("kilometres")
        for (r in km) {
            val rel = doc.createElement("releve")
            rel.setAttribute("date", r.date)
            rel.textContent = r.value.toString()
            kmRoot.appendChild(rel)
        }
        root.appendChild(kmRoot)

        val fuelRoot = doc.createElement("carburant")
        for (f in fuel) {
            val p = doc.createElement("plein")
            p.setAttribute("date", f.date)
            p.setAttribute("litres", f.liters.toString())
            p.setAttribute("cout", f.cost.toString())
            if (!f.countryCode.isNullOrBlank()) {
                p.setAttribute("country", f.countryCode)
            }
            fuelRoot.appendChild(p)
        }
        root.appendChild(fuelRoot)

        val evtsRoot = doc.createElement("evenements")
        for (e in evts) {
            val evtElem = doc.createElement("evenement")
            evtElem.setAttribute("id", e.id)
            evtElem.setAttribute("is_multiple", e.isMultiple.toString())
            if (!e.countryCode.isNullOrBlank()) {
                evtElem.setAttribute("country", e.countryCode)
            }
            doc.createElement("nom").apply { textContent = e.name; evtElem.appendChild(this) }
            doc.createElement("date").apply { textContent = e.date; evtElem.appendChild(this) }
            doc.createElement("category_key").apply { textContent = e.categoryKey; evtElem.appendChild(this) }
            doc.createElement("subcategory_key").apply { textContent = e.subCategoryKey; evtElem.appendChild(this) }
            doc.createElement("cout").apply { textContent = e.cost.toString(); evtElem.appendChild(this) }
            doc.createElement("description").apply { textContent = e.description; evtElem.appendChild(this) }
            
            // Actions internes
            val actionsRoot = doc.createElement("actions")
            for (a in e.actions) {
                val aElem = doc.createElement("action_interne")
                aElem.setAttribute("category_key", a.categoryKey)
                aElem.setAttribute("subcategory_key", a.subCategoryKey)
                if (!a.countryCode.isNullOrBlank()) {
                    aElem.setAttribute("country", a.countryCode)
                }
                doc.createElement("nom_interne").apply { textContent = a.name; aElem.appendChild(this) }
                actionsRoot.appendChild(aElem)
            }
            evtElem.appendChild(actionsRoot)

            // Photos
            val photosRoot = doc.createElement("photos")
            for (p in e.photos) {
                doc.createElement("photo").apply { 
                    textContent = p.path
                    setAttribute("label", p.label)
                    photosRoot.appendChild(this) 
                }
            }
            evtElem.appendChild(photosRoot)
            
            evtsRoot.appendChild(evtElem)
        }
        root.appendChild(evtsRoot)
    }

    /**
     * Parse categories.xml et retourne la liste des catégories et des sous-catégories.
     */
    fun parseCategories(inputStream: InputStream): Pair<List<Category>, List<SubCategory>> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        doc.documentElement.normalize()
        return parseCategoriesFromElement(doc.documentElement)
    }

    /**
     * Sérialise et écrit les catégories et sous-catégories dans le flux de sortie.
     */
    fun writeCategories(outputStream: OutputStream, categories: List<Category>, subCategories: List<SubCategory>) {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.newDocument()
        val root = doc.createElement("categories")
        doc.appendChild(root)
        writeCategoriesIntoElement(doc, root, categories, subCategories)

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.transform(DOMSource(doc), StreamResult(outputStream))
    }

    /**
     * Parse un fichier XML de suivi et retourne le véhicule, les relevés et les événements.
     */
    fun parseSuivi(inputStream: InputStream): Tuple5<String, Vehicle?, List<MileageReading>, List<Evenement>, List<FuelReading>> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        doc.documentElement.normalize()
        return parseSuiviFromElement(doc.documentElement)
    }

    /**
     * Sérialise et écrit les données de suivi (véhicule, kilomètres, événements) dans le flux de sortie.
     */
    fun writeSuivi(outputStream: OutputStream, vehicle: Vehicle?, mileageReadings: List<MileageReading>, evenements: List<Evenement>, fuelReadings: List<FuelReading>) {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.newDocument()
        val root = doc.createElement("suivi")
        if (vehicle != null) {
            root.setAttribute("vehicle_id", vehicle.id)
        }
        doc.appendChild(root)
        writeSuiviIntoElement(doc, root, vehicle, mileageReadings, evenements, fuelReadings)

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.transform(DOMSource(doc), StreamResult(outputStream))
    }
}

data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
