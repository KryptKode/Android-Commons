package com.kryptkode.common.extensions

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Point
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kryptkode.common.R
import com.kryptkode.common.helpers.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

val Context.isRTLLayout: Boolean get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL


fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (e: Exception) {
    }
}

private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.an_error_occurred), msg), length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}




fun Context.getLatestMediaId(uri: Uri = MediaStore.Files.getContentUri("external")): Long {
    val MAX_VALUE = "max_value"
    val projection = arrayOf("MAX(${BaseColumns._ID}) AS $MAX_VALUE")
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor?.moveToFirst() == true) {
            return cursor.getLongValue(MAX_VALUE)
        }
    } finally {
        cursor?.close()
    }
    return 0
}

fun Context.getLatestMediaByDateId(uri: Uri = MediaStore.Files.getContentUri("external")): Long {
    val projection = arrayOf(BaseColumns._ID)
    val sortOrder = "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, null, null, sortOrder)
        if (cursor?.moveToFirst() == true) {
            return cursor.getLongValue(BaseColumns._ID)
        }
    } finally {
        cursor?.close()
    }
    return 0
}

// some helper functions were taken from https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun Context.getRealPathFromURI(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }

    if (isDownloadsDocument(uri)) {
        val id = DocumentsContract.getDocumentId(uri)
        if (id.areDigitsOnly()) {
            val newUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
            val path = getDataColumn(newUri)
            if (path != null) {
                return path
            }
        }
    } else if (isExternalStorageDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val parts = documentId.split(":")
        if (parts[0].equals("primary", true)) {
            return "${Environment.getExternalStorageDirectory().absolutePath}/${parts[1]}"
        }
    } else if (isMediaDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val split = documentId.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
        val type = split[0]

        val contentUri = when (type) {
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val selection = "_id=?"
        val selectionArgs = arrayOf(split[1])
        val path = getDataColumn(contentUri, selection, selectionArgs)
        if (path != null) {
            return path
        }
    }

    return getDataColumn(uri)
}

fun Context.getDataColumn(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): String? {
    var cursor: Cursor? = null
    try {
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            val data = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
            if (data != "null") {
                return data
            }
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}

private fun isMediaDocument(uri: Uri) = uri.authority == "com.android.providers.media.documents"

private fun isDownloadsDocument(uri: Uri) = uri.authority == "com.android.providers.downloads.documents"

private fun isExternalStorageDocument(uri: Uri) = uri.authority == "com.android.externalstorage.documents"

fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(this, getPermissionString(permId)) == PackageManager.PERMISSION_GRANTED

fun Context.getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    PERMISSION_CAMERA -> Manifest.permission.CAMERA
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
    PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
    PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
    PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
    PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
    PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
    PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
    PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
    else -> ""
}

fun Context.getFilePublicUri(file: File, applicationId: String): Uri {
    // for images/videos/gifs try getting a media content uri first, like content://media/external/images/media/438
    // if media content uri is null, get our custom uri like content://com.simplemobiletools.gallery.provider/external_files/emulated/0/DCIM/IMG_20171104_233915.jpg
    var uri = if (file.isMediaFile()) {
        getMediaContentUri(file.absolutePath)
    } else {
        getMediaContent(file.absolutePath, MediaStore.Files.getContentUri("external"))
    }

    if (uri == null) {
        uri = FileProvider.getUriForFile(this, "$applicationId.provider", file)
    }

    return uri!!
}

fun Context.getMediaContentUri(path: String): Uri? {
    val uri = when {
        path.isImageFast() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        path.isVideoFast() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Files.getContentUri("external")
    }

    return getMediaContent(path, uri)
}

fun Context.getMediaContent(path: String, uri: Uri): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = MediaStore.Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            val id = cursor.getIntValue(MediaStore.Images.Media._ID).toString()
            return Uri.withAppendedPath(uri, id)
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}

fun Context.getFilenameFromUri(uri: Uri): String {
    return if (uri.scheme == "file") {
        File(uri.toString()).name
    } else {
        var name = getFilenameFromContentUri(uri) ?: ""
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: ""
        }
        name
    }
}

fun Context.getMimeTypeFromUri(uri: Uri): String {
    var mimetype = uri.path?.getMimeType()
    if (mimetype?.isEmpty() == true) {
        try {
            mimetype = contentResolver?.getType(uri)
        } catch (e: IllegalStateException) {
        }
    }
    return mimetype ?: ""
}

fun Context.ensurePublicUri(path: String, applicationId: String): Uri? {
    val uri = Uri.parse(path)
    return when {
        uri.scheme == "content" -> uri
        else -> {
            val newPath = if (uri.toString().startsWith("/")) uri.toString() else uri.path
            val file = File(newPath)
            getFilePublicUri(file, applicationId)
        }
    }
}

fun Context.ensurePublicUri(uri: Uri, applicationId: String): Uri {
    return if (uri.scheme == "content") {
        uri
    } else {
        val file = File(uri.path)
        getFilePublicUri(file, applicationId)
    }
}

fun Context.getFilenameFromContentUri(uri: Uri): String? {
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor?.moveToFirst() == true) {
            return cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return ""
}




fun Context.getCurrentFormattedDateTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
    return simpleDateFormat.format(Date(System.currentTimeMillis()))
}



fun Context.getUriMimeType(path: String, newUri: Uri): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
}

fun Context.isThankYouInstalled() = isPackageInstalled("com.simplemobiletools.thankyou")

fun Context.isAProApp() = packageName.startsWith("com.simplemobiletools.") && packageName.removeSuffix(".debug").endsWith(".pro")

fun Context.isPackageInstalled(pkgName: String): Boolean {
    return try {
        packageManager.getPackageInfo(pkgName, 0)
        true
    } catch (e: Exception) {
        false
    }
}





fun Context.grantReadUriPermission(uriString: String) {
    try {
        // ensure custom reminder sounds play well
        grantUriPermission("com.android.systemui", Uri.parse(uriString), Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (ignored: Exception) {
    }
}


fun Context.saveExifRotation(exif: ExifInterface, degrees: Int) {
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val orientationDegrees = (orientation.degreesFromOrientation() + degrees) % 360
    exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientationDegrees.orientationFromDegrees())
    exif.saveAttributes()
}


fun Context.getResolution(path: String): Point? {
    return if (path.isImageFast() || path.isImageSlow()) {
        path.getImageResolution()
    } else if (path.isVideoFast() || path.isVideoSlow()) {
        getVideoResolution(path)
    } else {
        null
    }
}

fun Context.getVideoResolution(path: String): Point? {
    var point = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
        Point(width, height)
    } catch (ignored: Exception) {
        null
    }

    if (point == null && path.startsWith("content://", true)) {
        try {
            val fd = contentResolver.openFileDescriptor(Uri.parse(path), "r")?.fileDescriptor
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(fd)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            point = Point(width, height)
        } catch (ignored: Exception) {
        }
    }

    return point
}

