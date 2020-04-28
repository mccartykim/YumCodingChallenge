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
        val tag_prefix = "tag:"
    }

    val filterQueryBindingSubject: PublishSubject<String> = PublishSubject.create()
    val stockTickerBindingSubject: PublishSubject<StockTickerState> = PublishSubject.create()
    val queryPredicateSubject: PublishSubject<(StockListing) -> Boolean> = PublishSubject.create()
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


    // perhaps move adapter out of this, since it's sort of a view? Sort of a view model? Bleh
    val diffFilteredStocks by lazy {
        StockTickerDisplayListingDataStore(queryPredicateSubject).observeDiffFilteredStocks
    }

    private val onChangedQueryConsumer: Consumer<String>
        get() = Consumer<String> {
            val queryItems = it.toUpperCase(Locale.getDefault()).split(" ")

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
        { listing: StockListing -> listing.id.startsWith(prefix) }

    @VisibleForTesting
    fun filterStockByCompanyType(companyType: String): (StockListing) -> Boolean =
        { listing: StockListing -> listing.companyType.map { it.toUpperCase(Locale.getDefault()) }.contains(companyType) }

    @VisibleForTesting
    fun <T>Collection<(T) -> Boolean>.chainPredicates(): (T) -> Boolean {
        return this.fold(
            initial = {_: T -> true},
            operation = { f, g -> { element:T -> f(element) && g(element) } }
        )
    }

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

