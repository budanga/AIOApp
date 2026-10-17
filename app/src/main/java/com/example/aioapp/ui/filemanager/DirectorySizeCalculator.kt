package com.example.aioapp.ui.filemanager

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

/**
 * Computes recursive directory sizes, cooperatively cancellable.
 *
 * The measured directory and its descendants up to [cacheDepth] levels below it are cached
 * by absolute path, so opening a subfolder right after its parent was measured is instant.
 * Deeper levels are not cached to keep memory bounded on huge trees (e.g. Android/data).
 */
class DirectorySizeCalculator(private val cacheDepth: Int = 2) {

    private val cache = ConcurrentHashMap<String, Long>()

    fun cached(path: String): Long? = cache[path]

    suspend fun sizeOf(dir: File): Long = sizeOf(dir, depth = 0)

    private suspend fun sizeOf(dir: File, depth: Int): Long {
        currentCoroutineContext().ensureActive()
        cache[dir.absolutePath]?.let { return it }

        var total = 0L
        val children = try { dir.listFiles() } catch (e: SecurityException) { null } ?: emptyArray()
        for (child in children) {
            val attrs = try {
                Files.readAttributes(child.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            } catch (e: Exception) {
                continue
            }
            when {
                attrs.isSymbolicLink -> Unit // Not followed: avoids loops and counting the target twice
                attrs.isDirectory -> total += sizeOf(child, depth + 1)
                else -> total += attrs.size()
            }
        }
        // Only reached when the whole subtree was measured, so a cancelled scan never caches a partial size
        if (depth <= cacheDepth) cache[dir.absolutePath] = total
        return total
    }

    /** Drops sizes affected by a change inside [path]: the folder itself, its descendants and its ancestors. */
    fun invalidate(path: String) {
        val normalized = path.trimEnd(File.separatorChar)
        cache.keys.removeIf { key ->
            val normalizedKey = key.trimEnd(File.separatorChar)
            normalizedKey == normalized ||
                normalizedKey.startsWith(normalized + File.separatorChar) ||
                normalized.startsWith(normalizedKey + File.separatorChar)
        }
    }
}
