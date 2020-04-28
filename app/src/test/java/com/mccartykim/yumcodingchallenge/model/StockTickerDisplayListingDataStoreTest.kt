package com.mccartykim.yumcodingchallenge.model

import io.mockk.spyk
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StockTickerDisplayListingDataStoreTest {
    private val companiesOne = listOf(
        StockListing(id = "FOO", name = "Foo", price = 900.00, companyType = listOf("Tech", "Fake"), priceDiff = null),
        StockListing(id = "TEST", name = "TEST", price = 900.00, companyType = listOf("Tech", "Fake"), priceDiff = null),
        StockListing(id = "MAR", name = "Mars", price = 900.00, companyType = listOf("Tech", "Fake"), priceDiff = null)
    )

    private val companiesTwo = listOf(
        StockListing(id = "FOO", name = "Foo", price = 1000.00, companyType = listOf("Tech", "Fake"), priceDiff = 15.00),
        StockListing(id = "TEST", name = "Test", price = 800.00, companyType = listOf("Tech", "Fake"), priceDiff = 15.00),
        StockListing(id = "MAR", name = "Mars", price = 500.00, companyType = listOf("Tech", "Fake"), priceDiff = 15.00)
    )

    private val predicateSubject: BehaviorSubject<(StockListing) -> Boolean> = BehaviorSubject.create()
    private val stockListSubject: BehaviorSubject<List<StockListing>> = BehaviorSubject.createDefault(companiesOne)

    @Test
    fun `getObserveStockWithPriceDifferenceAndSort - should sort list by ids, and not add price diff from just one list`() {
        val dataStore = StockTickerDisplayListingDataStore(predicateSubject, stockListSubject)
        val nopConsumer = spyk({ t: List<StockListing> -> /* nop */ })
        dataStore.observeStockWithPriceDifferenceAndSort.subscribe(nopConsumer)

        val results = mutableListOf<List<StockListing>>()
        verify { nopConsumer(capture(results)) }

        assertThat(results)
            .hasSize(1)
        assertThat(results.first().map { it.id })
            .isSorted()
        assertThat(results.first().map { it.priceDiff })
            .allMatch { it == null }
    }

    @Test
    fun `getObserveStockWithPriceDifferenceAndSort - should add price diffs with two iterations of a company`() {
        val dataStore = StockTickerDisplayListingDataStore(predicateSubject, stockListSubject)
        val nopConsumer = spyk({ t: List<StockListing> -> /* nop */ })
        dataStore.observeStockWithPriceDifferenceAndSort.subscribe(nopConsumer)
        stockListSubject.onNext(companiesTwo)

        val results = mutableListOf<List<StockListing>>()
        verify { nopConsumer(capture(results)) }

        assertThat(results)
            .hasSize(2)
        assertThat(results.last().map { it.priceDiff })
            .allMatch { it != null }
            .containsExactly(100.0, -400.0, -100.0)
    }

    @Test
    fun `getObserveNewestPredicate - Has default always-true predicate`() {
        val dataStore = StockTickerDisplayListingDataStore(Observable.empty(), stockListSubject)
        val nopConsumer = spyk({ t: (StockListing) -> Boolean -> /* nop */ })
        dataStore.observeNewestPredicate.subscribe(nopConsumer)

        stockListSubject.onNext(companiesTwo)
        val results = mutableListOf<(StockListing) -> Boolean>()
        verify { nopConsumer(capture(results)) }

        assertThat(results)
            .hasSize(1)
        val default = StockTickerDisplayListingDataStore.noFilter
        assertThat(default === results.first()).isTrue()
    }

    @Test
    fun `getObserveNewestPredicate - Returns latest predicate from the observable after the first`() {
        val mockFilter: (StockListing) -> Boolean = { false }
        val dataStore = StockTickerDisplayListingDataStore(Observable.just(mockFilter), stockListSubject)
        val nopConsumer = spyk({ t: (StockListing) -> Boolean -> /* nop */ })
        dataStore.observeNewestPredicate.subscribe(nopConsumer)

        stockListSubject.onNext(companiesTwo)
        val results = mutableListOf<(StockListing) -> Boolean>()
        verify { nopConsumer(capture(results)) }

        assertThat(results)
            .hasSize(2)
        val default = StockTickerDisplayListingDataStore.noFilter
        assertThat(default === results.first()).isTrue()
        assertThat(mockFilter === results.last()).isTrue()
    }

    @Test
    fun `getFilteredStocks applies latest predicate to most recent list`() {
        val dataStore = StockTickerDisplayListingDataStore(
            Observable.fromIterable(listOf({ it: StockListing -> it.id.startsWith("F") }, { it: StockListing -> it.id.startsWith("T") }))
            , stockListSubject)
        val nopConsumer = spyk({ t: List<StockListing> -> /* nop */ })
        dataStore.filteredStocks.subscribe(nopConsumer)

        val results = mutableListOf<List<StockListing>>()
        verify { nopConsumer(capture(results)) }

        assertThat(results)
            .hasSize(3)
        // first predicate is no filter, should have all the companies
        assertThat(results.first())
            .hasSize(3)
        // second: Starts with F
        assertThat(results[1])
            .hasSize(1)
            .first()
            .extracting("id")
            .isEqualTo("FOO")
        // third: Starts with T
        assertThat(results[2])
            .hasSize(1)
            .first()
            .extracting("id")
            .isEqualTo("TEST")
    }

    @Test
    fun `getFilteredStocks applies most recent predicate to latest list`() {
        val dataStore = StockTickerDisplayListingDataStore(
            Observable.fromIterable(listOf { it: StockListing -> it.id.startsWith("F") }),
                stockListSubject)
        val nopConsumer = spyk({ t: List<StockListing> -> /* nop */ })
        dataStore.filteredStocks.subscribe(nopConsumer)

        stockListSubject.onNext(companiesTwo)
        val results = mutableListOf<List<StockListing>>()
        verify { nopConsumer(capture(results)) }

        assertThat(results)
            .hasSize(3)
        // first predicate is no filter, should have all the companies
        assertThat(results.first())
            .hasSize(3)
        // second: Starts with F, first set of companies
        assertThat(results[1])
            .hasSize(1)
            .first()
            .extracting("id")
            .isEqualTo("FOO")
        assertThat(results[1])
            .hasSize(1)
            .first()
            .extracting("priceDiff")
            .isEqualTo(null)

        // third: starts with F, new data
        assertThat(results[2])
            .hasSize(1)
            .first()
            .extracting("id")
            .isEqualTo("FOO")
        assertThat(results[2])
            .hasSize(1)
            .first()
            .extracting("priceDiff")
            .isEqualTo(100.0)
    }

    @Test
    fun `getObserveDiffStocks should return at least as many results as stock updates going in`() {
        val dataStore = StockTickerDisplayListingDataStore(
            Observable.fromIterable(listOf { it: StockListing -> it.id.startsWith("F") }),
            stockListSubject)
        val nopConsumer = spyk({ t: DiffedStockUpdate -> /* nop */ })
        dataStore.observeDiffFilteredStocks.subscribe(nopConsumer)

        stockListSubject.onNext(companiesTwo)
        val results = mutableListOf<DiffedStockUpdate>()
        verify { nopConsumer(capture(results)) }

        assertThat(results)
            .hasSize(3)
        // initial result, all stocks because the default filter allows everything
        assertThat(results.first().displayStocks)
            .hasSize(3)
        // second: Starts with F, first set of companies
        assertThat(results[1].displayStocks)
            .hasSize(1)
            .first()
            .extracting("id")
            .isEqualTo("FOO")
        // third: starts with F, new data
        assertThat(results[2].displayStocks)
            .hasSize(1)
            .first()
            .extracting("id")
            .isEqualTo("FOO")
        assertThat(results[2].displayStocks)
            .hasSize(1)
            .first()
            .extracting("priceDiff")
            .isEqualTo(100.0)
    }
}