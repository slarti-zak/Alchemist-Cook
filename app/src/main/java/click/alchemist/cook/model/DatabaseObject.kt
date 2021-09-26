package click.alchemist.cook.model

interface DatabaseObject {
    var id: String
    var owner: String

    var type: String
        get() = javaClass.simpleName
        set(@Suppress("UNUSED_PARAMETER") value) {}
}