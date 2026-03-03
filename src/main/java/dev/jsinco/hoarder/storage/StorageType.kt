package dev.jsinco.hoarder.storage

/**
 * The different types of storage that can be used
 */
enum class StorageType {
    MYSQL,
    SQLITE,
    @Deprecated("Unsupported")
    FLATFILE
}