package com.mccartykim.yumcodingchallenge.ui.main

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.DiffUtil
import com.mccartykim.yumcodingchallenge.model.StockListing
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiFunction

sealed class StockTickerDataUpdate
class NewPredicate(val predicate: (StockListing) -> Boolean): StockTickerDataUpdate()
val clearPredicate = NewPredicate { true }
class NewStockListings(val newUnfilteredStocks: List<StockListing>): StockTickerDataUpdate()
val emptyStockListings: NewStockListings = NewStockListings(emptyList())

class DiffedStockUpdate(val displayStocks: List<StockListing>, val diff: DiffUtil.DiffResult)
val emptyDiffedStockUpdate = DiffedStockUpdate(emptyList(), DiffUtil.calculateDiff(StockDiffCallback(emptyList(), emptyList())))



class StockTickerDisplayListingDataStore(stockListingPublisher: Observable<StockTickerDataUpdate>) {
    // Unpack the observable and add default values
    @VisibleForTesting
    val observeUnfilteredStocks: Observable<List<StockListing>> =  stockListingPublisher.ofType(NewStockListings::class.java)
        .startWithItem(emptyStockListings).map { it.newUnfilteredStocks.sortedBy { it.id } }
    // TODO move the alphabetizing upstream...
    @VisibleForTesting
    val observeNewestPredicate = stockListingPublisher.ofType(NewPredicate::class.java).startWithItem(clearPredicate).map { it.predicate }

    @VisibleForTesting
    val filteredStocks: Observable<List<StockListing>> = Observable.combineLatest(observeUnfilteredStocks, observeNewestPredicate, BiFunction {
        stocks, predicate -> stocks.filter(predicate)
    })

    val observeDiffFilteredStocks: Observable<DiffedStockUpdate> = filteredStocks.scan(emptyDiffedStockUpdate) {
        lastUpdate, newFilteredStocks ->
            Log.d("OBSERVABLE", "${lastUpdate.displayStocks}, ${newFilteredStocks}")
            Log.d("diff", lastUpdate.diff.toString())
        DiffedStockUpdate(
                newFilteredStocks,
                DiffUtil.calculateDiff(StockDiffCallback(oldList = lastUpdate.displayStocks, newList = newFilteredStocks))
            )
    }
}
