package com.mccartykim.yumcodingchallenge.ui.ticker

import com.mccartykim.yumcodingchallenge.model.StockListing
import com.mccartykim.yumcodingchallenge.ui.ticker.StockTickerViewModel.Companion.chainPredicates
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test


class StockTickerViewModelTest {

    private val fakeCompany = StockListing(id = "FOO", name = "Foo", price = 900.00, companyType = listOf("Tech", "Fake"), priceDiff = 15.00)

    @Test
    fun `filterStockByPrefix - Should create stock filter from strings based on prefix`() {
        // arrange
        val model = StockTickerViewModel()

        // act
        val prefix = model.filterStockByPrefix("F")

        // assert
        assertThat(prefix(fakeCompany)).isTrue()
    }

    @Test
    fun `filterStockByPrefix - Should not match characters in the middle`() {
        // arrange
        val model = StockTickerViewModel()

        // act
        val prefix = model.filterStockByPrefix("O")

        // assert
        assertThat(prefix(fakeCompany)).isFalse()
    }


    @Test
    fun `filterStockByCompanyType - Should match company tags`() {
        // arrange
        val model = StockTickerViewModel()

        // act
        val tag = model.filterStockByCompanyType("TECH")

        // assert
        assertThat(tag(fakeCompany)).isTrue()
    }

    @Test
    fun `filterStockByCompanyType - Should match company tags other than first position`() {
        // arrange
        val model = StockTickerViewModel()

        // act
        val tag = model.filterStockByCompanyType("FAKE")

        // assert
        assertThat(tag(fakeCompany)).isTrue()
    }

    @Test
    fun `filterStockByCompanyType - Should not match company tags not in the description`() {
        // arrange
        val model = StockTickerViewModel()

        // act
        val tag = model.filterStockByCompanyType("REAL")

        // assert
        assertThat(tag(fakeCompany)).isFalse()
    }

    @Test
    fun `chainPredicates - Empty chain should be true`() {
        // arrange and act
        val emptyComposite = listOf<(Int) -> Boolean>().chainPredicates()

        // assert
        assertThat(emptyComposite(0)).isTrue()
    }

    @Test
    fun `chainPredicates - Should follow AND logic`() {
        // arrange and act
        val emptyComposite = listOf<(Int) -> Boolean>({true}, {true}, {true}, {false}).chainPredicates()

        // assert
        assertThat(emptyComposite(0)).isFalse()
    }

    @Test
    fun `chainPredicates pt 2 - Should follow AND logic`() {
        // arrange and act
        val emptyComposite = listOf<(Int) -> Boolean>({ it % 2 == 0 }, { it > 3 }).chainPredicates()

        // assert
        assertThat(emptyComposite(4)).isTrue()
        assertThat(emptyComposite(5)).isFalse()
        assertThat(emptyComposite(3)).isFalse()
    }
}