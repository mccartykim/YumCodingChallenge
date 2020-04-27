package com.mccartykim.yumcodingchallenge.ui.main

import androidx.lifecycle.ViewModel
import com.mccartykim.yumcodingchallenge.model.StockListing
import com.mccartykim.yumcodingchallenge.model.StockWebSocketListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject

class MainViewModel : ViewModel() {
    companion object {
        val tag_prefix = "tag:"
    }

    private val disposable = CompositeDisposable()
    private val websocketListener = StockWebSocketListener()

    val filterQueryBindingSubject: PublishSubject<ViewState> = PublishSubject.create()
    val stockTickerBindingSubject: PublishSubject<StockTickerState> = PublishSubject.create()
    val stockListingPublisher: PublishSubject<StockTickerDataUpdate> = PublishSubject.create()

    // perhaps move adapter out of this, since it's sort of a view? Sort of a view model? Bleh
    val diffFilteredStocks by lazy {
        StockTickerDisplayListingDataStore(stockListingPublisher).observeDiffFilteredStocks
    }

    private val onChangedQueryConsumer = Consumer<NewQuery> {
        val queryItems = it.query.split(" ")

        clearStockPredicate()

        val stockPredicate = queryItems.map {
            when {
                it.contains(tag_prefix) -> filterStockByCompanyType(it.substringAfter(tag_prefix))
                else -> filterStockByPrefix(it)
            }
        }.chainPredicates()
        stockListingPublisher.onNext(NewPredicate(stockPredicate))
    }

    private val stockTickerStateConsumer = Consumer<StockTickerState> {
        when (it) {
            is PauseTicker -> stockTickerPause()
            is ResumeTicker -> stockTickerStartOrResume()
        }
    }

    // TODO: Move to stockTickerDataUpdate...
    val observeStockWithPriceDifference = websocketListener.observable
        .buffer(2, 1) // Rolling window of two stocks.
        .map { (previous, current) ->
            current.mapNotNull { currentListing -> previous.find { it.id == currentListing.id }?.let { currentListing to it } }
                .forEach { (current, previous) -> current.priceDiff = current.price - previous.price }
            current
        }.map { NewStockListings(it) }

    init {
        disposable.addAll(
            filterQueryBindingSubject.ofType(NewQuery::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onChangedQueryConsumer),
            stockTickerBindingSubject
                .distinctUntilChanged()
                .subscribe(stockTickerStateConsumer),
            observeStockWithPriceDifference.subscribe { stockListingPublisher.onNext(it) }
        )
    }




    private var stockPredicate: (StockListing) -> Boolean = { true }

    private fun filterStockByPrefix(prefix: String) = { listing: StockListing -> listing.id.startsWith(prefix) }

    private fun filterStockByCompanyType(companyType: String) = { listing: StockListing -> listing.companyType.contains(companyType) }

    private fun clearStockPredicate() {
        stockPredicate = { true }
    }

    fun <T>Collection<(T) -> Boolean>.chainPredicates(): (T) -> Boolean {
        return this.reduce { f,g -> { element:T -> f(element) && g(element) } }
    }

    fun stockTickerStartOrResume() {
        websocketListener.start()
    }

    fun stockTickerPause() {
        websocketListener.pause()
    }

    override fun onCleared() {
        super.onCleared()
        websocketListener.pause()
        websocketListener.shutdown()
        disposable.clear()
    }
}

sealed class ViewState
class NewQuery(val query: String): ViewState()

sealed class StockTickerState
object PauseTicker: StockTickerState()
object ResumeTicker: StockTickerState()

