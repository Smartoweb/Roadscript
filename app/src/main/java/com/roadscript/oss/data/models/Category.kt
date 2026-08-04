package com.roadscript.oss.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val key: String,
    val name: String,
    val description: String,
    val icon: String,
    val isDefault: Boolean = false,
    val countryCode: String? = null
) : Parcelable {
    init {
        require(name.length <= 64) { "Le nom de la catégorie ne doit pas dépasser 64 caractères." }
        require(description.length <= 512) { "La description de la catégorie ne doit pas dépasser 512 caractères." }
    }
}

@Parcelize
data class SubCategory(
    val key: String,
    val categoryKey: String,
    val name: String,
    val description: String,
    val icon: String,
    val isDefault: Boolean = false,
    val countryCode: String? = null
) : Parcelable {
    init {
        require(name.length <= 64) { "Le nom de la sous-catégorie ne doit pas dépasser 64 caractères." }
        require(description.length <= 512) { "La description de la sous-catégorie ne doit pas dépasser 512 caractères." }
    }
}
