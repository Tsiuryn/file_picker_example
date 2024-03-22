package com.example.onactivityresulttest

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.app.AppCompatActivity

import kotlin.coroutines.CoroutineContext

private val documentInput = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.ms-excel",
    "application/zip",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel.sheet.macroEnabled.12",
    "image/*",
    "text/csv",
)
    private val BUFFER_SIZE = 1024 * 1024
    private val SLASH = "/"
    private val CONTENT = "content"
    private val DEFAULT_FILE_NAME = "DEFAULT_FILE_NAME"


interface FilePickerResultCallback{
    fun result(file: File)
}

/**
 * A class [FilePicker] must be initialized before the activity is created.
 */
class FilePicker {

    private var filePickerResult: FilePickerResultCallback? = null

    private lateinit var activity: Activity
    private lateinit var context: Context
    private var getDocument: ActivityResultLauncher<Array<String>>
    private var fragment: Fragment? = null

    constructor(fragment: Fragment) {
        this.fragment = fragment
        this.getDocument = fragment.registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            contentHandler(uri)
        }
    }

    constructor(activity: AppCompatActivity){
        this.activity = activity
        this.context = activity
        this.getDocument = activity.registerForActivityResult(
                ActivityResultContracts.OpenDocument(),
        ) { uri ->
            contentHandler(uri)
        }
    }

    fun launch(filePickerResult: FilePickerResultCallback){
        if(fragment!= null){
            this.activity = fragment!!.requireActivity()
            this.context = fragment!!.requireContext()
        }
        this.filePickerResult = filePickerResult
        getDocument.launch(documentInput)
    }

    private fun contentHandler(uri: Uri?) {
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val file = getFileFromUri(it)
                filePickerResult?.result(file)
            }
        }
    }


    private fun getFileFromUri(uri: Uri): File {
        val file = File(getNewFilePath(getNameFromExistingFile(uri)))

        try {
            activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val bytesAvailable = inputStream.available().coerceAtMost(BUFFER_SIZE)
                    val buffer = ByteArray(bytesAvailable)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (e: Throwable) {
//        Log.e("Get file from uri error: $e")
        }

        return file
    }

    private fun getNewFilePath(fileName: String = "DefaultName"): String {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val file: File? = ContextCompat.getExternalFilesDirs(context.applicationContext, null)[0]
            if (file == null) context.applicationContext.filesDir.absolutePath + SLASH + fileName
            else file.absolutePath + SLASH + fileName
        } else {
            context.applicationContext.filesDir.absolutePath + SLASH + fileName
        }
    }

    private fun getNameFromExistingFile(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == CONTENT) {
            val cursor: Cursor? = context.contentResolver
                .query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) result = cursor
                    .getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } catch (ex: IllegalArgumentException) {
                Log.e("TAG", "Error")
            } finally {
                cursor?.close()
            }
        }

        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf(SLASH)
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: DEFAULT_FILE_NAME
    }
}