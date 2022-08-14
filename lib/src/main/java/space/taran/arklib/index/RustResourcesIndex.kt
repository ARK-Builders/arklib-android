package space.taran.arklib.index

class RustResourcesIndex {
external fun listResources(prefix: String): ArrayList<ResourceId>
external fun getPath(id: ResourceId): String
// TODO
external fun getMeta(id: ResourceId)
external fun reindex()
external fun remove(id: ResourceId)
external fun updateResource()
}