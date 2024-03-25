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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.withContext

private const val DOCUMENT_WORD_DOC = "application/msword"
private const val DOCUMENT_WORD_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
private const val DOCUMENT_EXCEL_XLS = "application/vnd.ms-excel"
private const val DOCUMENT_EXCEL_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
private const val DOCUMENT_PDF = "application/pdf"
private const val DOCUMENT_IMAGE = "image/*"
private const val DOCUMENT_CSV = "text/csv"
private const val DOCUMENT_ZIP = "application/zip"


private const val FILE_PICKER_TAG = "FilePicker"

private val defaultDocumentInput = arrayOf(
    DOCUMENT_PDF,
    DOCUMENT_WORD_DOC,
    DOCUMENT_EXCEL_XLS,
    DOCUMENT_ZIP,
    DOCUMENT_WORD_DOCX,
    DOCUMENT_EXCEL_XLSX,
    "application/vnd.ms-excel.sheet.macroEnabled.12",
    DOCUMENT_IMAGE,
    DOCUMENT_CSV,
)
    private const val BUFFER_SIZE = 1024 * 1024
    private const val SLASH = "/"
    private const val CONTENT = "content"
    private const val DEFAULT_FILE_NAME = "DEFAULT_FILE_NAME"


interface FilePickerResultCallback{
    fun result(file: File)
}

enum class FileExtension(val documentInput: String){
    DOC(DOCUMENT_WORD_DOC),
    DOCX(DOCUMENT_WORD_DOCX),
    XLS(DOCUMENT_EXCEL_XLS),
    XLSX(DOCUMENT_EXCEL_XLSX),
    IMAGE(DOCUMENT_IMAGE),
    CSV(DOCUMENT_CSV),
    ZIP(DOCUMENT_ZIP),
    PDF(DOCUMENT_PDF)
}

class FilePickerOptions(
    val availableExtension: Array<FileExtension>? = null,
    val maxSizeInByte: Int = BUFFER_SIZE
)

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

    fun launch(filePickerResult: FilePickerResultCallback, options: FilePickerOptions = FilePickerOptions()){
        if(fragment!= null){
            this.activity = fragment!!.requireActivity()
            this.context = fragment!!.requireContext()
        }
        this.filePickerResult = filePickerResult
        val documentInput: Array<String> = options.availableExtension?.map {
            it.documentInput
        }?.toTypedArray() ?: defaultDocumentInput
        getDocument.launch(documentInput)
    }

    private fun contentHandler(uri: Uri?) {
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val file = getFileFromUri(it)
                withContext(Dispatchers.Main){
                    filePickerResult?.result(file)
                }
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
            Log.e(FILE_PICKER_TAG,"Get file from uri error: $e")
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