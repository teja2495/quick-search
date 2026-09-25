package com.tk.quicksearch.search.apps

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.intentHelpers.IntentHelpers
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.components.AppBottomPopup
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIconOverrideDrawer(
    packageName: String,
    appName: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val drawerHeight = LocalConfiguration.current.screenHeightDp.dp * 0.75f
    val iconContainerColor = AppColors.getSettingsCardContainerColor()
    val appliedIconPackPackage = remember(context) {
        UserAppPreferences(context).getSelectedIconPackPackage()
    }
    val iconOverride = remember(context, packageName) {
        UserAppPreferences(context).getAppIconOverride(packageName)
    }
    val hasAppliedIconPackIcon by produceState(
        initialValue = false,
        appliedIconPackPackage,
        packageName,
        iconOverride,
    ) {
        value =
            withContext(Dispatchers.IO) {
                appliedIconPackPackage != null &&
                    iconOverride?.useSystemDefault != true &&
                    IconPackManager.hasExplicitIcon(context, appliedIconPackPackage, packageName)
            }
    }
    val canResetToDefault = iconOverride?.useSystemDefault != true &&
        (iconOverride != null || hasAppliedIconPackIcon)
    val iconPacks by produceState(emptyList(), context) {
        value = withContext(Dispatchers.IO) { IconPackManager.findInstalledIconPacks(context) }
    }
    var selectedPackPackage by remember { mutableStateOf<String?>(null) }
    var query by remember(selectedPackPackage) { mutableStateOf("") }
    LaunchedEffect(iconPacks, selectedPackPackage) {
        val installedPackages = iconPacks.map { it.packageName }.toSet()
        if (selectedPackPackage !in installedPackages) {
            selectedPackPackage =
                appliedIconPackPackage?.takeIf { it in installedPackages }
                    ?: iconPacks.firstOrNull()?.packageName
        }
    }
    val selectedPack = iconPacks.firstOrNull { it.packageName == selectedPackPackage }

    AppBottomPopup(
        onDismiss = onDismiss,
        containerColor = AppColors.DialogBackground,
        contentCardColor = AppColors.DialogBackground,
        title = {
            Text(
                text = stringResource(R.string.icon_picker_title, appName),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        drawerHeight = drawerHeight,
        contentSpacing = DesignTokens.SpacingMedium,
        contentScrollable = false,
        aboveCardContent = {
            if (iconPacks.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.icon_picker_search)) },
                        shape = RoundedCornerShape(32.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = iconContainerColor,
                                unfocusedContainerColor = iconContainerColor,
                                disabledContainerColor = iconContainerColor,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        iconPacks.forEach { iconPack ->
                            val selected = iconPack.packageName == selectedPackPackage
                            Text(
                                text = iconPack.label,
                                modifier =
                                    Modifier
                                        .clickable { selectedPackPackage = iconPack.packageName }
                                        .padding(
                                            horizontal = DesignTokens.SpacingMedium,
                                            vertical = DesignTokens.SpacingXSmall,
                                        ),
                                color =
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (selected) 1f else 0.34f,
                                    ),
                                style =
                                    if (selected) MaterialTheme.typography.labelMedium
                                    else MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            if (selectedPack == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXLarge),
                    ) {
                        Text(
                            text = stringResource(R.string.icon_picker_no_icon_packs),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = {
                                onDismiss()
                                IntentHelpers.openSearchUrl(
                                    context = context.applicationContext as Application,
                                    query = context.getString(R.string.settings_icon_pack_search_query),
                                    searchEngine = SearchEngine.GOOGLE_PLAY,
                                )
                            },
                        ) {
                            Text(stringResource(R.string.icon_picker_download_icon_packs))
                        }
                    }
                }
            } else {
                val pack = selectedPack
                val iconDrawables by produceState(emptyList<IconPackDrawableInfo>(), pack.packageName) {
                    value = withContext(Dispatchers.IO) {
                        IconPackManager.getIconDrawables(context, pack.packageName)
                    }
                }
                val installedAppLabels by produceState(emptyMap<String, String>(), context) {
                    value = withContext(Dispatchers.IO) {
                        val packageManager = context.packageManager
                        val installedApps =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                packageManager.getInstalledApplications(
                                    PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                packageManager.getInstalledApplications(PackageManager.MATCH_ALL)
                            }
                        installedApps.associate { appInfo ->
                            appInfo.packageName to packageManager.getApplicationLabel(appInfo).toString()
                        }
                    }
                }
                var unavailableDrawableNames by remember(pack.packageName) {
                    mutableStateOf(emptySet<String>())
                }
                val filteredIconDrawables = remember(
                    iconDrawables,
                    query,
                    installedAppLabels,
                    unavailableDrawableNames,
                ) {
                    filterIconPackDrawables(
                        iconDrawables = iconDrawables,
                        query = query,
                        installedAppLabels = installedAppLabels,
                    ).filterNot { it.drawableName in unavailableDrawableNames }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    items(filteredIconDrawables, key = { it.drawableName }) { icon ->
                        IconPackDrawable(
                            iconPackPackage = pack.packageName,
                            drawableName = icon.drawableName,
                            onUnavailable = {
                                unavailableDrawableNames += icon.drawableName
                            },
                            onClick = {
                                UserAppPreferences(context).setAppIconOverride(
                                    packageName = packageName,
                                    iconPackPackage = pack.packageName,
                                    drawableName = icon.drawableName,
                                )
                                invalidateAppIconCache()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (canResetToDefault) {
                Button(
                    onClick = {
                        UserAppPreferences(context).setAppIconToSystemDefault(packageName)
                        invalidateAppIconCache()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_launcher_icon_reset_default))
                }
            }
        }
    }
}

@Composable
private fun IconPackDrawable(
    iconPackPackage: String,
    drawableName: String,
    onUnavailable: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, iconPackPackage, drawableName) {
        val loadedIcon = withContext(Dispatchers.IO) {
            IconPackManager.loadDrawableBitmap(context, iconPackPackage, drawableName)
        }
        value = loadedIcon
        if (loadedIcon == null) onUnavailable()
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Box(
            modifier = Modifier.padding(DesignTokens.SpacingSmall),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = drawableName,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}
