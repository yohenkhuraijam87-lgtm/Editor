package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DocumentRepository(database.folderDao(), database.documentDao())

    // UI state streams from Database
    val folders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<Document>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedDocuments: StateFlow<List<Document>> = repository.pinnedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Navigation State
    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId: StateFlow<Int?> = _selectedFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Screen state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Document Editing States
    private val _activeDocument = MutableStateFlow<Document?>(null)
    val activeDocument: StateFlow<Document?> = _activeDocument.asStateFlow()

    private val _editingBlocks = MutableStateFlow<List<ContentBlock>>(emptyList())
    val editingBlocks: StateFlow<List<ContentBlock>> = _editingBlocks.asStateFlow()

    // Styling configuration state (loaded from Document or defaulted)
    val paperColorType = _activeDocument.map { it?.paperColorType ?: "PAPER" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PAPER")

    val typefaceName = _activeDocument.map { it?.typefaceName ?: "Serif" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Serif")

    val fontScale = _activeDocument.map { it?.fontScale ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val pageSpacing = _activeDocument.map { it?.pageSpacing ?: 1.15f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.15f)

    private val _distractionFree = MutableStateFlow(false)
    val distractionFree: StateFlow<Boolean> = _distractionFree.asStateFlow()

    // AI Check state
    private val _aiResult = MutableStateFlow<AiResultState>(AiResultState.Idle)
    val aiResult: StateFlow<AiResultState> = _aiResult.asStateFlow()

    private var autoSaveJob: Job? = null

    // Navigation and folder grouping helpers
    fun selectFolder(folderId: Int?) {
        _selectedFolderId.value = folderId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.Home) {
            _activeDocument.value = null
            _editingBlocks.value = emptyList()
            _aiResult.value = AiResultState.Idle
            _distractionFree.value = false
        }
    }

    fun toggleDistractionFree() {
        _distractionFree.value = !_distractionFree.value
    }

    fun updateDistractionFree(value: Boolean) {
        _distractionFree.value = value
    }

    // CREATE FOLDER
    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertFolder(Folder(name = name, colorHex = colorHex))
        }
    }

    // DELETE FOLDER
    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            // If the folder deleted is selected, fallback to Root
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    // RENAME FOLDER
    fun renameFolder(folder: Folder, newName: String) {
        viewModelScope.launch {
            repository.updateFolder(folder.copy(name = newName))
        }
    }

    // CREATE DOCUMENT
    fun createDocument(title: String, folderId: Int?, templateType: String) {
        viewModelScope.launch {
            val starterBlocks = Templates.getDefaultBlocks(templateType, "A4 Write User")
            val blocksJson = BlockSerializer.toJson(starterBlocks)
            val newDoc = Document(
                title = if (title.isBlank()) "Untitled A4 Document" else title,
                folderId = folderId,
                contentJson = blocksJson,
                templateType = templateType.uppercase(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val docId = repository.insertDocument(newDoc)
            val savedDoc = repository.getDocumentById(docId.toInt())
            if (savedDoc != null) {
                openDocument(savedDoc)
            }
        }
    }

    // OPEN DOCUMENT
    fun openDocument(document: Document) {
        _activeDocument.value = document
        _editingBlocks.value = BlockSerializer.toList(document.contentJson)
        _aiResult.value = AiResultState.Idle
        _distractionFree.value = false
        setScreen(Screen.Editor)
    }

    // UPDATE DOCUMENT TITLE
    fun updateDocumentTitle(newTitle: String) {
        val currentDoc = _activeDocument.value ?: return
        val updated = currentDoc.copy(title = newTitle, updatedAt = System.currentTimeMillis())
        _activeDocument.value = updated
        viewModelScope.launch {
            repository.updateDocument(updated)
        }
    }

    // TOGGLE PIN DOCUMENT
    fun togglePinDocument(document: Document) {
        viewModelScope.launch {
            repository.updateDocument(document.copy(isPinned = !document.isPinned))
        }
    }

    // UPDATE LAYOUT PARAMETERS (Kindle layout style configs)
    fun updatePaperStyle(paperType: String) {
        val currentDoc = _activeDocument.value ?: return
        val updated = currentDoc.copy(paperColorType = paperType, updatedAt = System.currentTimeMillis())
        _activeDocument.value = updated
        saveDocumentNow(updated)
    }

    fun updateTypeface(typeface: String) {
        val currentDoc = _activeDocument.value ?: return
        val updated = currentDoc.copy(typefaceName = typeface, updatedAt = System.currentTimeMillis())
        _activeDocument.value = updated
        saveDocumentNow(updated)
    }

    fun updateFontScale(scale: Float) {
        val currentDoc = _activeDocument.value ?: return
        val updated = currentDoc.copy(fontScale = scale, updatedAt = System.currentTimeMillis())
        _activeDocument.value = updated
        saveDocumentNow(updated)
    }

    fun updatePageSpacing(spacing: Float) {
        val currentDoc = _activeDocument.value ?: return
        val updated = currentDoc.copy(pageSpacing = spacing, updatedAt = System.currentTimeMillis())
        _activeDocument.value = updated
        saveDocumentNow(updated)
    }

    // BLOCK MODIFICATIONS (Instantly in-memory + debounced database auto-save)
    fun updateBlockText(blockId: String, newText: String) {
        val updatedList = _editingBlocks.value.map {
            if (it.id == blockId) it.copy(text = newText) else it
        }
        _editingBlocks.value = updatedList
        triggerAutoSave()
    }

    fun updateBlockType(blockId: String, newType: String) {
        val updatedList = _editingBlocks.value.map {
            if (it.id == blockId) it.copy(type = newType) else it
        }
        _editingBlocks.value = updatedList
        triggerAutoSave()
    }

    fun addNewBlockAfter(currentBlockId: String) {
        val currentList = _editingBlocks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == currentBlockId }
        val newBlock = ContentBlock(id = UUID.randomUUID().toString(), type = "PARAGRAPH", text = "")
        if (index != -1) {
            currentList.add(index + 1, newBlock)
        } else {
            currentList.add(newBlock)
        }
        _editingBlocks.value = currentList
        triggerAutoSave()
    }

    fun deleteBlock(blockId: String) {
        val currentList = _editingBlocks.value.toMutableList()
        if (currentList.size <= 1) {
            // Keep at least one empty paragraph to edit
            _editingBlocks.value = listOf(ContentBlock(id = UUID.randomUUID().toString(), type = "PARAGRAPH", text = ""))
        } else {
            currentList.removeAll { it.id == blockId }
            _editingBlocks.value = currentList
        }
        triggerAutoSave()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000) // Debounce autosaver: write after 2 seconds of silence
            val currentDoc = _activeDocument.value ?: return@launch
            val blocksJson = BlockSerializer.toJson(_editingBlocks.value)
            val updated = currentDoc.copy(
                contentJson = blocksJson,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateDocument(updated)
            _activeDocument.value = updated
        }
    }

    private fun saveDocumentNow(doc: Document) {
        viewModelScope.launch {
            repository.updateDocument(doc)
        }
    }

    // Force save in full immediately (useful when manually pressing save or leaving)
    fun forceSave() {
        val currentDoc = _activeDocument.value ?: return
        val blocksJson = BlockSerializer.toJson(_editingBlocks.value)
        val updated = currentDoc.copy(
            contentJson = blocksJson,
            updatedAt = System.currentTimeMillis()
        )
        _activeDocument.value = updated
        saveDocumentNow(updated)
    }

    // DELETE DOCUMENT
    fun deleteDocument(documentId: Int) {
        viewModelScope.launch {
            repository.deleteDocumentById(documentId)
            if (_activeDocument.value?.id == documentId) {
                setScreen(Screen.Home)
            }
        }
    }

    // INTELLIGENT WRITING ASSISTANT (Gemini REST-driven checks and corrections)
    fun runAiFeature(command: String) {
        val currentDoc = _activeDocument.value ?: return
        val fullText = _editingBlocks.value.joinToString("\n") { it.text }

        if (fullText.trim().isEmpty()) {
            _aiResult.value = AiResultState.Error("A4 container is empty. Type some content first.")
            return
        }

        _aiResult.value = AiResultState.Checking
        viewModelScope.launch {
            val response = GeminiService.improveText(fullText, command)
            if (response.startsWith("Error:") || response.startsWith("Network Connection Failed")) {
                _aiResult.value = AiResultState.Error(response)
            } else {
                _aiResult.value = AiResultState.Success(response, command)
            }
        }
    }

    fun applyAiCorrection(correctedText: String) {
        // Parse the corrected blocks or simply replace the document blocks with corrected text paragraphs
        // Splitting lines into paragraphs of ContentBlocks is extremely simple and robust.
        val lines = correctedText.split("\n")
        val blocks = lines.map { line ->
            val type = if (line.startsWith("# ")) "HEADING_1" else if (line.startsWith("## ")) "HEADING_2" else if (line.startsWith("- ")) "BULLET_LIST" else "PARAGRAPH"
            val text = line.removePrefix("# ").removePrefix("## ").removePrefix("- ").trim()
            ContentBlock(id = UUID.randomUUID().toString(), type = type, text = text)
        }
        _editingBlocks.value = if (blocks.isEmpty()) listOf(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "")) else blocks
        _aiResult.value = AiResultState.Idle
        triggerAutoSave()
    }

    fun dismissAi() {
        _aiResult.value = AiResultState.Idle
    }
}

// Hierarchy Navigation Screens
sealed class Screen {
    object Home : Screen()
    object Editor : Screen()
}

// Sealed state for AI Writing improvements
sealed class AiResultState {
    object Idle : AiResultState()
    object Checking : AiResultState()
    data class Success(val responseText: String, val lastCommand: String) : AiResultState()
    data class Error(val message: String) : AiResultState()
}

// Simple Factory for ViewModel lifecycle
class DocumentViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
