package com.yaroslav.calcvault.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

data class VaultFile(val id: String, val originalName: String, val isVideo: Boolean)

object VaultManager {
    private const val VAULT_DIR = "secure_vault"
    private const val META_FILE = "metadata.json"
    private val KEY = "YaroslavSuperSecretKey12345678".toByteArray() // 32 bytes for AES-256

    private fun getVaultDir(context: Context): File {
        val dir = File(context.filesDir, VAULT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getMetaFile(context: Context): File = File(getVaultDir(context), META_FILE)

    fun saveFile(context: Context, uri: Uri, fileName: String, isVideo: Boolean): VaultFile {
        val id = System.currentTimeMillis().toString()
        val ext = if (isVideo) ".mp4" else ".jpg"
        val encryptedFile = File(getVaultDir(context), "$id$ext")

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            encrypt(inputStream, encryptedFile.outputStream())
        }

        val meta = loadMetadata(context)
        val newFile = VaultFile(id, fileName, isVideo)
        meta.put(newFile.toJson())
        getMetaFile(context).writeText(meta.toString())
        
        return newFile
    }

    fun getFiles(context: Context): List<VaultFile> {
        val meta = loadMetadata(context)
        val list = mutableListOf<VaultFile>()
        for (i in 0 until meta.length()) {
            list.add(VaultFile.fromJson(meta.getJSONObject(i)))
        }
        return list
    }

    fun deleteFile(context: Context, id: String) {
        val dir = getVaultDir(context)
        dir.listFiles()?.find { it.name.startsWith(id) }?.delete()
        
        val meta = loadMetadata(context)
        val newMeta = JSONArray()
        for (i in 0 until meta.length()) {
            val obj = meta.getJSONObject(i)
            if (obj.getString("id") != id) newMeta.put(obj)
        }
        getMetaFile(context).writeText(newMeta.toString())
    }

    fun getDecryptedTempFile(context: Context, id: String): File? {
        val dir = getVaultDir(context)
        val encFile = dir.listFiles()?.find { it.name.startsWith(id) } ?: return null
        val tempFile = File(context.cacheDir, "temp_${encFile.name}")
        
        encFile.inputStream().use { inputStream ->
            decrypt(inputStream, tempFile.outputStream())
        }
        return tempFile
    }

    private fun loadMetadata(context: Context): JSONArray {
        val file = getMetaFile(context)
        return if (file.exists()) JSONArray(file.readText()) else JSONArray()
    }

    private fun encrypt(input: InputStream, output: OutputStream) {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY, "AES"))
        CipherOutputStream(output, cipher).use { cos ->
            input.copyTo(cos)
        }
    }

    private fun decrypt(input: InputStream, output: OutputStream) {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(KEY, "AES"))
        CipherInputStream(input, cipher).use { cis ->
            cis.copyTo(output)
        }
    }

    private fun VaultFile.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", originalName)
            put("isVideo", isVideo)
        }
    }

    private fun VaultFile.Companion.fromJson(json: JSONObject): VaultFile {
        return VaultFile(json.getString("id"), json.getString("name"), json.getBoolean("isVideo"))
    }
    
    // Extension to allow Companion usage in fromJson
    private val VaultFile.Companion: Unit get() = Unit
}
