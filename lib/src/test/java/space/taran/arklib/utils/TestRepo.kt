package space.taran.arklib.utils

import dev.arkbuilders.arkfilepicker.PartialResult
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.arklib.data.meta.MetadataProcessorRepo
import dev.arkbuilders.arklib.data.preview.PreviewProcessorRepo
import dev.arkbuilders.arklib.user.properties.PropertiesStorageRepo
import dev.arkbuilders.arklib.user.score.ScoreStorageRepo
import dev.arkbuilders.arklib.user.tags.TagsStorageRepo
import io.mockk.coEvery
import io.mockk.mockk

object TestRepo {
    val folders = mockk<FoldersRepo>().apply {
        coEvery { provideWithMissing() } returns PartialResult(
            succeeded = mapOf(
                TestFiles.root1 to emptyList(),
                TestFiles.root2 to emptyList()
            ),
            failed = emptyList()
        )
        coEvery {
            resolveRoots(TestFiles.rootAndFav1())
        } returns listOf(TestFiles.root1)
        coEvery {
            resolveRoots(TestFiles.rootAndFav2())
        } returns listOf(TestFiles.root2)
        coEvery {
            resolveRoots(TestFiles.allRoots)
        } returns listOf(TestFiles.root1, TestFiles.root2)
    }

    val index = ResourceIndexRepo(folders)
    val tags = TagsStorageRepo(TestDeps.scope, TestDeps.statsFlow)
    val score = ScoreStorageRepo(TestDeps.scope)
    val properties = PropertiesStorageRepo(TestDeps.scope)
    val meta = MetadataProcessorRepo(TestDeps.scope)
    val preview = PreviewProcessorRepo(TestDeps.scope, meta)
}