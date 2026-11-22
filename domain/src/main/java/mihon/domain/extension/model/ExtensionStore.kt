package mihon.domain.extension.model

data class ExtensionStore(
    val indexUrl: String,
    val name: String,
    val badgeLabel: String,
    val signingKey: String,
    val contact: Contact,
    val isLegacy: Boolean,
    val extensionListUrl: String?,
) {
    data class Contact(
        val website: String,
        val discord: String?,
    )

    companion object {
        /** Stores that were carried over or restored have no key until their index is first fetched. */
        const val NO_SIGNING_KEY = "NO_SIGNING_KEY"
    }
}
