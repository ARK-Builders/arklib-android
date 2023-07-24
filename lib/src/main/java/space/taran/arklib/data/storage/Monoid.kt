package space.taran.arklib.data.storage

interface Monoid<V> {
    /* Default value, e.g. empty set or 0, must not be
     * stored in a storage. All resources are implicitly
     * assumed to be mapped into neutral element.
     * E.g. all resources are scored with 0 score
     * by default, and also labeled by empty tag set. */
    val neutral: V

    /* This method is used to sync with another peers.
     * E.g. for tags this operation is union of sets,
     * for scores it's value with greater absolute value */
    fun combine(a: V, b: V): V

    fun combineAll(values: Iterable<V>): V = values
        .fold(neutral) { acc, value ->
            combine(acc, value)
        }
}