package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.ContentBlock
import com.example.data.Document
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: DocumentViewModel,
    document: Document,
    editingBlocks: List<ContentBlock>,
    paperColorType: String,
    typefaceName: String,
    fontScale: Float,
    pageSpacing: Float,
    distractionFree: Boolean,
    aiResult: AiResultState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showSettingsMenu by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(document.title) }
    var isRenaming by remember { mutableStateOf(false) }
    var activeBlockIdForToolbar by remember { mutableStateOf<String?>(null) }
    
    // Set up paper background and text color based on document preferences
    val (paperBgColor, paperTextColor, pageFrameBgColor) = when (paperColorType.uppercase()) {
        "SEPIA" -> Triple(Color(0xFFF4ECD8), Color(0xFF3C3B33), Color(0xFFE5D9BD))
        "DARK" -> Triple(Color(0xFF1E1E1E), Color(0xFFE0E0E0), Color(0xFF121212))
        else -> Triple(Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFFF1F5F9)) // Standard white paper on slate-grey frame
    }

    val selectedFontFamily = when (typefaceName.uppercase()) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    // Baseline sizes scaled by the document scale
    val baseParagraphSize = (14 * fontScale).sp
    val baseH1Size = (24 * fontScale).sp
    val baseH2Size = (19 * fontScale).sp
    val baseLineHeight = baseParagraphSize * pageSpacing

    // Auto-save feedback message
    var saveStatusMsg by remember { mutableStateOf("Autosaved locally") }

    Scaffold(
        topBar = {
            if (!distractionFree) {
                TopAppBar(
                    title = {
                        if (isRenaming) {
                            OutlinedTextField(
                                value = renameInput,
                                onValueChange = { renameInput = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rename_document_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (renameInput.isNotBlank()) {
                                                viewModel.updateDocumentTitle(renameInput)
                                                isRenaming = false
                                            }
                                        },
                                        modifier = Modifier.testTag("save_rename_button")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save Title")
                                    }
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .clickable { isRenaming = true }
                                    .testTag("document_title_display"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = document.title,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.forceSave()
                                onBack()
                            },
                            modifier = Modifier.testTag("back_to_home_button")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // AI check button
                        IconButton(
                            onClick = { viewModel.runAiFeature("PROOFREAD") },
                            modifier = Modifier.testTag("ai_proofread_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Toggle settings typography menu
                        IconButton(
                            onClick = { showSettingsMenu = !showSettingsMenu },
                            modifier = Modifier.testTag("typography_settings_button")
                        ) {
                            Icon(imageVector = Icons.Default.FontDownload, contentDescription = "Typography")
                        }

                        // Export Trigger button
                        var showExportOptions by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showExportOptions = true },
                            modifier = Modifier.testTag("export_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.IosShare, contentDescription = "Export")
                        }

                        if (showExportOptions) {
                            DropdownMenu(
                                expanded = showExportOptions,
                                onDismissRequest = { showExportOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export standard A4 PDF", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        showExportOptions = false
                                        viewModel.forceSave()
                                        val pdfFile = PdfExporter.exportToPdf(context, document, editingBlocks)
                                        if (pdfFile != null) {
                                            sharePdf(context, pdfFile)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share DOCX Compatible HTML") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    onClick = {
                                        showExportOptions = false
                                        shareHtml(context, document.title, editingBlocks)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Plain Text") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showExportOptions = false
                                        shareText(context, document.title, editingBlocks)
                                    }
                                )
                            }
                        }

                        // Distraction-Free Toggle
                        IconButton(
                            onClick = { viewModel.toggleDistractionFree() },
                            modifier = Modifier.testTag("distraction_free_toggle")
                        ) {
                            Icon(
                                imageVector = if (distractionFree) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Distraction Free"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!distractionFree) {
                // Formatting helper floating bar just above system navigation
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 48.dp) {
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { viewModel.updateBlockType(it, "HEADING_1") }
                                },
                                modifier = Modifier.testTag("format_h1_button")
                            ) {
                                Text("H1", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { viewModel.updateBlockType(it, "HEADING_2") }
                                },
                                modifier = Modifier.testTag("format_h2_button")
                            ) {
                                Text("H2", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { viewModel.updateBlockType(it, "PARAGRAPH") }
                                },
                                modifier = Modifier.testTag("format_paragraph_button")
                            ) {
                                Text("¶", fontWeight = FontWeight.Medium)
                            }
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { viewModel.updateBlockType(it, "BULLET_LIST") }
                                },
                                modifier = Modifier.testTag("format_bullet_button")
                            ) {
                                Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = "Bullet List")
                            }
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { viewModel.updateBlockType(it, "ALIGN_CENTER") }
                                },
                                modifier = Modifier.testTag("format_align_center")
                            ) {
                                Icon(imageVector = Icons.Default.FormatAlignCenter, contentDescription = "Center")
                            }
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { viewModel.updateBlockType(it, "ALIGN_RIGHT") }
                                },
                                modifier = Modifier.testTag("format_align_right")
                            ) {
                                Icon(imageVector = Icons.Default.FormatAlignRight, contentDescription = "Right")
                            }
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { viewModel.addNewBlockAfter(it) }
                                },
                                modifier = Modifier.testTag("add_block_button")
                            ) {
                                Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Below", tint = Color(0xFF059669))
                            }
                            IconButton(
                                onClick = {
                                    activeBlockIdForToolbar?.let { 
                                        viewModel.deleteBlock(it)
                                        activeBlockIdForToolbar = null
                                    }
                                },
                                modifier = Modifier.testTag("delete_block_button")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(pageFrameBgColor)
        ) {
            // MAIN CANVAS (REPRESENTING THE A4 SHEETS)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp)
                    .testTag("a4_paper_scrollable"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    // Page physical dimensions represent margins & elevated design
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(8.dp, shape = RoundedCornerShape(4.dp))
                            .background(paperBgColor)
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 24.dp, vertical = 32.dp) // Physical A4 Margins
                            .testTag("a4_paper_canvas")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Page title visualization
                            Text(
                                text = document.title.uppercase(),
                                fontSize = 10.sp,
                                letterSpacing = 2.sp,
                                color = paperTextColor.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Iterate block-by-block editors
                            editingBlocks.forEachIndexed { index, block ->
                                val active = activeBlockIdForToolbar == block.id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            activeBlockIdForToolbar = block.id 
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = if (active && !distractionFree) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Bullet layout or spacing
                                        if (block.type == "BULLET_LIST") {
                                            Text(
                                                text = "• ",
                                                fontSize = baseParagraphSize,
                                                color = paperTextColor,
                                                fontFamily = selectedFontFamily,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                        }

                                        val styleToUse = when (block.type) {
                                            "HEADING_1" -> TextStyle(
                                                fontSize = baseH1Size,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = selectedFontFamily,
                                                color = paperTextColor,
                                                lineHeight = baseLineHeight * 1.2f
                                            )
                                            "HEADING_2" -> TextStyle(
                                                fontSize = baseH2Size,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = selectedFontFamily,
                                                color = paperTextColor,
                                                lineHeight = baseLineHeight * 1.15f
                                            )
                                            "ALIGN_CENTER" -> TextStyle(
                                                fontSize = baseParagraphSize,
                                                fontFamily = selectedFontFamily,
                                                color = paperTextColor,
                                                textAlign = TextAlign.Center,
                                                lineHeight = baseLineHeight
                                            )
                                            "ALIGN_RIGHT" -> TextStyle(
                                                fontSize = baseParagraphSize,
                                                fontFamily = selectedFontFamily,
                                                color = paperTextColor,
                                                textAlign = TextAlign.Right,
                                                lineHeight = baseLineHeight
                                            )
                                            else -> TextStyle(
                                                fontSize = baseParagraphSize,
                                                fontFamily = selectedFontFamily,
                                                color = paperTextColor,
                                                lineHeight = baseLineHeight
                                            )
                                        }

                                        // Inline transparent borderless text editor
                                        BasicTextField(
                                            value = block.text,
                                            onValueChange = { viewModel.updateBlockText(block.id, it) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("block_text_${block.id}"),
                                            textStyle = styleToUse
                                        )

                                        // quick context actions if clicked
                                        if (active && !distractionFree) {
                                            IconButton(
                                                onClick = { viewModel.addNewBlockAfter(block.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "New Block",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // FLOATING ACTION OVERLAY IF IN DISTRACTION FREE MODE (To restore layout bar)
            if (distractionFree) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleDistractionFree() },
                        modifier = Modifier.testTag("restore_layout_fab"),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Restore Menu UI")
                    }
                }
            }

            // CUSTOM CONFIG TYPOGRAPHY MENU SLIDE
            AnimatedVisibility(
                visible = showSettingsMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Kindle Reader Layout",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showSettingsMenu = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Settings")
                            }
                        }

                        // PAGE THEME COLORS
                        Column {
                            Text("Page Palette Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(
                                    "PAPER" to "Studio White",
                                    "SEPIA" to "Warm Sepia",
                                    "DARK" to "Slate Dark"
                                ).forEach { (type, name) ->
                                    val isSelected = paperColorType.uppercase() == type
                                    val bg = when (type) {
                                        "SEPIA" -> Color(0xFFF4ECD8)
                                        "DARK" -> Color(0xFF1E1E1E)
                                        else -> Color.White
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.updatePaperStyle(type) }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (type == "DARK") Color.White else Color.Black,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // FONT SCALE CONTROLLER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Text Scaling Size", fontWeight = FontWeight.Bold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.updateFontScale((fontScale - 0.1f).coerceAtLeast(0.6f)) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    String.format("%.1fx", fontScale),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.updateFontScale((fontScale + 0.1f).coerceAtMost(2.0f)) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // FONT TYPEFACE CONTROLLER
                        Column {
                            Text("Page Typeset Font", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Serif", "Sans", "Monospace").forEach { face ->
                                    val isSelected = face.uppercase() == typefaceName.uppercase()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.updateTypeface(face) }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            face,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = when (face.uppercase()) {
                                                "SERIF" -> FontFamily.Serif
                                                "MONOSPACE" -> FontFamily.Monospace
                                                else -> FontFamily.SansSerif
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // LINE SPACING CONTROLLER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kindle Vertical Spacing", fontWeight = FontWeight.Bold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1.0f, 1.15f, 1.5f, 2.0f).forEach { spacing ->
                                    val isSelected = spacing == pageSpacing
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updatePageSpacing(spacing) },
                                        label = { Text("${spacing}x") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // GEMINI WRITING ASSISTANT INTELLIGENT PANEL
            AnimatedVisibility(
                visible = aiResult !is AiResultState.Idle,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "AI Writing Partner",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            IconButton(onClick = { viewModel.dismissAi() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close AI panel")
                            }
                        }

                        when (aiResult) {
                            is AiResultState.Checking -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                    Text(
                                        "Gemini is analyzing styling, spelling, and tone boundaries...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            is AiResultState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = (aiResult as AiResultState.Error).message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Button(
                                        onClick = { viewModel.dismissAi() },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                            is AiResultState.Success -> {
                                val successState = aiResult as AiResultState.Success
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Option toggles to analyze in real-time
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf(
                                            "PROOFREAD" to "Spellcheck",
                                            "FORMAL" to "Formalize",
                                            "SIMPLIFY" to "Simplify",
                                            "IMPROVE" to "Improve Style"
                                        ).forEach { (cmd, label) ->
                                            val isSelected = successState.lastCommand == cmd
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                    .clickable { viewModel.runAiFeature(cmd) }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Preview Gemini Optimization:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    
                                    // Response display container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            item {
                                                Text(
                                                    text = successState.responseText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontFamily = FontFamily.Serif
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { viewModel.dismissAi() }) {
                                            Text("Discard")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.applyAiCorrection(successState.responseText) }
                                        ) {
                                            Text("Apply Suggestion")
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

// Share Intent triggers (100% standard native Android shares)
private fun sharePdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export A4 Paper PDF via..."))
}

private fun shareHtml(context: Context, title: String, blocks: List<ContentBlock>) {
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("<!DOCTYPE html><html><head><title>$title</title><meta charset='utf-8'></head><body>")
    htmlBuilder.append("<h1>$title</h1><hr>")
    for (block in blocks) {
        when (block.type) {
            "HEADING_1" -> htmlBuilder.append("<h2>${block.text}</h2>")
            "HEADING_2" -> htmlBuilder.append("<h3>${block.text}</h3>")
            "BULLET_LIST" -> htmlBuilder.append("<ul><li>${block.text}</li></ul>")
            "ALIGN_CENTER" -> htmlBuilder.append("<p style='text-align:center;'>${block.text}</p>")
            "ALIGN_RIGHT" -> htmlBuilder.append("<p style='text-align:right;'>${block.text}</p>")
            else -> htmlBuilder.append("<p>${block.text}</p>")
        }
    }
    htmlBuilder.append("</body></html>")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/html"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, htmlBuilder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share DOCX Web Format via..."))
}

private fun shareText(context: Context, title: String, blocks: List<ContentBlock>) {
    val textBuilder = StringBuilder()
    textBuilder.append("Document: $title\n")
    textBuilder.append("=========================\n\n")
    for (block in blocks) {
        val line = if (block.type == "BULLET_LIST") {
            "• ${block.text}"
        } else {
            block.text
        }
        textBuilder.append(line).append("\n\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Plain Text via..."))
}
