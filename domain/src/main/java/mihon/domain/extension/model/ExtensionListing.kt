package mihon.domain.extension.model

import eu.kanade.tachiyomi.extension.model.Extension

/**
 * The extensions of the stores that could be reached, along with the ones that couldn't. Nothing is known about
 * the extensions of an unreachable store, which isn't the same as it no longer listing them.
 */
data class ExtensionListing(
    val extensions: List<Extension.Available>,
    val unreachableStores: List<ExtensionStore>,
)
