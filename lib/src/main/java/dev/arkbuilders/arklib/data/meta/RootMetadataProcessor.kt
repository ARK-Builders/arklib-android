package dev.arkbuilders.arklib.data.meta

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.arkFolder
import dev.arkbuilders.arklib.arkMetadata
import dev.arkbuilders.arklib.data.index.NewResource
import dev.arkbuilders.arklib.data.index.RootIndex
import dev.arkbuilders.arklib.data.processor.RootProcessor
import java.nio.file.Path

class MetadataUpdate(
    val added: List<AddedMetadata>,
    val deleted: List<DeletedMetadata>
)

data class AddedMetadata(val id: ResourceId, val path: Path, val metadata: Metadata)
data class DeletedMetadata(val id: ResourceId)

class RootMetadataProcessor private constructor(
    private val scope: CoroutineScope,
    private val index: RootIndex,
) : RootProcessor<Metadata, MetadataUpdate>() {
    val root = index.path

    private val storage = MetadataStorage(scope, root.arkFolder().arkMetadata())

    override suspend fun init() {
        storage.init()
        initKnownResources()
        initUpdatedResourcesListener()
    }

    // used for RootPreviewProcessor initialization
    internal fun state(): Map<ResourceId, Metadata> = storage.valueById

    override fun retrieve(id: ResourceId): Result<Metadata> {
        val metadata = storage.valueById[id]
            ?: return Result.failure(NoSuchElementException())

        return Result.success(metadata)
    }

    override fun forget(id: ResourceId) {
        storage.remove(id)

        scope.launch {
            _updates.emit(
                MetadataUpdate(
                    added = emptyList(),
                    deleted = listOf(DeletedMetadata(id))
                )
            )
        }
    }

    private suspend fun generate(resources: Collection<NewResource>): List<AddedMetadata> =
        withContext(Dispatchers.Default) {
            val amount = resources.size
            Log.i(
                LOG_PREFIX,
                "Checking metadata for $amount known resources in $root"
            )

            _busy.emit(true)

            val jobs = resources.map { added ->
                async {
                    generate(added)
                }
            }

            val added = jobs.awaitAll().filterNotNull()
            _busy.emit(false)

            // UI should become interactive before persisting
            scope.launch { storage.persist() }
            return@withContext added
        }

    private suspend fun generate(newResource: NewResource): AddedMetadata? =
        withContext(Dispatchers.Default) {
            val resource = newResource.resource
            val path = newResource.path

            var metadata = storage.valueById[resource.id]

            metadata?.let {
                return@withContext null
            }

            Log.v(
                LOG_PREFIX,
                "generating metadata for resource ${resource.id} by path $path"
            )

            metadata = MetadataGenerator
                .generate(path, resource)
                .getOrNull()

            metadata?.let {
                storage.setValue(resource.id, it)

                return@withContext AddedMetadata(
                    newResource.resource.id,
                    newResource.path,
                    metadata
                )
            }

            return@withContext null
        }


    private fun initUpdatedResourcesListener() {
        Log.i(LOG_PREFIX, "Listening for updates in the index")
        index.updates.onEach { diff ->
            val addedMetadata = generate(diff.added.values)

            diff.deleted.forEach { (id, _) ->
                storage.remove(id)
            }

            _updates.emit(
                MetadataUpdate(
                    addedMetadata,
                    diff.deleted.map { (id, _) -> DeletedMetadata(id) }
                )
            )
        }.launchIn(scope + Dispatchers.Default)
    }

    private suspend fun initKnownResources() = generate(index.asAdded())

    companion object {
        suspend fun provide(
            scope: CoroutineScope,
            index: RootIndex,
        ) = RootMetadataProcessor(scope, index).also {
            it.init()
        }
    }
}
