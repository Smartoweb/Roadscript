package com.roadscript.oss.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Attachment(
    val path: String,
    val label: String = ""
) : Parcelable

@Parcelize
data class Vehicle(
    val id: String = java.util.UUID.randomUUID().toString(), // Identifiant unique du véhicule
    val model: String = "", // Modèle commercial
    val brand: String = "", // Marque
    val plateNumber: String = "", // Numéro d'immatriculation
    val firstRegistrationDate: String = "", // Date de première immatriculation YYYY-MM-DD
    val vin: String = "", // Numéro de châssis / VIN
    val type: String = "", // Genre national (ex: VP)
    val fiscalPower: Int? = null, // Puissance fiscale
    val fuelType: String = "", // Carburant (Essence, Diesel, Électrique, Hybride...)
    val themeKey: String = "INDIGO", // Thème visuel associé (INDIGO, BLUE, GREEN, RED, GREY)
    val photoPath: String = "", // Chemin local vers la photo du véhicule
    val acquisitionDate: String = "", // Date d'achat/possession YYYY-MM-DD
    val acquisitionMileage: Int = 0, // KM lors de l'achat
    val countryCode: String = "", // Code pays (ISO 3166-1 alpha-2) pour les juridictions
    val photos: List<Attachment> = emptyList(), // Documents associés au véhicule
    val mechanic: MechanicInfo = MechanicInfo(),
    val insurance: InsuranceInfo = InsuranceInfo()
) : Parcelable

@Parcelize
data class MechanicInfo(
    val logoPath: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val customerNumber: String = "",
    val contractNumber: String = "",
    val photos: List<Attachment> = emptyList() // Documents associés au garage
) : Parcelable

@Parcelize
data class InsuranceInfo(
    val logoPath: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val contractNumber: String = "",
    val customerNumber: String = "",
    val photos: List<Attachment> = emptyList() // Documents associés à l'assurance
) : Parcelable

@Parcelize
data class MileageReading(
    val date: String, // YYYY-MM-DD
    val value: Int // Relevé kilométrique
) : Parcelable

@Parcelize
data class FuelReading(
    val date: String, // YYYY-MM-DD
    val liters: Double,
    val cost: Double,
    val countryCode: String? = null
) : Parcelable

@Parcelize
data class EvenementAction(
    val name: String,
    val categoryKey: String,
    val subCategoryKey: String,
    val countryCode: String? = null
) : Parcelable

@Parcelize
data class Evenement(
    val id: String, // UUID
    val name: String,
    val date: String, // YYYY-MM-DD
    val categoryKey: String,
    val subCategoryKey: String,
    val cost: Double,
    val description: String,
    val isMultiple: Boolean = false,
    val actions: List<EvenementAction> = emptyList(),
    val photos: List<Attachment> = emptyList(), // Documents de l'événement
    val countryCode: String? = null
) : Parcelable
