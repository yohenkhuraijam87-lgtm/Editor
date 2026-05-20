package com.example.data

import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val folderDao: FolderDao,
    private val documentDao: DocumentDao
) {
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()
    val pinnedDocuments: Flow<List<Document>> = documentDao.getPinnedDocuments()

    fun getDocumentsInFolder(folderId: Int): Flow<List<Document>> {
        return documentDao.getDocumentsInFolder(folderId)
    }

    fun getRootDocuments(): Flow<List<Document>> {
        return documentDao.getRootDocuments()
    }

    suspend fun getDocumentById(id: Int): Document? {
        return documentDao.getDocumentById(id)
    }

    suspend fun insertFolder(folder: Folder): Long {
        return folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        folderDao.deleteFolder(folder)
    }

    suspend fun insertDocument(document: Document): Long {
        return documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: Document) {
        documentDao.updateDocument(document)
    }

    suspend fun deleteDocumentById(id: Int) {
        documentDao.deleteDocumentById(id)
    }
}
