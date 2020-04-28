package com.mccartykim.yumcodingchallenge.ui.main

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import com.mccartykim.yumcodingchallenge.model.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*

class MainViewModel : ViewModel() {
    companion object {
        // queries are compared in uppercase, even though the app encourages the user to prefix "tag:"
        const val tag_prefix = "TAG:"

        @VisibleForTesting
        fun <T>Collection<(T) -> Boolean>.chainPredicates(): (T) -> Boolean {
            return this.fold(
                initial = { element: T -> true },
                operation = { f, g -> { element:T -> f(element) && g(element) } }
            )
        }
    }

    val filterQueryBindingSubject: PublishSubject<String> = PublishSubject.create()
    val stockTickerBindingSubject: PublishSubject<StockTickerState> = PublishSubject.create()
    private val queryPredicateSubject: PublishSubject<(StockListing) -> Boolean> = PublishSubject.create()
    val viewDetailsSubject: PublishSubject<Unit> = PublishSubject.create()

    private val disposable = CompositeDisposable().apply {
        addAll(
            filterQueryBindingSubject
                .subscribe(onChangedQueryConsumer),
            stockTickerBindingSubject
                .distinctUntilChanged()
                .subscribe {
                    when (it) {
                        PauseTicker -> stockTickerPause()
                        ResumeTicker -> stockTickerStartOrResume()
                        is LoadDetails -> {
                            viewDetailsSubject.onNext(Unit)
                        }
                    }
                },
            stockTickerBindingSubject.ofType(LoadDetails::class.java).observeOn(Schedulers.io()).subscribe {
                StockNetworkService.getDetails(it.id)
            }
        )
    }

    val diffFilteredStocks by lazy {
        StockTickerDisplayListingDataStore(queryPredicateSubject).observeDiffFilteredStocks
    }

    @VisibleForTesting
    val onChangedQueryConsumer: Consumer<String>
        get() = Consumer<String> { rawStr ->
            val queryItems = rawStr.toUpperCase(Locale.getDefault()).split(" ")

            val stockPredicate = queryItems.map {
                when {
                    it.contains(tag_prefix) -> filterStockByCompanyType(it.substringAfter(tag_prefix))
                    else -> filterStockByPrefix(it)
                }
            }.chainPredicates()
            queryPredicateSubject.onNext(stockPredicate)
        }

    @VisibleForTesting
    fun filterStockByPrefix(prefix: String): (StockListing) -> Boolean =
        { listing: StockListing ->
            require(prefix == prefix.toUpperCase(Locale.getDefault()))
            listing.id.startsWith(prefix) }

    @VisibleForTesting
    fun filterStockByCompanyType(companyType: String): (StockListing) -> Boolean =
        { listing: StockListing ->
            require(companyType == companyType.toUpperCase(Locale.getDefault()))
            listing.companyType.map { it.toUpperCase(Locale.getDefault()) }.contains(companyType) }


    private fun stockTickerStartOrResume() {
        StockNetworkService.startTickerWebService()
    }

    private fun stockTickerPause() {
        StockNetworkService.pauseTickerWebService()
    }

    override fun onCleared() {
        super.onCleared()
        StockNetworkService.pauseTickerWebService()
        StockNetworkService.shutdownTickerWebService()
        disposable.clear()
    }
}

sealed class StockTickerState
object PauseTicker: StockTickerState()
object ResumeTicker: StockTickerState()
class LoadDetails(val id: CharSequence): StockTickerState()

