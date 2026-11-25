package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppIconOverride
import com.example.model.ExternalIconPackInfo
import com.example.model.IconPackConfig
import com.example.model.LaunchableAppInfo
import com.example.widget.ExternalPackEntry
import com.example.widget.IconPackImporter
import com.example.widget.IconPackRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberStyledAppBitmap(
  app: LaunchableAppInfo,
  config: IconPackConfig,
  override: AppIconOverride?,
  externalPack: ExternalIconPackInfo? = null,
  sizePx: Int = 160,
): Bitmap? {
  val context = LocalContext.current
  val state by produceState<Bitmap?>(
    initialValue = null,
    key1 = app.componentKey,
    key2 = config,
    key3 = Pair(override, externalPack?.packId),
  ) {
    value = withContext(Dispatchers.Default) {
      val ext = IconPackImporter.resolveExternalIconBitmap(context, externalPack, app, override, sizePx = 144)
      val raw = ext ?: IconPackRenderer.loadAppIconBitmap(context, app.packageName, app.activityName, sizePx = 144)
      val customPhoto = override?.customPhotoUri?.let { IconPackRenderer.loadCustomPhotoBitmap(context, it, maxPx = 192) }
      IconPackRenderer.renderStyledIconBitmap(
        sourceBitmap = raw,
        customPhotoBitmap = customPhoto,
        config = config,
        dominantAppColor = app.dominantColor,
        override = override,
        sizePx = sizePx,
      )
    }
  }
  return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconEditorSheet(
  app: LaunchableAppInfo,
  config: IconPackConfig,
  externalPack: ExternalIconPackInfo?,
  existingOverride: AppIconOverride?,
  onDismiss: () -> Unit,
  onSaveOverride: (AppIconOverride) -> Unit,
  onClearOverride: () -> Unit,
  onPinToHome: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var labelInput by remember(app.componentKey) { mutableStateOf(existingOverride?.customLabel ?: app.label) }
  var photoUri by remember(app.componentKey) { mutableStateOf(existingOverride?.customPhotoUri) }
  var tintOverride by remember(app.componentKey) { mutableStateOf(existingOverride?.customTintColor) }
  var extDrawable by remember(app.componentKey) { mutableStateOf(existingOverride?.externalDrawableName) }
  var extImgPath by remember(app.componentKey) { mutableStateOf(existingOverride?.externalImagePath) }

  val draftOverride = AppIconOverride(
    componentKey = app.componentKey,
    customLabel = labelInput.trim().takeIf { it != app.label && it.isNotEmpty() },
    customPhotoUri = photoUri,
    customTintColor = tintOverride,
    isPinnedToHome = existingOverride?.isPinnedToHome ?: false,
    externalDrawableName = extDrawable,
    externalImagePath = extImgPath,
  )

  val packEntries by produceState<List<ExternalPackEntry>>(initialValue = emptyList(), key1 = externalPack?.packId) {
    value = if (externalPack != null) {
      withContext(Dispatchers.IO) { IconPackImporter.listPackSampleEntries(context, externalPack, limit = 40) }
    } else {
      emptyList()
    }
  }

  val previewBitmap = rememberStyledAppBitmap(
    app = app,
    config = config,
    override = draftOverride,
    externalPack = externalPack,
    sizePx = 224,
  )

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      photoUri = uri.toString()
    }
  }

  val tintSwatches = listOf(
    null to "Pack Default",
    0xFF38BDF8.toInt() to "Cyan",
    0xFFF43F5E.toInt() to "Rose",
    0xFF10B981.toInt() to "Mint",
    0xFFF59E0B.toInt() to "Amber",
    0xFFA855F7.toInt() to "Violet",
    0xFFFFFFFF.toInt() to "White",
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(app.label, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close editor")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(92.dp)
              .clip(RoundedCornerShape(22.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center,
        ) {
          if (previewBitmap != null) {
            Image(
              bitmap = previewBitmap.asImageBitmap(),
              contentDescription = "Styled icon preview for ${app.label}",
              modifier = Modifier.size(84.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = labelInput,
            onValueChange = { labelInput = it.take(24) },
            singleLine = true,
            label = { Text("Home Icon Label") },
            modifier = Modifier.fillMaxWidth().testTag("icon_override_label_input"),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
              onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              modifier = Modifier.weight(1f).testTag("icon_override_pick_photo_btn"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
              Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text(if (photoUri != null) "Change Photo" else "Custom Photo", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (existingOverride != null || photoUri != null || tintOverride != null || extDrawable != null || extImgPath != null) {
              OutlinedButton(
                onClick = {
                  labelInput = app.label
                  photoUri = null
                  tintOverride = null
                  extDrawable = null
                  extImgPath = null
                  onClearOverride()
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset icon", modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      if (externalPack != null && packEntries.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "PICK ICON FROM ${externalPack.name.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packEntries) { entry ->
              val entryBmp by produceState<Bitmap?>(initialValue = null, key1 = entry) {
                value = withContext(Dispatchers.IO) {
                  IconPackImporter.loadEntryBitmap(context, externalPack, entry, sizePx = 96)
                }
              }
              val isChosen = (entry.drawableName != null && extDrawable == entry.drawableName) ||
                (entry.filePath != null && extImgPath == entry.filePath)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier =
                  Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                      width = if (isChosen) 2.dp else 1.dp,
                      color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                      shape = RoundedCornerShape(12.dp),
                    )
                    .clickable {
                      photoUri = null
                      extDrawable = entry.drawableName
                      extImgPath = entry.filePath
                    },
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (entryBmp != null) {
                    Image(bitmap = entryBmp!!.asImageBitmap(), contentDescription = entry.name, modifier = Modifier.size(38.dp))
                  }
                }
              }
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CUSTOM ACCENT OVERRIDE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(tintSwatches) { (colorInt, name) ->
            val selected = tintOverride == colorInt
            Surface(
              shape = CircleShape,
              color = colorInt?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant,
              modifier =
                Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                  )
                  .clickable { tintOverride = colorInt },
            ) {
              if (colorInt == null) {
                Box(contentAlignment = Alignment.Center) {
                  Text("Auto", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            onSaveOverride(draftOverride)
            onDismiss()
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("icon_override_save_btn"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text("Save Style")
        }
        Button(
          onClick = {
            onSaveOverride(draftOverride)
            onPinToHome()
            onDismiss()
          },
          modifier = Modifier.weight(1.3f).height(48.dp).testTag("icon_override_pin_btn"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add to Home")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchApplyIconsSheet(
  apps: List<LaunchableAppInfo>,
  config: IconPackConfig,
  externalPack: ExternalIconPackInfo?,
  overrides: Map<String, AppIconOverride>,
  onPinApp: (LaunchableAppInfo) -> Boolean,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val selectedKeys = remember {
    mutableStateListOf<String>().apply {
      addAll(apps.take(8).map { it.componentKey })
    }
  }
  var queueIndex by remember { mutableIntStateOf(0) }
  var isRunningQueue by remember { mutableStateOf(false) }

  val queuedApps = apps.filter { it.componentKey in selectedKeys }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Apply Icons to Home Screen", style = MaterialTheme.typography.titleLarge)
          Text(
            text = "Android adds themed app icons via Home shortcuts. Tap Pin to send each icon directly to your Home Screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close batch apply")
        }
      }

      if (isRunningQueue && queuedApps.isNotEmpty()) {
        val currentApp = queuedApps.getOrNull(queueIndex)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Step ${minOf(queueIndex + 1, queuedApps.size)} of ${queuedApps.size}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
              progress = { (queueIndex + 1).toFloat() / queuedApps.size.toFloat() },
              modifier = Modifier.fillMaxWidth(),
            )
            if (currentApp != null) {
              val bmp = rememberStyledAppBitmap(
                app = currentApp,
                config = config,
                override = overrides[currentApp.componentKey],
                externalPack = externalPack,
                sizePx = 160,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                if (bmp != null) {
                  Image(bitmap = bmp.asImageBitmap(), contentDescription = currentApp.label, modifier = Modifier.size(56.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(currentApp.label, style = MaterialTheme.typography.titleMedium)
                  Text("Tap below to confirm adding to Home Screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Button(
                onClick = {
                  onPinApp(currentApp)
                  if (queueIndex + 1 < queuedApps.size) {
                    queueIndex++
                  } else {
                    isRunningQueue = false
                  }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("batch_pin_next_btn"),
                shape = RoundedCornerShape(14.dp),
              ) {
                Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pin \"${currentApp.label}\" (${queueIndex + 1}/${queuedApps.size})")
              }
            }
          }
        }
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              selectedKeys.clear()
              selectedKeys.addAll(apps.take(8).map { it.componentKey })
            },
          ) {
            Text("Top 8")
          }
          OutlinedButton(
            onClick = {
              selectedKeys.clear()
              selectedKeys.addAll(apps.map { it.componentKey })
            },
          ) {
            Text("Select All (${apps.size})")
          }
          OutlinedButton(onClick = { selectedKeys.clear() }) {
            Text("Clear")
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(apps, key = { it.componentKey }) { app ->
            val checked = app.componentKey in selectedKeys
            val isPinned = overrides[app.componentKey]?.isPinnedToHome == true
            val bmp = rememberStyledAppBitmap(
              app = app,
              config = config,
              override = overrides[app.componentKey],
              externalPack = externalPack,
              sizePx = 96,
            )
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    if (checked) selectedKeys.remove(app.componentKey) else selectedKeys.add(app.componentKey)
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = { want ->
                  if (want) {
                    if (!checked) selectedKeys.add(app.componentKey)
                  } else {
                    selectedKeys.remove(app.componentKey)
                  }
                },
              )
              if (bmp != null) {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(36.dp))
              }
              Text(app.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
              if (isPinned) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        Button(
          onClick = {
            if (queuedApps.isNotEmpty()) {
              queueIndex = 0
              isRunningQueue = true
            }
          },
          enabled = queuedApps.isNotEmpty(),
          modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 4.dp).testTag("batch_start_apply_btn"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Start Adding ${queuedApps.size} Icons to Home")
        }
      }
    }
  }
}
