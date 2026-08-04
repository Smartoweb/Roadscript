package com.roadscript.oss.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadscript.oss.data.models.SubCategory
import androidx.compose.ui.res.stringResource
import com.roadscript.oss.R
import com.roadscript.oss.ui.VehicleViewModel
import com.roadscript.oss.ui.utils.TranslationHelper
import com.roadscript.oss.ui.components.CategoryIconMapper
import com.roadscript.oss.ui.components.IconSelectorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val subCategories by viewModel.subCategories.collectAsState()
    val evenements by viewModel.evenements.collectAsState()
    val enableMultiVehicle by viewModel.enableMultiVehicle.collectAsState()

    // On utilise la liste de "Entretien" comme référence pour la liste partagée
    val sharedSubs = remember(subCategories) {
        subCategories.filter { it.categoryKey == "entretien" }
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var subToDelete by remember { mutableStateOf<SubCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_task_list), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cat_btn_add))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val headerText = if (enableMultiVehicle) {
                    stringResource(R.string.cat_shared_domains_desc)
                } else {
                    stringResource(R.string.cat_shared_domains_desc_single)
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }

            item {
                // Légende des icônes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFFB0B0B0), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.cat_system_legend), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF909090), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.cat_user_legend), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            items(sharedSubs, key = { it.key }) { sub ->
                // Vérifier si cette tâche est utilisée (en Entretien ou Dépannage)
                val isUsed = evenements.any { evt -> 
                    evt.actions.any { it.subCategoryKey == sub.key || it.subCategoryKey == sub.key.replace("ent_", "dep_") } 
                    || evt.subCategoryKey == sub.key 
                    || evt.subCategoryKey == sub.key.replace("ent_", "dep_")
                }

                SharedTaskRowCard(
                    subCategory = sub,
                    isUsed = isUsed,
                    onClickDelete = { subToDelete = sub }
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showAddDialog) {
        SubCategoryEditDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, desc, icon ->
                viewModel.addSharedSubCategory(
                    name = name,
                    description = desc,
                    icon = icon,
                    onSuccess = {
                        showAddDialog = false
                        Toast.makeText(context, context.getString(R.string.msg_domain_added), Toast.LENGTH_SHORT).show()
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (subToDelete != null) {
        AlertDialog(
            onDismissRequest = { subToDelete = null },
            title = { Text(stringResource(R.string.cat_delete_title)) },
            text = { Text(stringResource(R.string.cat_delete_confirm, subToDelete!!.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSharedSubCategory(
                            name = subToDelete!!.name,
                            onSuccess = {
                                subToDelete = null
                                Toast.makeText(context, context.getString(R.string.msg_domain_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { subToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun SharedTaskRowCard(
    subCategory: SubCategory,
    isUsed: Boolean,
    onClickDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CategoryIconMapper.getIcon(subCategory.icon),
                contentDescription = null,
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TranslationHelper.getSubCategoryName(context, subCategory.key, subCategory.name),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (subCategory.isDefault) Icons.Default.VerifiedUser else Icons.Default.Person,
                        contentDescription = if (subCategory.isDefault) stringResource(R.string.cat_system) else stringResource(R.string.cat_user),
                        tint = if (subCategory.isDefault) Color(0xFFCCCCCC) else Color(0xFF909090),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = subCategory.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }

            if (!subCategory.isDefault && !isUsed) {
                IconButton(onClick = onClickDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red.copy(alpha = 0.7f))
                }
            } else if (isUsed) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Utilisé",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp).padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SubCategoryEditDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("ic_maintenance") }
    var selectIconExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cat_new_domain)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 64) name = it },
                    label = { Text(stringResource(R.string.cat_name_req)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 255) description = it },
                    label = { Text(stringResource(R.string.cat_desc_free)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Text(stringResource(R.string.category_icon_label), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectIconExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(CategoryIconMapper.getIcon(icon), null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.cat_choose_icon), modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                if (selectIconExpanded) {
                    IconSelectorDialog(
                        selectedIconKey = icon,
                        onSelect = { icon = it; selectIconExpanded = false },
                        onDismiss = { selectIconExpanded = false }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name.trim(), description.trim(), icon) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
