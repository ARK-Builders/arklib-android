package space.taran.arklib.domain.meta

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import space.taran.arklib.ResourceId
import space.taran.arklib.arkFolder
import space.taran.arklib.arkMetadata
import space.taran.arklib.domain.index.NewResource
import space.taran.arklib.domain.index.RootIndex
import space.taran.arklib.domain.processor.Processor
import space.taran.arklib.domain.processor.RootProcessor
import java.nio.file.Path

sealed class MetadataUpdate {
    data class Deleted(val id: ResourceId): MetadataUpdate()
    data class Added(val id: ResourceId, val path: Path, val metadata: Metadata): MetadataUpdate()
}

class RootMetadataProcessor(
    private val scope: CoroutineScope,
    private val index: RootIndex,
): RootProcessor<Metadata, MetadataUpdate>() {
    val root = index.path

    private val storage = MetadataStorage(scope, root.arkFolder().arkMetadata())

    init {
        Log.i(LOG_PREFIX, "Initializing metadata storage for root $root")

        storage.refresh()

        initUpdatedResourcesListener()
        initKnownResources()
    }

    private suspend fun emit(id: ResourceId, path: Path, metadata: Metadata) =
        _updates.emit(MetadataUpdate.Added(id, path, metadata))

    private fun generate(resources: Collection<NewResource>) {
        val amount = resources.size
        Log.i(LOG_PREFIX, "Checking metadata for $amount known resources in $root")

        scope.launch(Dispatchers.IO) {
            _busy.emit(true)

            val jobs = resources.map { added ->
                launch {
                    val resource = added.resource
                    val path = added.path

                    val result = storage.valueById[resource.id]
                    if (result != null) {

                        // we still emit the metadata even if it already exists
                        // initialization of previews pipeline relies on it
                        emit(resource.id, path, result)
                    } else {
                        Log.d(
                            LOG_PREFIX,
                            "generating metadata for resource ${resource.id} by path $path"
                        )

                        MetadataGenerator
                            .generate(path, resource)
                            .onSuccess {
                                storage.setValue(resource.id, it)
                                emit(resource.id, path, it)
                            }
                    }
                }
            }

            jobs.joinAll()
            _busy.emit(false)

            // UI should become interactive before persisting
            storage.persist()
        }
    }

    private fun initUpdatedResourcesListener() {
        Log.i(LOG_PREFIX, "Listening for updates in the index")
        index.updates.onEach { diff ->
            generate(diff.added.values)

            diff.deleted.forEach { (id, _) ->
                forget(id)
            }
        }.launchIn(scope + Dispatchers.Default)
    }

    // this forces to check existing metadata,
    // existing metadata will be pushed into `updates` too,
    // this is needed for checking previews
    private fun initKnownResources() {
        scope.launch(Dispatchers.Default) {
            generate(index.asAdded())
        }
    }

    override fun retrieve(id: ResourceId): Result<Metadata> {
        val metadata = storage.valueById[id]
            ?: return Result.failure(NoSuchElementException())

        return Result.success(metadata)
    }

    override fun forget(id: ResourceId) {
        storage.remove(id)

        scope.launch {
            _updates.emit(MetadataUpdate.Deleted(id))
        }
    }
}