package com.roadscript.oss.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    /**
     * Retourne l'icône Material correspondante pour une clé donnée.
     * Si la clé n'est pas connue, renvoie une icône de voiture par défaut.
     */
    fun getIcon(iconKey: String): ImageVector {
        return when (iconKey) {
            "ic_maintenance" -> Icons.Default.Build
            "ic_repair" -> Icons.Default.CarRepair
            "ic_accessories" -> Icons.Default.Extension
            "ic_inspection" -> Icons.Default.FactCheck
            
            "ic_oil" -> Icons.Default.Opacity
            "ic_tire" -> Icons.Default.TireRepair
            "ic_brake" -> Icons.Default.RadioButtonChecked
            "ic_filter" -> Icons.Default.FilterAlt
            
            "ic_engine" -> Icons.Default.Settings
            "ic_battery" -> Icons.Default.BatteryChargingFull
            "ic_electric" -> Icons.Default.ElectricBolt
            
            "ic_audio" -> Icons.Default.Radio
            "ic_seat" -> Icons.Default.Chair
            
            "ic_calendar" -> Icons.Default.CalendarToday
            "ic_warning" -> Icons.Default.Warning
            
            else -> Icons.Default.DirectionsCar
        }
    }
    
    /**
     * Liste des icônes disponibles pour l'utilisateur dans l'interface de création/modification.
     */
    val availableIcons = listOf(
        Pair("ic_maintenance", "Entretien (Clé)"),
        Pair("ic_repair", "Réparation (Garage)"),
        Pair("ic_accessories", "Accessoires (Puzzle)"),
        Pair("ic_inspection", "Contrôle (Validation)"),
        Pair("ic_oil", "Huile / Goutte"),
        Pair("ic_tire", "Pneumatiques"),
        Pair("ic_brake", "Freinage (Cercle)"),
        Pair("ic_filter", "Filtration"),
        Pair("ic_engine", "Moteur (Engrenage)"),
        Pair("ic_battery", "Batterie"),
        Pair("ic_electric", "Électricité (Éclair)"),
        Pair("ic_audio", "Radio / Audio"),
        Pair("ic_seat", "Confort / Siège"),
        Pair("ic_calendar", "Calendrier"),
        Pair("ic_warning", "Alerte / Warning")
    )
}
