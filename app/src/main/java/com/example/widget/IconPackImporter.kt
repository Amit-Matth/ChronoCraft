package com.example.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.model.AppIconOverride
import com.example.model.ExternalIconPackInfo
import com.example.model.IconPackSourceType
import com.example.model.LaunchableAppInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ExternalPackEntry(
  val name: String,
  val drawableName: String? = null,
  val filePath: String? = null,
)

object IconPackImporter {

  private val ICON_PACK_ACTIONS = listOf(
    "com.novalauncher.THEME",
    "org.adw.launcher.THEMES",
    "com.anddoes.launcher.THEME",
    "com.gau.go.launcherex.theme",
  )

  private val appFilterCache = ConcurrentHashMap<String, Map<String, String>>()
  private val zipFilesCache = ConcurrentHashMap<String, Map<String, String>>()

  fun discoverInstalledIconPacks(context: Context): List<ExternalIconPackInfo> {
    val pm = context.packageManager
    val found = LinkedHashMap<String, ExternalIconPackInfo>()
    for (action in ICON_PACK_ACTIONS) {
      val intent = Intent(action)
      val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
      }
      for (ri in list) {
        val pkg = ri.activityInfo?.packageName ?: continue
        if (pkg == context.packageName || found.containsKey(pkg)) continue
        val label = ri.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
        val filterMap = loadApkAppFilter(context, pkg)
        found[pkg] = ExternalIconPackInfo(
          packId = "apk:$pkg",
          name = label,
          sourceType = IconPackSourceType.INSTALLED_APK,
          packageName = pkg,
          iconCount = filterMap.size,
        )
      }
    }
    return found.values.toList()
  }

  private fun loadApkAppFilter(context: Context, packageName: String): Map<String, String> {
    appFilterCache[packageName]?.let { return it }
    val map = HashMap<String, String>()
    runCatching {
      val res = context.packageManager.getResourcesForApplication(packageName)
      val xmlId = res.getIdentifier("appfilter", "xml", packageName)
      if (xmlId != 0) {
        val parser = res.getXml(xmlId)
        parseAppFilter(parser, map)
      } else {
        val rawId = res.getIdentifier("appfilter", "raw", packageName)
        val stream = if (rawId != 0) {
          res.openRawResource(rawId)
        } else {
          res.assets.open("appfilter.xml")
        }
        stream.use { s ->
          val factory = XmlPullParserFactory.newInstance()
          val parser = factory.newPullParser()
          parser.setInput(s, "UTF-8")
          parseAppFilter(parser, map)
        }
      }
    }
    appFilterCache[packageName] = map
    return map
  }

  private fun parseAppFilter(parser: XmlPullParser, out: MutableMap<String, String>) {
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      if (event == XmlPullParser.START_TAG && parser.name == "item") {
        val comp = parser.getAttributeValue(null, "component")
        val drawable = parser.getAttributeValue(null, "drawable")
        if (!comp.isNullOrBlank() && !drawable.isNullOrBlank()) {
          val cleaned = comp.removePrefix("ComponentInfo{").removeSuffix("}")
          out[cleaned] = drawable
          val pkgOnly = cleaned.substringBefore('/')
          if (pkgOnly.isNotBlank() && !out.containsKey("pkg:$pkgOnly")) {
            out["pkg:$pkgOnly"] = drawable
          }
        }
      }
      event = parser.next()
    }
  }

  fun importZipIconPack(context: Context, uri: Uri): Result<ExternalIconPackInfo> {
    return runCatching {
      val displayName = resolveFileDisplayName(context, uri)
        ?.removeSuffix(".zip")
        ?.take(32)
        ?.ifBlank { "Custom Icon Pack" }
        ?: "Custom Icon Pack"
      val packId = "zip_${System.currentTimeMillis()}"
      val packDir = File(context.filesDir, "imported_packs/$packId").apply { mkdirs() }
      val canonicalRoot = packDir.canonicalPath
      var count = 0

      context.contentResolver.openInputStream(uri)?.use { rawIn ->
        ZipInputStream(rawIn).use { zis ->
          var entry = zis.nextEntry
          while (entry != null && count < 1500) {
            if (!entry.isDirectory) {
              val lower = entry.name.lowercase()
              if (lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".jpg")) {
                val baseName = File(entry.name).name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                if (baseName.isNotBlank()) {
                  val targetFile = File(packDir, baseName)
                  if (targetFile.canonicalPath.startsWith(canonicalRoot + File.separator)) {
                    targetFile.outputStream().use { out ->
                      zis.copyTo(out)
                    }
                    count++
                  }
                }
              }
            }
            zis.closeEntry()
            entry = zis.nextEntry
          }
        }
      } ?: error("Unable to open ZIP file")

      check(count > 0) { "No PNG/WEBP icon files found inside ZIP" }
      ExternalIconPackInfo(
        packId = packId,
        name = displayName,
        sourceType = IconPackSourceType.IMPORTED_ZIP,
        folderPath = packDir.absolutePath,
        iconCount = count,
      )
    }
  }

  private fun resolveFileDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
      }
    }.getOrNull()
  }

  private fun indexZipFolder(folderPath: String): Map<String, String> {
    zipFilesCache[folderPath]?.let { return it }
    val dir = File(folderPath)
    val map = LinkedHashMap<String, String>()
    dir.listFiles()?.forEach { file ->
      if (file.isFile) {
        val key = file.nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (key.isNotBlank()) {
          map[key] = file.absolutePath
        }
      }
    }
    zipFilesCache[folderPath] = map
    return map
  }

  fun listPackSampleEntries(
    context: Context,
    pack: ExternalIconPackInfo,
    limit: Int = 90,
  ): List<ExternalPackEntry> {
    return when (pack.sourceType) {
      IconPackSourceType.IMPORTED_ZIP -> {
        val dir = pack.folderPath?.let { File(it) } ?: return emptyList()
        dir.listFiles()
          ?.filter { it.isFile }
          ?.sortedBy { it.name.lowercase() }
          ?.take(limit)
          ?.map { ExternalPackEntry(name = it.nameWithoutExtension, filePath = it.absolutePath) }
          .orEmpty()
      }
      IconPackSourceType.INSTALLED_APK -> {
        val pkg = pack.packageName ?: return emptyList()
        val filter = loadApkAppFilter(context, pkg)
        filter.values.distinct().sorted().take(limit).map { drName ->
          ExternalPackEntry(name = drName.replace('_', ' '), drawableName = drName)
        }
      }
    }
  }

  fun loadEntryBitmap(
    context: Context,
    pack: ExternalIconPackInfo,
    entry: ExternalPackEntry,
    sizePx: Int = 128,
  ): Bitmap? {
    entry.filePath?.let { path ->
      return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }
    val pkg = pack.packageName ?: return null
    val drName = entry.drawableName ?: return null
    return loadApkDrawableBitmap(context, pkg, drName, sizePx)
  }

  fun resolveExternalIconBitmap(
    context: Context,
    pack: ExternalIconPackInfo?,
    app: LaunchableAppInfo,
    override: AppIconOverride?,
    sizePx: Int = 192,
  ): Bitmap? {
    override?.externalImagePath?.let { path ->
      runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.let { return it }
    }
    if (pack == null) return null

    override?.externalDrawableName?.let { dr ->
      if (pack.packageName != null) {
        loadApkDrawableBitmap(context, pack.packageName, dr, sizePx)?.let { return it }
      }
    }

    return when (pack.sourceType) {
      IconPackSourceType.INSTALLED_APK -> {
        val pkg = pack.packageName ?: return null
        val filter = loadApkAppFilter(context, pkg)
        val drawableName = filter[app.componentKey]
          ?: filter["pkg:${app.packageName}"]
          ?: sanitizeName(app.label).takeIf { it.isNotBlank() }
        drawableName?.let { loadApkDrawableBitmap(context, pkg, it, sizePx) }
      }
      IconPackSourceType.IMPORTED_ZIP -> {
        val folder = pack.folderPath ?: return null
        val index = indexZipFolder(folder)
        val lblKey = sanitizeName(app.label)
        val pkgTail = sanitizeName(app.packageName.substringAfterLast('.'))
        val pkgFull = sanitizeName(app.packageName)
        val matchedPath = index[lblKey]
          ?: index[pkgTail]
          ?: index[pkgFull]
          ?: index.entries.firstOrNull { (k, _) ->
            (lblKey.length >= 4 && k.contains(lblKey)) || (pkgTail.length >= 4 && k.contains(pkgTail))
          }?.value
        matchedPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
      }
    }
  }

  private fun sanitizeName(raw: String): String =
    raw.lowercase().replace(Regex("[^a-z0-9]"), "")

  private fun loadApkDrawableBitmap(
    context: Context,
    packageName: String,
    drawableName: String,
    sizePx: Int,
  ): Bitmap? {
    return runCatching {
      val res: Resources = context.packageManager.getResourcesForApplication(packageName)
      val id = res.getIdentifier(drawableName, "drawable", packageName)
      if (id == 0) return null
      @Suppress("DEPRECATION")
      val drawable = res.getDrawable(id, null) ?: return null
      if (drawable is BitmapDrawable && drawable.bitmap != null) {
        Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
      } else {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        bmp
      }
    }.getOrNull()
  }
}
