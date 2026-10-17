package com.example.aioapp.filemanager

import com.example.aioapp.ui.filemanager.DirectorySizeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DirectorySizeCalculatorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.write(name: String, bytes: Int): File =
        File(this, name).apply { parentFile?.mkdirs(); writeBytes(ByteArray(bytes)) }

    /** root/a(10) + root/sub/b(20) + root/sub/deep/c(30) + root/sub/deep/deeper/d(40) */
    private fun sampleTree(): File {
        val root = tmp.newFolder("root")
        root.write("a", 10)
        root.write("sub/b", 20)
        root.write("sub/deep/c", 30)
        root.write("sub/deep/deeper/d", 40)
        return root
    }

    @Test
    fun `sums all files recursively`() = runBlocking {
        val root = sampleTree()
        assertEquals(100L, DirectorySizeCalculator().sizeOf(root))
    }

    @Test
    fun `caches subfolders up to the configured depth`() = runBlocking {
        val root = sampleTree()
        val calculator = DirectorySizeCalculator(cacheDepth = 2)

        calculator.sizeOf(root)

        assertEquals(100L, calculator.cached(root.absolutePath))
        assertEquals(90L, calculator.cached(File(root, "sub").absolutePath))
        assertEquals(70L, calculator.cached(File(root, "sub/deep").absolutePath))
        assertNull(calculator.cached(File(root, "sub/deep/deeper").absolutePath))
    }

    @Test
    fun `cached size is reused instead of rescanning`() = runBlocking {
        val root = sampleTree()
        val calculator = DirectorySizeCalculator()
        calculator.sizeOf(root)

        File(root, "sub/new").writeBytes(ByteArray(5))

        assertEquals(100L, calculator.sizeOf(root))
    }

    @Test
    fun `invalidate drops the folder, its descendants and its ancestors`() = runBlocking {
        val root = sampleTree()
        val other = tmp.newFolder("other").apply { write("x", 7) }
        val calculator = DirectorySizeCalculator()
        calculator.sizeOf(root)
        calculator.sizeOf(other)

        calculator.invalidate(File(root, "sub").absolutePath)

        assertNull(calculator.cached(root.absolutePath))
        assertNull(calculator.cached(File(root, "sub").absolutePath))
        assertNull(calculator.cached(File(root, "sub/deep").absolutePath))
        assertEquals(7L, calculator.cached(other.absolutePath))
    }

    @Test
    fun `invalidate does not touch folders sharing a name prefix`() = runBlocking {
        val photos = tmp.newFolder("photos").apply { write("p", 1) }
        val photosOld = tmp.newFolder("photos_old").apply { write("q", 2) }
        val calculator = DirectorySizeCalculator()
        calculator.sizeOf(photos)
        calculator.sizeOf(photosOld)

        calculator.invalidate(photos.absolutePath)

        assertEquals(2L, calculator.cached(photosOld.absolutePath))
    }

    @Test
    fun `cancelled scan throws and caches nothing`() = runBlocking {
        val root = sampleTree()
        val calculator = DirectorySizeCalculator()

        try {
            coroutineScope {
                cancel()
                calculator.sizeOf(root)
            }
            fail("Expected cancellation")
        } catch (e: CancellationException) {
            // expected
        }

        assertNull(calculator.cached(root.absolutePath))
    }
}
