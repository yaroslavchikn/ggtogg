package com.yaroslav.calcvault.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.yaroslav.calcvault.data.VaultFile
import com.yaroslav.calcvault.data.VaultManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(VaultManager.getFiles(context)) }
    var selectedFile by remember { mutableStateOf<VaultFile?>(null) }

    // Используем современный Photo Picker, не требует разрешений!
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast('/') ?: "file"
            val isVideo = context.contentResolver.getType(it)?.startsWith("video") == true
            VaultManager.saveFile(context, it, name, isVideo)
            files = VaultManager.getFiles(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Секретное хранилище", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pickerLauncher.launch(ActivityResultContracts.PickVisualMedia.ImageAndVideo) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Добавить", tint = Color.Black)
            }
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Хранилище пусто.\nНажми +, чтобы добавить.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    VaultItem(file, context, onDelete = {
                        VaultManager.deleteFile(context, file.id)
                        files = VaultManager.getFiles(context)
                    }, onClick = {
                        selectedFile = file
                    })
                }
            }
        }
    }

    // Диалог для просмотра фото
    selectedFile?.let { file ->
        if (!file.isVideo) {
            val tempFile = VaultManager.getDecryptedTempFile(context, file.id)
            AlertDialog(
                onDismissRequest = { 
                    selectedFile = null 
                    tempFile?.delete()
                },
                confirmButton = {
                    TextButton(onClick = { 
                        selectedFile = null 
                        tempFile?.delete()
                    }) { Text("Закрыть") }
                },
                title = { Text(file.originalName) },
                text = {
                    tempFile?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            )
        } else {
            // Для видео открываем внешним плеером через Intent
            val tempFile = VaultManager.getDecryptedTempFile(context, file.id)
            tempFile?.let {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
            selectedFile = null
            tempFile?.deleteOnExit()
        }
    }
}

@Composable
fun VaultItem(file: VaultFile, context: Context, onDelete: () -> Unit, onClick: () -> Unit) {
    val tempFile = remember(file.id) { VaultManager.getDecryptedTempFile(context, file.id) }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A2A))
            .clickable { onClick() }
    ) {
        if (!file.isVideo && tempFile != null) {
            AsyncImage(
                model = tempFile,
                contentDescription = file.originalName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("▶", color = Color.White, fontSize = 32.sp)
            }
        }
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
        ) {
            Icon(Icons.Default.Delete, "Удалить", tint = Color.Red, modifier = Modifier.size(16.dp))
        }
    }
}
