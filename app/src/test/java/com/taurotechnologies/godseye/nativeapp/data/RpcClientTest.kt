package com.taurotechnologies.godseye.nativeapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RpcClientTest {
    /** Captured from the live public endpoint on 2026-11-07 (`?public=1`). Shape only: no markets were published. */
    private val livePredictionEmpty = """{"markets":[],"fetchedAt":0,"dataAvailable":false}"""

    private val endpoint = RpcEndpoint.parse("prediction/v1/list-prediction-markets?public=1")!!

    private class Recorder {
        val urls = CopyOnWriteArrayList<String>()
        val bodies = CopyOnWriteArrayList<String>()
        val userAgents = CopyOnWriteArrayList<String?>()
    }

    private fun respond(request: okhttp3.Request, code: Int, body: String, headers: Map<String, String> = emptyMap()): Response {
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
        headers.forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }

    private fun client(recorder: Recorder, handler: (okhttp3.Request) -> Response): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                recorder.urls += chain.request().url.toString()
                recorder.bodies += chain.request().body?.let { body ->
                    okio.Buffer().also { body.writeTo(it) }.readUtf8()
                }.orEmpty()
                recorder.userAgents += chain.request().header("User-Agent")
                handler(chain.request())
            })
            .build()

    private fun rpc(client: OkHttpClient, timeoutSeconds: Long = 25) = RpcClient(
        http = client,
        publicBaseUrl = "https://gods-eye.app",
        sessionBaseUrl = "https://explorer.gods-eye.app",
        timeoutSeconds = timeoutSeconds,
    )

    @Test
    fun parsesEndpointsWithQueryAndRejectsUnsafeOnes() {
        val parsed = RpcEndpoint.parse("prediction/v1/list-prediction-markets?public=1&page_size=50")!!
        assertEquals("prediction/v1/list-prediction-markets", parsed.path)
        assertEquals(mapOf("public" to "1", "page_size" to "50"), parsed.query)

        assertNull(RpcEndpoint.parse("https://evil.example/v1/x"))
        assertNull(RpcEndpoint.parse("../etc/passwd"))
        assertNull(RpcEndpoint.parse("prediction/v1/list-prediction-markets/extra"))
        assertNull(RpcEndpoint.parse("prediction/v1/list-prediction-markets?public"))
        assertNull(RpcEndpoint.parse("prediction/v1/list-prediction-markets?a=1&a=2"))
        assertNull(RpcEndpoint.parse("prediction/v1/list-prediction-markets?q=a%20b"))
        assertNull(RpcEndpoint.parse(""))
    }

    @Test
    fun publicCallBuildsApiUrlWithQueryAndReturnsLiveShape() = runBlocking {
        val recorder = Recorder()
        val json = rpc(client(recorder) { respond(it, 200, livePredictionEmpty) }).getJson(endpoint)

        assertEquals("https://gods-eye.app/api/prediction/v1/list-prediction-markets?public=1", recorder.urls.single())
        assertEquals("GodsEyeNative/1.0 (https://www.worldmonitor.app)", recorder.userAgents.single())
        assertEquals(listOf("markets", "fetchedAt", "dataAvailable"), json.jsonObject.keys.toList())
        assertEquals("false", json.jsonObject["dataAvailable"]!!.jsonPrimitive.content)
    }

    @Serializable
    private data class PredictionList(val markets: List<Market> = emptyList(), val dataAvailable: Boolean = false)

    @Serializable
    private data class Market(val id: String, val title: String, val yesPrice: Double, val closesAt: Long = 0)

    @Test
    fun typedGetDecodesWithIgnoredUnknownFields() = runBlocking {
        val body = """{"markets":[{"id":"m1","title":"Test?","yesPrice":0.3,"closesAt":1798761600000,"volume":5,"source":"MARKET_SOURCE_KALSHI"}],"fetchedAt":1,"dataAvailable":true,"newField":1}"""
        val decoded = rpc(client(Recorder()) { respond(it, 200, body) })
            .get(endpoint, PredictionList.serializer())
        assertEquals(1, decoded.markets.size)
        assertEquals("Test?", decoded.markets.single().title)
        assertEquals(0.3, decoded.markets.single().yesPrice, 0.0)
        assertTrue(decoded.dataAvailable)
    }

    @Test
    fun typedGetWrapsShapeMismatchAsParseError() = runBlocking {
        val body = """{"markets":[{"id":"m1"}]}"""
        try {
            rpc(client(Recorder()) { respond(it, 200, body) }).get(endpoint, PredictionList.serializer())
            fail("expected ParseError")
        } catch (error: RpcException.ParseError) {
            assertTrue(error.detailMessage.contains("prediction/v1/list-prediction-markets"))
        }
    }

    @Test
    fun unauthorizedMapsToSignInMessageWithStatus() = runBlocking {
        try {
            rpc(client(Recorder()) { respond(it, 401, """{"error":"API key required"}""") })
                .getJson(RpcEndpoint.parse("prediction/v1/list-prediction-markets")!!)
            fail("expected HttpError")
        } catch (error: RpcException.HttpError) {
            assertEquals(401, error.statusCode)
            assertEquals("Sign in required for this data (HTTP 401).", error.displayMessage)
            assertTrue(error.detailMessage.contains("fetch"))
        }
    }

    @Test
    fun rateLimitKeepsRetryAfter() = runBlocking {
        try {
            rpc(client(Recorder()) { respond(it, 429, "{}", mapOf("Retry-After" to "30")) }).getJson(endpoint)
            fail("expected HttpError")
        } catch (error: RpcException.HttpError) {
            assertEquals("Rate limited (HTTP 429). Retry after 30s.", error.displayMessage)
            assertTrue(error.detailMessage.endsWith("(Retry-After: 30)"))
        }
    }

    @Test
    fun upstreamFailureIsNamedAsUpstream() = runBlocking {
        try {
            rpc(client(Recorder()) { respond(it, 503, "{}") }).getJson(endpoint)
            fail("expected HttpError")
        } catch (error: RpcException.HttpError) {
            assertEquals("GodsEye upstream error (HTTP 503). Retry.", error.displayMessage)
        }
    }

    @Test
    fun socketTimeoutMapsToTimeoutWithLimit() = runBlocking {
        val client = client(Recorder()) { throw SocketTimeoutException("read timed out") }
        try {
            rpc(client, timeoutSeconds = 25).getJson(endpoint)
            fail("expected Timeout")
        } catch (error: RpcException.Timeout) {
            assertEquals("GodsEye request timed out (25s limit). Retry.", error.displayMessage)
            assertTrue(error.cause is SocketTimeoutException)
        }
    }

    @Test
    fun connectionFailureMapsToNetworkError() = runBlocking {
        val client = client(Recorder()) { throw IOException("Unable to resolve host") }
        try {
            rpc(client).getJson(endpoint)
            fail("expected Network")
        } catch (error: RpcException.Network) {
            assertEquals("Network error (Unable to resolve host). Retry.", error.displayMessage)
        }
    }

    @Test
    fun blankAndNonJsonBodiesAreParseErrors() = runBlocking {
        for (body in listOf("", "   ", "<html>oops</html>")) {
            try {
                rpc(client(Recorder()) { respond(it, 200, body) }).getJson(endpoint)
                fail("expected ParseError for '$body'")
            } catch (error: RpcException.ParseError) {
                assertEquals("Data unreadable. Retry.", error.displayMessage)
            }
        }
    }

    @Test
    fun cancellingTheCoroutineCancelsTheOkHttpCall() = runBlocking {
        val started = CountDownLatch(1)
        val observedCancel = CountDownLatch(1)
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                started.countDown()
                // Poll until the call is cancelled, giving up after 5s so the test cannot hang.
                val deadline = System.currentTimeMillis() + 5_000
                while (!chain.call().isCanceled() && System.currentTimeMillis() < deadline) Thread.sleep(10)
                if (chain.call().isCanceled()) observedCancel.countDown()
                throw IOException("Canceled")
            })
            .build()
        val job = launch(Dispatchers.Default) { rpc(client).getJson(endpoint) }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertTrue("the OkHttp call should have been cancelled", observedCancel.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun cancelledCallPropagatesCancellationNotRpcException() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Thread.sleep(3_000)
                respond(chain.request(), 200, livePredictionEmpty)
            })
            .build()
        val started = System.nanoTime()
        try {
            withTimeout(150) { rpc(client).getJson(endpoint) }
            fail("expected cancellation")
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            // Expected: the wrapper cancelled the call and returned without waiting for the 3s interceptor.
        }
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2_500)
    }

    @Test
    fun sessionExchangeRunsBeforeFetch() = runBlocking {
        val recorder = Recorder()
        val client = client(recorder) { request ->
            if (request.url.encodedPath == "/godseye/session") respond(request, 200, """{"authenticated":true}""")
            else respond(request, 200, livePredictionEmpty)
        }
        val auth = RpcAuth.Session("test-token-123")
        assertEquals("RpcAuth.Session", auth.toString())
        try {
            rpc(client).getJson(endpoint, auth)
            fail("expected MissingSessionCookie because the fixture client cannot set cookies")
        } catch (error: RpcException.MissingSessionCookie) {
            assertTrue(error.detailMessage.contains("prediction/v1/list-prediction-markets"))
        }
        assertEquals(listOf("https://explorer.gods-eye.app/godseye/session"), recorder.urls)
        assertTrue(recorder.bodies.single().contains("\"accessToken\":\"test-token-123\""))
    }

    @Test
    fun sessionRejectionStopsBeforeFetch() = runBlocking {
        val recorder = Recorder()
        val client = client(recorder) { respond(it, 401, """{"error":"Sign in to GodsEye"}""") }
        try {
            rpc(client).getJson(endpoint, RpcAuth.Session("expired"))
            fail("expected HttpError at session stage")
        } catch (error: RpcException.HttpError) {
            assertEquals(401, error.statusCode)
            assertTrue(error.detailMessage.contains("session"))
        }
        assertEquals(1, recorder.urls.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun sessionAuthRejectsBlankToken() {
        RpcAuth.Session("  ")
    }

    @Test
    fun equalEndpointsAreEqualKeysForCaching() {
        assertEquals(RpcEndpoint.parse("a/v1/b?x=1&y=2"), RpcEndpoint.parse("a/v1/b?x=1&y=2"))
        assertEquals("1", RpcEndpoint.parse("a/v1/b?x=1")!!.query["x"])
    }
}
