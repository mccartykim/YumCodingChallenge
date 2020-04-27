package com.mccartykim.yumcodingchallenge.model

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import okhttp3.*
import java.util.concurrent.TimeUnit

class StockWebSocketListener: WebSocketListener() {
    val moshi = Moshi.Builder().build()
    val observable: Subject<List<StockListing>> = PublishSubject.create()

    val client = OkHttpClient.Builder()
        .pingInterval(20000, TimeUnit.MILLISECONDS)
        .readTimeout(30000, TimeUnit.MILLISECONDS)
        .build()

    val request = Request.Builder()
        .url("wss://interviews.yum.dev/ws/stocks")
        .build()

    lateinit var ws: WebSocket

    fun start() {
        ws = client.newWebSocket(request, this)
        Log.d("WS", "connection started")
    }

    fun pause() {
        ws.close(1001, "App onPause")
    }

    fun shutdown() {
        Log.d("WS", "Shutdown called")
        client.dispatcher.executorService.shutdown()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.d("WS", "onopen called")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        Log.d("WS", "onmessage str called")
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
        Log.e("WS", "onfailure called")
        Log.e("WS", "${t.message}")
        Log.e("WS", "${response?.body.toString()}")
    }
}