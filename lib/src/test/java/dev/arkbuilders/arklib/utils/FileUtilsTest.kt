package dev.arkbuilders.arklib.utils

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

class FileUtilsTest {

    private lateinit var testDir: Path
    private lateinit var favFolder: Path
    private lateinit var rootFolder: Path
    private lateinit var favFolderFile: Path
    private lateinit var rootFolderFile: Path
    private lateinit var emptyFavFolder: Path

    @Before
    fun beforeEach() {
        mockLog()
        initFileStructure()
    }

    private fun mockLog() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
    }

    private fun initFileStructure() {
        testDir = Files.createTempDirectory("testFileUtils")
        rootFolder = testDir.resolve("root")
        favFolder = rootFolder.resolve("child1")
        emptyFavFolder = rootFolder.resolve("child2")
        rootFolderFile = rootFolder.resolve("file1.txt")
        favFolderFile = favFolder.resolve("file2.txt")
        Files.createDirectories(favFolder)
        Files.createDirectories(emptyFavFolder)
        Files.createFile(rootFolderFile)
        Files.createFile(favFolderFile)
    }

    @After
    fun afterEach() {
        unmockkAll()
        deleteFileStructure()
    }

    private fun deleteFileStructure() {
        rootFolderFile.deleteIfExists()
        favFolderFile.deleteIfExists()
        emptyFavFolder.deleteIfExists()
        favFolder.deleteIfExists()
        rootFolder.deleteIfExists()
        testDir.deleteIfExists()
    }

    @Test
    fun testListAllFiles() = runBlocking {
        val result = listAllFiles(rootFolder)
        assertEquals(listOf(rootFolderFile, favFolderFile), result)
    }

    @Test
    fun testDeleteRecursivelyExistent() = runBlocking {
        deleteRecursively(rootFolder)
        assertTrue(Files.notExists(rootFolder))
        assertTrue(Files.notExists(rootFolderFile))
        assertTrue(Files.notExists(favFolderFile))
        assertTrue(Files.notExists(favFolder))
        assertTrue(Files.notExists(emptyFavFolder))
    }

    @Test
    fun testDeleteRecursivelyNonExistent() = runBlocking {
        val nonExistentFolder = testDir.resolve("nonExistentFolder")
        deleteRecursively(nonExistentFolder)
    }

    @Test
    fun testDeleteRecursivelyEmptyFolder() = runBlocking {
        assertEquals(emptyList<Path>() to emptyList<Path>(), listChildren(emptyFavFolder))
        deleteRecursively(emptyFavFolder)
        assertTrue(Files.notExists(emptyFavFolder))
    }

    @Test
    fun testDeleteRecursivelySingleFile() = runBlocking {
        assertTrue(Files.exists(favFolderFile))
        deleteRecursively(favFolderFile)
        assertTrue(Files.notExists(favFolderFile))
    }

    @Test
    fun testListChildren() {
        val result = listChildren(rootFolder)
        val expectedFiles = listOf(rootFolderFile)
        val expectedFolders = setOf(favFolder, emptyFavFolder)
        assertEquals(expectedFiles, result.second)
        assertEquals(expectedFolders, result.first.toSet())
    }

    @Test
    fun testListChildrenEmptyFolder() {
        val result = listChildren(emptyFavFolder)
        assertEquals(emptyList<Path>() to emptyList<Path>(), result)
    }

    @Test
    fun testListChildrenHiddenFilesAndFolders() {
        val hiddenFile = rootFolder.resolve(".hidden_file.txt")
        val hiddenFolder = rootFolder.resolve(".hidden_folder")
        Files.createFile(hiddenFile)
        Files.createDirectories(hiddenFolder)
        val result = listChildren(rootFolder)
        val expectedFiles = listOf(rootFolderFile)
        val expectedFolders = setOf(favFolder, emptyFavFolder)
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
