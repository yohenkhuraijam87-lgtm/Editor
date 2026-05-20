package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.DocumentViewModel
import com.example.ui.DocumentViewModelFactory
import com.example.ui.EditorScreen
import com.example.ui.HomeScreen
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: DocumentViewModel by viewModels {
        DocumentViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val folders by viewModel.folders.collectAsState()
                val documents by viewModel.documents.collectAsState()
                val pinnedDocs by viewModel.pinnedDocuments.collectAsState()
                val selectedFolderId by viewModel.selectedFolderId.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()

                val paperColorType by viewModel.paperColorType.collectAsState()
                val typefaceName by viewModel.typefaceName.collectAsState()
                val fontScale by viewModel.fontScale.collectAsState()
                val pageSpacing by viewModel.pageSpacing.collectAsState()
                val distractionFree by viewModel.distractionFree.collectAsState()
                val aiResult by viewModel.aiResult.collectAsState()

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(), // Ensures safe notch margins from the top of the canvas
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        label = "ScreenTransition",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }
                    ) { screen ->
                        when (screen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    folders = folders,
                                    documents = documents,
                                    pinnedDocuments = pinnedDocs,
                                    selectedFolderId = selectedFolderId,
                                    searchQuery = searchQuery,
                                    onOpenDoc = { doc -> viewModel.openDocument(doc) }
                                )
                            }
                            is Screen.Editor -> {
                                viewModel.activeDocument.value?.let { activeDoc ->
                                    EditorScreen(
                                        viewModel = viewModel,
                                        document = activeDoc,
                                        editingBlocks = viewModel.editingBlocks.collectAsState().value,
                                        paperColorType = paperColorType,
                                        typefaceName = typefaceName,
                                        fontScale = fontScale,
                                        pageSpacing = pageSpacing,
                                        distractionFree = distractionFree,
                                        aiResult = aiResult,
                                        onBack = { viewModel.setScreen(Screen.Home) }
                                    )
                                } ?: viewModel.setScreen(Screen.Home)
                            }
                        }
                    }
                }
            }
        }
    }
}

