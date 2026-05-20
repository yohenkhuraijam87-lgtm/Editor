package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Document
import com.example.data.Folder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: DocumentViewModel,
    folders: List<Folder>,
    documents: List<Document>,
    pinnedDocuments: List<Document>,
    selectedFolderId: Int?,
    searchQuery: String,
    onOpenDoc: (Document) -> Unit
) {
    val context = LocalContext.current
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var selectedFolderColorHex by remember { mutableStateOf("#475569") }

    var showCreateDocDialog by remember { mutableStateOf(false) }
    var docNameInput by remember { mutableStateOf("") }
    var selectedTemplateForNewDoc by remember { mutableStateOf("BLANK") }

    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var docToDelete by remember { mutableStateOf<Document?>(null) }

    // Color choices for folders
    val folderColors = listOf(
        "#E11D48" to "Rose",
        "#D97706" to "Amber",
        "#059669" to "Emerald",
        "#2563EB" to "Blue",
        "#7C3AED" to "Violet",
        "#475569" to "Slate"
    )

    // Filter documents based on Folder selection & Search query
    val filteredDocs = documents.filter { doc ->
        val matchesFolder = if (selectedFolderId != null) {
            doc.folderId == selectedFolderId
        } else {
            true // show all
        }
        val matchesSearch = if (searchQuery.isNotBlank()) {
            doc.title.contains(searchQuery, ignoreCase = true)
        } else {
            true
        }
        matchesFolder && matchesSearch
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "A4 Write",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Power of Word. Elegance of Kindle.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.testTag("add_folder_button")
                    ) {
                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    docNameInput = ""
                    selectedTemplateForNewDoc = "BLANK"
                    showCreateDocDialog = true
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("create_doc_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Document")
                    Text("New Document", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SEARCH BAR
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    placeholder = { Text("Search drafts, notes, resumes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // TEMPLATE STARTER GALLERY
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A4 Starter Templates",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        val templates = listOf(
                            Triple("BLANK", "Blank Page", Icons.Outlined.NoteAdd),
                            Triple("RESUME", "Modern Resume", Icons.Outlined.ContactPage),
                            Triple("ESSAY", "Research Essay", Icons.Outlined.MenuBook),
                            Triple("LETTER", "Cover Letter", Icons.Outlined.MailOutline),
                            Triple("NOTES", "Cornell Notes", Icons.Outlined.FormatListNumbered)
                        )
                        items(templates) { (type, name, icon) ->
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable {
                                        viewModel.createDocument("", selectedFolderId, type)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Simulated A4 portrait mini representation
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Simple placeholder representation of content
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = name,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            // tiny lines for style
                                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.LightGray))
                                            Box(modifier = Modifier.width(30.dp).height(2.dp).background(Color.LightGray))
                                        }
                                    }
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FOLDERS HORIZONTAL ROW FILTER
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Folders / Categories",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Folders bubble
                        item {
                            FilterChip(
                                selected = selectedFolderId == null,
                                onClick = { viewModel.selectFolder(null) },
                                label = { Text("All Documents") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                            )
                        }

                        // Configured folders
                        items(folders) { folder ->
                            val parsedColor = remember(folder.colorHex) {
                                try { Color(android.graphics.Color.parseColor(folder.colorHex)) } catch (e: Exception) { Color.Gray }
                            }
                            
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = { viewModel.selectFolder(folder.id) },
                                label = { Text(folder.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { folderToDelete = folder },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete folder",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // PINNED DOCUMENTS
            if (pinnedDocuments.isNotEmpty() && selectedFolderId == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Pinned A4 Papers",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pinnedDocuments) { doc ->
                                Card(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .combinedClickable(
                                            onClick = { onOpenDoc(doc) },
                                            onLongClick = { docToDelete = doc }
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = "Pinned",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.togglePinDocument(doc) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Unpin",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Template: ${doc.templateType}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PRIMARY LISTINGS
            item {
                Text(
                    text = if (selectedFolderId != null) "Documents in Folder" else "Recent Writings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (filteredDocs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "Empty",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No drafts found matching search." else "No documents found. Start a new blank A4 draft!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredDocs) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onOpenDoc(doc) },
                                onLongClick = { docToDelete = doc }
                            )
                            .testTag("document_item_${doc.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // A4 icon badge
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A4",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = doc.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val formattedDate = remember(doc.updatedAt) {
                                        val format = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                                        format.format(Date(doc.updatedAt))
                                    }
                                    Text(
                                        text = "Last updated: $formattedDate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.togglePinDocument(doc) }
                                ) {
                                    Icon(
                                        imageVector = if (doc.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = "Pin document",
                                        tint = if (doc.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { docToDelete = doc }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete document",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Spacer to avoid overlapping with FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // CREATE FOLDER DIALOG
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        placeholder = { Text("Folder Name (e.g. University, Resume)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_folder_name_input")
                    )

                    Column {
                        Text("Color Tag:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            folderColors.forEach { (hex, name) ->
                                val parsed = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(parsed)
                                        .clickable { selectedFolderColorHex = hex }
                                        .padding(if (selectedFolderColorHex == hex) 4.dp else 0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedFolderColorHex == hex) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.createFolder(folderNameInput, selectedFolderColorHex)
                            folderNameInput = ""
                            showCreateFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_folder")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CREATE DOCUMENT DIALOG
    if (showCreateDocDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDocDialog = false },
            title = { Text("Create A4 Draft") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = docNameInput,
                        onValueChange = { docNameInput = it },
                        placeholder = { Text("Document Title (e.g. Term Paper)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_doc_title_input")
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Choose Format Template:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        val formats = listOf(
                            "BLANK" to "Blank A4 layout",
                            "RESUME" to "Professional resume",
                            "ESSAY" to "College academic essay",
                            "LETTER" to "Official Cover Letter",
                            "NOTES" to "Handy Cornell notes"
                        )
                        formats.forEach { (type, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTemplateForNewDoc = type }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selectedTemplateForNewDoc == type,
                                    onClick = { selectedTemplateForNewDoc = type }
                                )
                                Column {
                                    Text(type, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createDocument(docNameInput, selectedFolderId, selectedTemplateForNewDoc)
                        showCreateDocDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_doc")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDocDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE FOLDER DIALOG
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete folder '${folder.name}'? Documents associated with this folder will not be deleted, they will be moved to general draft lists.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folder)
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE DOCUMENT DIALOG
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Delete Draft") },
            text = { Text("Are you sure you want to permanently delete '${doc.title}'? This action is offline-safe but cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc.id)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
