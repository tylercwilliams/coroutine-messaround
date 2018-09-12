import kotlinx.coroutines.experimental.*
import okhttp3.*
import java.net.URL
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) = runBlocking {
    val time = measureTimeMillis {
        val job = networkRequest(HTTPMethod.GET, URL("https://jsonplaceholder.typicode.com/todos/1"))
        val job2 = networkRequest(HTTPMethod.GET, URL("https://jsonplaceholder.typicode.com/todos/1"))

        println("$job\n$job2")
    }

    println(time)

    println("-----------")

    val time2 = measureTimeMillis {
        val job = async(start = CoroutineStart.LAZY) { networkRequest(HTTPMethod.GET, URL("https://jsonplaceholder.typicode.com/todos/1")) }
        val job2 = async(start = CoroutineStart.LAZY) { networkRequest(HTTPMethod.GET, URL("https://jsonplaceholder.typicode.com/todos/1")) }

        job.start()
        job2.start()

        println("${job.await()}\n${job2.await()}")
    }

    println(time2)

    println("-----------")

    val time3 = measureTimeMillis {
        val job = async(start = CoroutineStart.LAZY) { networkRequest(HTTPMethod.GET, URL("https://jsonplceholder.typicode.com/todos/1")) }
        val job2 = async(start = CoroutineStart.LAZY) { networkRequest(HTTPMethod.GET, URL("https://jsonplceholder.typicode.com/todos/1")) }

        job.start()
        job2.start()

        println("${job.await()}\n${job2.await()}")
    }

    println(time3)

    println("-----------")

    val time4 = measureTimeMillis {
        val list = List(1000) { async(start = CoroutineStart.LAZY) { networkRequest(HTTPMethod.GET, URL("https://jsonplaceholder.typicode.com/todos/1")) } }

        list.forEach { it.start() }
        list.forEach { println(it.await()) }
    }

    println(time4)
}

enum class HTTPMethod {
    GET,
    POST
}

//First is the type of header second is the value
typealias Header = Pair<String, String>

sealed class Result
data class Success(val json: String?) : Result()
data class Failure(val error: Exception?) : Result()

suspend fun networkRequest(method: HTTPMethod, url: URL, headers: List<Header>? = null, body: String? = null): Result {
    val client = OkHttpClient()
    return when (method) {
        HTTPMethod.GET -> getRequest(client, url, headers)
        HTTPMethod.POST -> postRequest(client, url, body)
    }
}

private suspend fun getRequest(client: OkHttpClient, url: URL, headers: List<Header>? = null): Result = suspendCoroutine { cont ->
    val request = Request.Builder()
            .apply { headers?.forEach { this.addHeader(it.first, it.second) } }
            .get()
            .url(url)
            .build()

    try {
        val response = client.newCall(request).execute()
        cont.resume(Success(response.body()?.string()))
    } catch (e: Exception) {
        cont.resume(Failure(e))
    }
}

private suspend fun postRequest(client: OkHttpClient, url: URL, body: String?, headers: List<Header>? = null): Result = suspendCoroutine { cont ->
    if (body == null) {
        cont.resumeWithException(IllegalStateException("Body cannot be null"))
        return@suspendCoroutine
    }

    val request = Request.Builder()
            .apply { headers?.forEach { this.addHeader(it.first, it.second) } }
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
            .url(url)
            .build()

    try {
        val response = client.newCall(request).execute()
        cont.resume(Success(response.body()?.string()))
    } catch (e: Exception) {
        cont.resume(Failure(e))
    }
}
