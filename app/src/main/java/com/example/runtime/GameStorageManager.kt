package com.example.runtime

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.CustomGameProject
import com.example.model.GameAssetFile
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class GameStorageManager(private val context: Context) {

    private val gamesRootDir: File
        get() = File(context.filesDir, "games").apply { if (!exists()) mkdirs() }

    private val modelsRootDir: File
        get() = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    /**
     * Imports user-selected HTML, CSS, JS, ZIP, or multi-file game project preserving all original files.
     */
    fun importGameFromUris(uris: List<Uri>): Result<CustomGameProject> = runCatching {
        require(uris.isNotEmpty()) { "No files selected" }
        val gameId = "game_${UUID.randomUUID().toString().take(8)}"
        val projectDir = File(gamesRootDir, gameId).apply { mkdirs() }

        var primaryTitle = "Custom Motion Game"
        var sourceType = "HTML/JS"

        for (uri in uris) {
            val fileName = resolveFileName(uri)
            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (ext == "zip") {
                sourceType = "ZIP Archive"
                primaryTitle = fileName.substringBeforeLast('.')
                extractZipSafely(uri, projectDir)
            } else if (ext in ALLOWED_GAME_EXTENSIONS) {
                if (primaryTitle == "Custom Motion Game") {
                    primaryTitle = fileName.substringBeforeLast('.')
                }
                val targetFile = File(projectDir, sanitizeFileName(fileName))
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                } ?: error("Unable to read $fileName")
            } else {
                error("Unsupported game file format: .$ext")
            }
        }

        val manifest = scanProjectFiles(projectDir)
        require(manifest.isNotEmpty()) { "No valid game files found in upload" }

        val entryPoint = detectEntryPoint(manifest)
            ?: error("Could not locate index.html or an HTML entry point in uploaded project")

        CustomGameProject(
            id = gameId,
            title = primaryTitle.replace('_', ' ').replace('-', ' ').trim()
                .ifBlank { "Uploaded Game" },
            entryPoint = entryPoint,
            rootDirectory = projectDir.absolutePath,
            files = manifest,
            sourceType = sourceType,
            supportsMotionSdk = true,
            isBuiltInTemplate = false
        )
    }

    /**
     * Installs a real editable HTML5 + MotionX SDK sample game into filesDir so users can also test immediately.
     */
    fun installCyberArenaTemplate(): CustomGameProject {
        val gameId = "motionx_cyber_arena"
        val projectDir = File(gamesRootDir, gameId).apply { mkdirs() }
        val indexFile = File(projectDir, "index.html")
        indexFile.writeText(MotionXSdkTemplates.CYBER_ARENA_INDEX_HTML)
        val manifest = scanProjectFiles(projectDir)
        return CustomGameProject(
            id = gameId,
            title = "Cyber Motion Arena (SDK Demo)",
            entryPoint = "index.html",
            rootDirectory = projectDir.absolutePath,
            files = manifest,
            sourceType = "MotionX SDK HTML5",
            supportsMotionSdk = true,
            isBuiltInTemplate = true
        )
    }

    fun import3DModelUri(uri: Uri): Result<Pair<String, String>> = runCatching {
        val fileName = sanitizeFileName(resolveFileName(uri))
        val ext = fileName.substringAfterLast('.', "").lowercase()
        require(ext == "glb" || ext == "gltf" || ext == "obj") {
            "Only .glb, .gltf, or .obj 3D assets are supported"
        }
        val destFile = File(modelsRootDir, "${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: error("Failed to read 3D asset")
        fileName.substringBeforeLast('.') to destFile.absolutePath
    }

    fun resolveGameFile(gameId: String, relativePath: String): File? {
        val gameDir = File(gamesRootDir, sanitizeFileName(gameId))
        if (!gameDir.exists()) return null
        val cleanRel = relativePath.trimStart('/').ifBlank { "index.html" }
        val candidate = File(gameDir, cleanRel)
        val canonicalBase = gameDir.canonicalPath
        val canonicalTarget = candidate.canonicalPath
        if (!canonicalTarget.startsWith(canonicalBase)) return null
        return if (candidate.exists() && candidate.isFile) candidate else null
    }

    fun deleteGameFiles(gameId: String) {
        File(gamesRootDir, sanitizeFileName(gameId)).deleteRecursively()
    }

    private fun extractZipSafely(uri: Uri, targetDir: File) {
        val canonicalBase = targetDir.canonicalPath + File.separator
        var totalBytes = 0L
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(targetDir, entry.name)
                        val canonicalOut = outFile.canonicalPath
                        if (!canonicalOut.startsWith(canonicalBase)) {
                            throw SecurityException("Blocked unsafe zip entry: ${entry.name}")
                        }
                        val ext = outFile.extension.lowercase()
                        if (ext in ALLOWED_GAME_EXTENSIONS) {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out ->
                                val buf = ByteArray(8192)
                                var read: Int
                                while (zip.read(buf).also { read = it } != -1) {
                                    totalBytes += read
                                    if (totalBytes > MAX_EXTRACT_BYTES) {
                                        throw IllegalStateException("Game archive exceeds 80 MB safety limit")
                                    }
                                    out.write(buf, 0, read)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Cannot open ZIP archive")
    }

    private fun scanProjectFiles(projectDir: File): List<GameAssetFile> {
        val baseLen = projectDir.absolutePath.length + 1
        return projectDir.walkTopDown()
            .filter { it.isFile }
            .map { file ->
                val rel = file.absolutePath.drop(baseLen).replace('\\', '/')
                GameAssetFile(
                    relativePath = rel,
                    extension = file.extension.lowercase(),
                    sizeBytes = file.length()
                )
            }
            .sortedBy { it.relativePath }
            .toList()
    }

    private fun detectEntryPoint(files: List<GameAssetFile>): String? {
        val exactRootIndex = files.firstOrNull { it.relativePath.equals("index.html", ignoreCase = true) }
        if (exactRootIndex != null) return exactRootIndex.relativePath
        val nestedIndex = files.firstOrNull { it.relativePath.endsWith("/index.html", ignoreCase = true) }
        if (nestedIndex != null) return nestedIndex.relativePath
        return files.firstOrNull { it.extension == "html" || it.extension == "htm" }?.relativePath
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "game_${System.currentTimeMillis()}.html"
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    companion object {
        private const val MAX_EXTRACT_BYTES = 80L * 1024L * 1024L
        private val ALLOWED_GAME_EXTENSIONS = setOf(
            "html", "htm", "css", "js", "mjs", "json", "txt", "md",
            "glb", "gltf", "bin", "obj", "mtl",
            "png", "jpg", "jpeg", "webp", "svg", "gif",
            "mp3", "wav", "ogg", "woff", "woff2"
        )
    }
}
