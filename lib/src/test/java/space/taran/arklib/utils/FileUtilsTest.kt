package space.taran.arklib.utils

import android.util.Log
import dev.arkbuilders.arklib.utils.*
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class FileUtilsTest {

    private lateinit var testDir: Path
    private lateinit var rootFolder: Path
    private lateinit var childFolder: Path
    private lateinit var rootFolderFile: Path
    private lateinit var childFolderFile: Path
    private lateinit var emptyChildFolder: Path

    @Before
    fun beforeEach() {
        initTikaSupport()
        initFileStructure()
    }

    private fun initTikaSupport() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
    }

    private fun initFileStructure() {
        testDir = Files.createTempDirectory("testFileUtils")
        rootFolder = testDir.resolve("root")
        childFolder = rootFolder.resolve("child1")
        emptyChildFolder = rootFolder.resolve("child2")
        rootFolderFile = rootFolder.resolve("file1.txt")
        childFolderFile = childFolder.resolve("file2.txt")
        Files.createDirectories(childFolder)
        Files.createDirectories(emptyChildFolder)
        Files.createFile(rootFolderFile)
        Files.createFile(childFolderFile)
    }

    @After
    fun afterEach() {
        unmockkAll()
        deleteFileStructure()
    }

    private fun deleteFileStructure() {
        Files.deleteIfExists(rootFolderFile)
        Files.deleteIfExists(childFolderFile)
        Files.deleteIfExists(emptyChildFolder)
        Files.deleteIfExists(childFolder)
        Files.deleteIfExists(rootFolder)
        Files.deleteIfExists(testDir)
    }

    private fun hideFileOrFolderOnWindows(path: Path) {
        try {
            val command = "attrib +h \"${path}\""
            val process = Runtime.getRuntime().exec(command)
            process.waitFor()
        } catch (e: IOException) {
            println("FileUtilsTest.hideFileOrFolderOnWindows: not on a Windows system")
        }
    }

    @Test
    fun testListAllFiles() = runBlocking {
        val result = listAllFiles(rootFolder)
        assertEquals(listOf(rootFolderFile, childFolderFile), result)
    }

    @Test
    fun testDeleteRecursivelyExistent() = runBlocking {
        deleteRecursively(rootFolder)
        assertTrue(Files.notExists(rootFolder))
        assertTrue(Files.notExists(rootFolderFile))
        assertTrue(Files.notExists(childFolderFile))
        assertTrue(Files.notExists(childFolder))
        assertTrue(Files.notExists(emptyChildFolder))
    }

    @Test
    fun testDeleteRecursivelyNonExistent() = runBlocking {
        val nonExistentFolder = testDir.resolve("nonExistentFolder")
        deleteRecursively(nonExistentFolder)
    }

    @Test
    fun testDeleteRecursivelyEmptyFolder() = runBlocking {
        assertEquals(emptyList<Path>() to emptyList<Path>(), listChildren(emptyChildFolder))
        deleteRecursively(emptyChildFolder)
        assertTrue(Files.notExists(emptyChildFolder))
    }

    @Test
    fun testDeleteRecursivelySingleFile() = runBlocking {
        assertTrue(Files.exists(childFolderFile))
        deleteRecursively(childFolderFile)
        assertTrue(Files.notExists(childFolderFile))
    }

    @Test
    fun testListChildren() {
        val result = listChildren(rootFolder)
        val expectedFiles = listOf(rootFolderFile)
        val expectedFolders = setOf(childFolder, emptyChildFolder)
        assertEquals(expectedFiles, result.second)
        assertEquals(expectedFolders, result.first.toSet())
    }

    @Test
    fun testListChildrenEmptyFolder() {
        val result = listChildren(emptyChildFolder)
        assertEquals(emptyList<Path>() to emptyList<Path>(), result)
    }

    @Test
    fun testListChildrenHiddenFilesAndFolders() {
        val hiddenFile = rootFolder.resolve(".hidden_file.txt")
        val hiddenFolder = rootFolder.resolve(".hidden_folder")
        Files.createFile(hiddenFile)
        Files.createDirectories(hiddenFolder)
        hideFileOrFolderOnWindows(hiddenFile)
        hideFileOrFolderOnWindows(hiddenFolder)
        val result = listChildren(rootFolder)
        val expectedFiles = listOf(rootFolderFile)
        val expectedFolders = setOf(childFolder, emptyChildFolder)
        assertEquals(expectedFiles, result.second)
        assertEquals(expectedFolders, result.first.toSet())
        Files.deleteIfExists(hiddenFile)
        Files.deleteIfExists(hiddenFolder)
    }

    @Test
    fun testExtensionWithNoExtension() {
        val path = Path("fileWithoutExtension")
        val result = extension(path)
        assertEquals("", result)
    }

    @Test
    fun testExtensionWithUpperCaseExtension() {
        val path = Path("fileWithUpperCase.JPG")
        val result = extension(path)
        assertEquals("jpg", result)
    }

    @Test
    fun testExtensionWithLowerCaseExtension() {
        val path = Path("fileWithLowerCase.txt")
        val result = extension(path)
        assertEquals("txt", result)
    }

    @Test
    fun testExtensionWithMixedCaseExtension() {
        val path = Path("fileWithMixedCase.PnG")
        val result = extension(path)
        assertEquals("png", result)
    }

    @Test
    fun testExtensionWithMultipleDots() {
        val path = Path("file.with.multiple.dots.txt")
        val result = extension(path)
        assertEquals("txt", result)
    }

    @Test
    fun testDetectMimeTypeSuccess() {
        val result = detectMimeType(rootFolderFile)
        assertEquals("application/octet-stream", result)
    }

    @Test
    fun testDetectMimeTypeFailure() {
        val path = Path("nonexistent.file")
        val result = detectMimeType(path)
        assertNull(result)
    }

}
