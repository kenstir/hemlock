package net.kenstir.hemlock.mock

import net.kenstir.hemlock.data.models.RecordMetadata
import net.kenstir.hemlock.data.Result

object MockMetadataSource {
    fun getRecordMetadata(recordId: Int): RecordMetadata {
        return RecordMetadata("Mock Title", "Mock Author", "Mock ISBN")
    }
}
