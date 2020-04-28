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
        @VisibleForTesting
        val noFilter: (StockListing) -> Boolean = { true }

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
            .map { pair ->
                // occurs if onComplete is called upstream and the buffer doesn't have two elements yet
                if (pair.size == 1) return@map pair.first()
                val previous = pair.getOrNull(0)
                val current = pair.getOrNull(1)
                current?.forEach { currentListing -> val previousListing = previous?.find { it.id == currentListing.id }
                    if (previousListing != null) {
                        currentListing.priceDiff = currentListing.price - previousListing.price
                    }
                }
                current?.sortedBy { it.id }
            }

    @VisibleForTesting
    val observeNewestPredicate: Observable<(StockListing) -> Boolean> =
        predicateObservable
            .startWithItem(noFilter)

    @VisibleForTesting
    val filteredStocks: Observable<List<StockListing>> = Observable.combineLatest(observeStockWithPriceDifferenceAndSort, observeNewestPredicate, BiFunction {
        stocks, predicate -> stocks.filter(predicate)
    })

    val observeDiffFilteredStocks: Observable<DiffedStockUpdate> =
        filteredStocks.scan(emptyDiffedStockUpdate) { lastUpdate, newFilteredStocks ->
            DiffedStockUpdate(
                newFilteredStocks,
                DiffUtil.calculateDiff(StockDiffCallback(oldList = lastUpdate.displayStocks, newList = newFilteredStocks))
            )
    }.skip(1) // first result is empty because of the initial value for scan
}

class DiffedStockUpdate(val displayStocks: List<StockListing>, val diff: DiffUtil.DiffResult)
