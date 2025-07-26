@file:OptIn(ExperimentalTime::class)

import kotlinx.coroutines.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

val prettyJson = Json { prettyPrint = true }

@Serializable
data class Person(val name: String, val age: Int)

val path = Path("temp.txt")

context(scope:CoroutineScope)
suspend fun doSomething() = withContext(Dispatchers.IO) {

    // Launch an async job to write to a file
    val fileWriteDeferred = scope.async {
        log("LAUNCH: WriteFile job ...")
        val randomBytes = Random.nextBytes(256)
        SystemFileSystem.sink(path, append = true).buffered().use {
            val bytes = randomBytes.toHexString().chunked(128).joinToString("\n")
            it.writeString("-----\nBytes\n-----\n${bytes}\n")
        }
        log("FINISHED: WriteFile job")
    }

    // Launch an async job to make a JsonObject
    val jsonDeferred = scope.async {
        log("LAUNCH: JSON job ...")
        val json = buildJsonObject {
            put("key1", 1)
            put("key2", 2.0f)
            put("key3", "Hello World!")
            put("key4", true)
            put("key5", Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString())
            put("key6", Random.nextBytes(16).toHexString())
            put(
                "key7",
                Json.encodeToJsonElement(Person("John", 30))
            )
        }
        log("FINISHED: JSON job")
        return@async json
    }

    // Await them all
    fileWriteDeferred.await()
    val json = jsonDeferred.await()

    log("AWAITED: both jobs")
    log("JSON job result:\n${prettyJson.encodeToString(json)}")

    // Write JSON to a file
    SystemFileSystem.sink(path, append = true).buffered().use {
        it.writeString("-----\nJson\n-----\n${json}\n")
    }
}

suspend fun log(msg: String) = withContext(Dispatchers.Default) {
    val now = Clock.System.now()
    val localTime = now.toLocalDateTime(TimeZone.UTC).time
    val timeString = buildString {
        append("${localTime.hour.toString().padStart(2, '0')}:")
        append("${localTime.minute.toString().padStart(2, '0')}:")
        append("${localTime.second.toString().padStart(2, '0')}.")
        append(localTime.nanosecond / 1_000_000).toString().padStart(3, '0')
    }
    println("[$timeString]: $msg")
}

fun main() = runBlocking(Dispatchers.Default) {
	log("--------START--------")
    doSomething()
    log("---------END---------")
}