package com.mccartykim.yumcodingchallenge.model

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit

// Were this a larger project, I'd use Dagger to inject this elsewhere, but singletons work well for smaller apps
object StockNetworkService: WebSocketListener() {
    private const val PING_INTERVAL = 20000L
    private const val READ_TIMEOUT = 20000L
    private const val GOING_AWAY_WS_CODE = 1001

    val moshi: Moshi = Moshi.Builder().build()
    val observable: Subject<List<StockListing>> = BehaviorSubject.createDefault(emptyList<StockListing>())

    private val wsClient = OkHttpClient.Builder()
        .pingInterval(PING_INTERVAL, TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()

    private val wsRequest = Request.Builder()
        .url("wss://interviews.yum.dev/ws/stocks")
        .build()

    private lateinit var ws: WebSocket

    fun startTickerWebService() {
        ws = wsClient.newWebSocket(wsRequest, this)
        Log.d("WS", "connection started")
    }

    fun pauseTickerWebService() {
        ws.close(GOING_AWAY_WS_CODE, "App onPause")
    }

    fun shutdownTickerWebService() {
        Log.d("WS", "Shutdown called")
        wsClient.dispatcher.executorService.shutdown()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.d("WS", "onOpen called")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        Log.d("WS", "onMessage str called")
        val type = Types.newParameterizedType(List::class.java, StockListing::class.java)
        val jsonAdapter = moshi.adapter<List<StockListing>>(type)
        val stockListings = jsonAdapter.fromJson(text)
        Log.d("STOCKTICKER", stockListings.toString())
        observable.onNext(stockListings)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        Log.d("WS", "Closed because of $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        Log.e("WS", "onFailure called")
        Log.e("WS", "${t.message}")
        Log.e("WS", response?.body.toString())
    }

    private val DETAIL_URL = "https://interviews.yum.dev/api/stocks/".toHttpUrl()

    val detailSubject: PublishSubject<Detail> = PublishSubject.create()

    private val okHttpClient = OkHttpClient.Builder().build()

    // Post details asynchronously to RxJava
    fun getDetails(id: CharSequence) {
        val request = Request.Builder()
            .url(buildRequestDetailsUrl(id))
            .build()

        okHttpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DETAIL_REQUEST", "Failure: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val status = response.message
                response.code
                Log.d("DETAIL_REQUEST", status)
                val detail: Detail = response.use {
                    when {
                        !it.isSuccessful -> NetworkError(status)
                        else -> it.body?.use { StockDetailJsonAdapter(moshi).fromJson(it.source()) }
                            ?: NetworkError("Invalid response from server despite successful code")
                    }
                }
                Log.d("DETAIL_REQUEST", detail.toString())
                detailSubject.onNext(detail)
            }
        })
    }

    // In production, I'd consider the factor of us using a string we've downloaded for this query,
    // although a WSS query on a server we own would stay safe so long as we've kept the server secure
    private fun buildRequestDetailsUrl(id:CharSequence) = DETAIL_URL.newBuilder().addPathSegment(id.toString()).build()
}
