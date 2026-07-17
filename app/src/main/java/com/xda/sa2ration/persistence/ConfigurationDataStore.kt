package com.xda.sa2ration.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private object JsonStringSerializer : Serializer<String> {
    override val defaultValue: String = ""

    override suspend fun readFrom(input: InputStream): String =
        input.readBytes().toString(Charsets.UTF_8)

    override suspend fun writeTo(t: String, output: OutputStream) {
        output.write(t.toByteArray(Charsets.UTF_8))
    }
}

/** Small Java-friendly bridge around transactional AndroidX DataStore. */
class ConfigurationDataStore private constructor(context: Context) {
    private val dataStore: DataStore<String> = DataStoreFactory.create(
        serializer = JsonStringSerializer,
        produceFile = {
            File(context.applicationContext.filesDir, "datastore/$FILE_NAME").also {
                it.parentFile?.mkdirs()
            }
        }
    )

    fun readBlocking(): String = runBlocking { dataStore.data.first() }

    fun writeBlocking(json: String) = runBlocking {
        dataStore.updateData { json }
    }

    companion object {
        private const val FILE_NAME = "sa2ration_state.json"
        @Volatile private var instance: ConfigurationDataStore? = null

        @JvmStatic
        fun getInstance(context: Context): ConfigurationDataStore =
            instance ?: synchronized(this) {
                instance ?: ConfigurationDataStore(context).also { instance = it }
            }
    }
}
