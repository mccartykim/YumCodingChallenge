package com.mccartykim.yumcodingchallenge.model

import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.DiffUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiFunction


class StockTickerDisplayListingDataStore(
    predicateObservable: Observable<(StockListing) -> Boolean>,
    stockListingUpdatesObservable: Observable<List<StockListing>> = StockNetworkService.observable
) {
    companion object {
        private val noFilter: (StockListing) -> Boolean = { true }

        private val emptyDiffedStockUpdate =
            DiffedStockUpdate(
                displayStocks = emptyList(),
                diff = DiffUtil.calculateDiff(StockDiffCallback(emptyList(), emptyList()))
            )
    }
    @VisibleForTesting
    val observeStockWithPriceDifferenceAndSort: Observable<List<StockListing>> =
        stockListingUpdatesObservable
            .startWithItem(emptyList()) // Add default value so we immediately get results even if they have no price diff
            .buffer(2, 1) // Rolling window of two stocks.
            .map { (previous, current) ->
                current.mapNotNull { currentListing -> previous.find { it.id == currentListing.id }?.let { currentListing to it } }
                    .forEach { (current, previous) -> current.priceDiff = current.price - previous.price }
                current.sortedBy { it.id }
            }

    @VisibleForTesting
    val observeNewestPredicate: Observable<(StockListing) -> Boolean> =
        predicateObservable
            .startWithItem(noFilter)
            .map { it }

    @VisibleForTesting
    val filteredStocks: Observable<List<StockListing>> = Observable.combineLatest(observeStockWithPriceDifferenceAndSort, observeNewestPredicate, BiFunction {
        stocks, predicate -> stocks.filter(predicate)
    })

    val observeDiffFilteredStocks: Observable<DiffedStockUpdate> = filteredStocks.scan(
        emptyDiffedStockUpdate
    ) { lastUpdate, newFilteredStocks ->
        DiffedStockUpdate(newFilteredStocks,
            DiffUtil.calculateDiff(StockDiffCallback(oldList = lastUpdate.displayStocks, newList = newFilteredStocks)))
    }
}

class DiffedStockUpdate(val displayStocks: List<StockListing>, val diff: DiffUtil.DiffResult)
