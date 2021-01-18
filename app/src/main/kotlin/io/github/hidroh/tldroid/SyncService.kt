package io.github.hidroh.tldroid

import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.OperationApplicationException
import android.os.RemoteException
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*

class SyncService : JobIntentService() {
  companion object {
    private const val TAG = "SyncService"
    private const val BASE_URL = "https://tldr.sh"
    private const val INDEX_URL = "$BASE_URL/assets/index.json"
    private const val ZIP_URL = "$BASE_URL/assets/tldr.zip"
    const val EXTRA_ASSET_TYPE = "$TAG.EXTRA_ASSET_TYPE"
    const val PREF_LAST_REFRESHED = INDEX_URL
    const val PREF_LAST_ZIPPED = ZIP_URL
    const val PREF_COMMAND_COUNT = "PREF_COMMAND_COUNT"
    const val ASSET_TYPE_INDEX = 0
    const val ASSET_TYPE_ZIP = 1
    private const val JOB_ID = 1000
    fun enqueueWork(context: Context, intent: Intent) {
      enqueueWork(context, SyncService::class.java, JOB_ID, intent)
    }
  }

  override fun onHandleWork(intent: Intent) {
    if (intent.getIntExtra(EXTRA_ASSET_TYPE, ASSET_TYPE_INDEX) == ASSET_TYPE_INDEX) {
      NetworkSync(this).syncIndex()
    } else {
      NetworkSync(this).syncZip()
    }
  }

  class NetworkSync(val context: Context) {

    fun syncIndex() {
      val connection = connect(INDEX_URL) ?: return
      val inputStream = connection.getInputStream()
      if (inputStream != null) {
        val commands = try {
          Moshi.Builder().addLast(KotlinJsonAdapterFactory())
              .build()
              .adapter(Commands::class.java)
              .fromJson(Utils.readUtf8(inputStream))
        } catch (e: IOException) {
          null
        } catch (e: JsonDataException) {
          null
        }
        persist(commands)
        inputStream.close()
      }
      connection.disconnect()
    }

    fun syncZip() {
      val connection = connect(ZIP_URL) ?: return
      val inputStream = connection.getInputStream()
      if (inputStream != null) {
        val sink = File(context.cacheDir, Constants.ZIP_FILENAME).sink().buffer()
        sink.writeAll(inputStream.source())
        sink.close()
        inputStream.close()
      }
      connection.disconnect()
    }

    private fun connect(url: String): NetworkConnection? {
      val sharedPrefs = getDefaultSharedPreferences(context)
      val connection = NetworkConnection(url)
      connection.setIfModifiedSince(sharedPrefs.getLong(url, 0L))
      connection.connect()
      if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
        connection.disconnect()
        return null
      }
      sharedPrefs.edit().putLong(url, connection.getLastModified()).apply()
      return connection
    }

    private fun persist(commands: Commands?) {
      if (commands?.commands!!.isNullOrEmpty()) {
        return
      }
      getDefaultSharedPreferences(context)
          .edit()
          .putInt(PREF_COMMAND_COUNT, commands.commands!!.size)
          .apply()
      val operations = ArrayList<ContentProviderOperation>()
      for (command in commands.commands!!) {
        for (platform in command.platform!!) {
          operations.add(ContentProviderOperation.newInsert(TldrProvider.URI_COMMAND)
                  .withValue(TldrProvider.CommandEntry.COLUMN_NAME, command.name)
                  .withValue(TldrProvider.CommandEntry.COLUMN_PLATFORM, platform)
                  .build())
        }
      }
      val cr = context.contentResolver
      try {
        cr.applyBatch(TldrProvider.AUTHORITY, operations)
        cr.notifyChange(TldrProvider.URI_COMMAND, null)
      } catch (e: RemoteException) {
        // no op
      } catch (e: OperationApplicationException) {
      }

    }

    private class Commands {
      internal var commands: Array<Command>? = null
    }

    private class Command {
      internal var name: String? = null
      internal var platform: Array<String>? = null
    }
  }
}
