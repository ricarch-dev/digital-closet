package dev.digitalcloset.server.media

internal data class SignedUpload(val url: String, val headers: Map<String, String>)

internal data class PhotoMetadata(val contentType: String, val byteSize: Long)

internal interface PrivatePhotoStore {
    fun presignUpload(key: String, contentType: String, byteSize: Long): SignedUpload
    fun metadata(key: String): PhotoMetadata?
    fun presignRead(key: String): String
    fun delete(key: String)
}

internal class PhotoStorageUnavailable : IllegalStateException("Private photo storage is not configured")

internal object UnavailablePhotoStore : PrivatePhotoStore {
    override fun presignUpload(key: String, contentType: String, byteSize: Long): SignedUpload =
        throw PhotoStorageUnavailable()

    override fun metadata(key: String): PhotoMetadata? = throw PhotoStorageUnavailable()

    override fun presignRead(key: String): String = throw PhotoStorageUnavailable()

    override fun delete(key: String): Unit = throw PhotoStorageUnavailable()
}
