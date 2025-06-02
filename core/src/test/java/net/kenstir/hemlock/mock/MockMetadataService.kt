package net.kenstir.hemlock.mock

import net.kenstir.hemlock.data.MetadataService
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.models.RecordMetadata

class MockMetadataService: MetadataService {
    override suspend fun fetchRecordMetadata(recordId: Int): Result<RecordMetadata> {
        return Result.Success(MockMetadataSource.getRecordMetadata(recordId))
    }
}
