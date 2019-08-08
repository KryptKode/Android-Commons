package com.kryptkode.common.extensions

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.TransactionTooLargeException
import com.kryptkode.common.R
import com.kryptkode.common.helpers.REQUEST_SET_AS
import com.kryptkode.common.helpers.ensureBackgroundThread
import java.util.*


fun Activity.isAppInstalledOnSDCard(): Boolean = try {
    val applicationInfo = packageManager.getPackageInfo(packageName, 0).applicationInfo
    (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE
} catch (e: Exception) {
    false
}




fun Activity.launchViewIntent(id: Int) = launchViewIntent(getString(id))

fun Activity.launchViewIntent(url: String) {
    ensureBackgroundThread {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}

fun Activity.sharePathIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, newUri)
            type = getUriMimeType(path, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                if (resolveActivity(packageManager) != null) {
                    startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                } else {
                    toast(R.string.no_app_found)
                }
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    showErrorToast(e)
                }
            }
        }
    }
}

fun Activity.sharePath(path: String, applicationId: String) {
    sharePathIntent(path, applicationId)
}

fun Activity.sharePaths(paths: ArrayList<String>, applicationId: String) {
    sharePathsIntent(paths, applicationId)
}

fun Activity.shareMediumPath(path: String, applicationId: String) {
    sharePath(path, applicationId)
}

fun Activity.shareMediaPaths(paths: ArrayList<String>, applicationId: String) {
    sharePaths(paths, applicationId)
}

fun Activity.sharePathsIntent(paths: ArrayList<String>, applicationId: String) {
    ensureBackgroundThread {
        if (paths.size == 1) {
            sharePathIntent(paths.first(), applicationId)
        } else {
            val uriPaths = ArrayList<String>()
            val newUris = paths.map {
                val uri = getFinalUriFromPath(it, applicationId) ?: return@ensureBackgroundThread
                uriPaths.add(uri.path ?: "")
                uri
            } as ArrayList<Uri>

            var mimeType = uriPaths.getMimeType()
            if (mimeType.isEmpty() || mimeType == "*/*") {
                mimeType = paths.getMimeType()
            }

            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, newUris)

                try {
                    if (resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                    } else {
                        toast(R.string.no_app_found)
                    }
                } catch (e: RuntimeException) {
                    if (e.cause is TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    } else {
                        showErrorToast(e)
                    }
                }
            }
        }
    }
}

fun Activity.setAsIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_ATTACH_DATA
            setDataAndType(newUri, getUriMimeType(path, newUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(this, getString(R.string.set_as))

            if (resolveActivity(packageManager) != null) {
                startActivityForResult(chooser, REQUEST_SET_AS)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}



fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }

    if (uri == null) {
        toast(R.string.unknown_error_occurred)
        return null
    }

    return uri
}

fun Activity.tryGenericMimeType(intent: Intent, mimeType: String, uri: Uri): Boolean {
    var genericMimeType = mimeType.getGenericMimeType()
    if (genericMimeType.isEmpty()) {
        genericMimeType = "*/*"
    }

    intent.setDataAndType(uri, genericMimeType)
    return if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        true
    } else {
        false
    }
}

