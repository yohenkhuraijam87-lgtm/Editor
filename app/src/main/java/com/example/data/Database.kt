package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId")]
)
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val folderId: Int? = null,
    val contentJson: String, // Stringified list of ContentBlock
    val templateType: String = "BLANK", // BLANK, RESUME, ESSAY, LETTER, NOTES
    val isPinned: Boolean = false,
    val fontScale: Float = 1.0f, // kindle-style font scaling
    val paperColorType: String = "PAPER", // PAPER, SEPIA, DARK
    val pageSpacing: Float = 1.15f, // Kindle spacing (1.0f, 1.15f, 1.5f, 2.0f)
    val typefaceName: String = "Serif", // Serif, Sans, Monospace
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Data class representing structured blocks in our document
data class ContentBlock(
    val id: String,
    val type: String, // PARAGRAPH, HEADING_1, HEADING_2, BULLET_LIST, ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT
    val text: String
)

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getDocumentsInFolder(folderId: Int): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId IS NULL ORDER BY updatedAt DESC")
    fun getRootDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): Document?

    @Query("SELECT * FROM documents WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedDocuments(): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)
}

@Database(entities = [Folder::class, Document::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "a4_write_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
