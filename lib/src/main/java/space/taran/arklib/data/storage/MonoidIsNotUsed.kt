package space.taran.arklib.data.storage

/*
 * Bogus instance of Monoid, used as a stub for
 * "generated" storages, i.e. caches. Because
 * those storages are same as normal ones,
 * they just don't have merging use-case.
 *
 * This class must not be used if `combine`
 * is called from anywhere except `Storage` class.
 *
 * Also, if merging of generated data from different
 * app versions will be needed to support, this
 * class also should not be used.
 */
internal class MonoidIsNotUsed<T> : Monoid<T> {

    override val neutral: T by lazy<T> {
        throw IllegalStateException(
            "Must not be used"
        )
    }

    override fun combine(a: T, b: T): T {
        // the implementation must be provided
        // if merging of metadata from different app versions
        // will be needed to support
        throw IllegalStateException(
            "Metadata is generated and always must be the same for same ids"
        )
        // otherwise, `a == b` unless `combine` is called
        // from anywhere except `Storage` class
    }
}