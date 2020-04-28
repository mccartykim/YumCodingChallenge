package com.mccartykim.yumcodingchallenge.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mccartykim.yumcodingchallenge.R
import com.mccartykim.yumcodingchallenge.model.StockListing
import io.reactivex.rxjava3.subjects.PublishSubject

class StockAdapter(val stockTickerBindingSubject: PublishSubject<StockTickerState>) : RecyclerView.Adapter<StockListingViewHolder>() {
    private var stockData: List<StockListing> = emptyList()

    fun updateStocks(newStocks: List<StockListing>, diff: DiffUtil.DiffResult) {
        stockData = newStocks
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockListingViewHolder {
        val viewGroup = LayoutInflater.from(parent.context).inflate(R.layout.stock_listing_item, parent, false) as ViewGroup

        return StockListingViewHolder(viewGroup, stockTickerBindingSubject)
    }

    override fun getItemCount(): Int = stockData.size

    override fun onBindViewHolder(holder: StockListingViewHolder, position: Int) {
        val stock = stockData[position]
        val res = holder.viewGroup.resources
        with(holder) {
            id.text = stock.id
            name.text = stock.name
            when (stock.priceDiff) {
                null -> price.text = res.getString(R.string.price_format, stock.price)
                else -> price.text = res.getString(R.string.price_with_diff_format, stock.price, stock.priceDiff)
            }
            companyType.text = stock.companyType.joinToString("|") // TODO this is really quick and dirty

            setOnClickListener(stock.id)
        }
    }

    override fun getItemId(position: Int): Long {
        return stockData[position].id.hashCode().toLong()
    }
}

class StockListingViewHolder(val viewGroup: ViewGroup, val stockTickerBindingSubject: PublishSubject<StockTickerState>): RecyclerView.ViewHolder(viewGroup) {
    val id: TextView by lazy { viewGroup.findViewById<TextView>(R.id.stock_id) }
    val name: TextView by lazy { viewGroup.findViewById<TextView>(R.id.stock_name) }
    val price: TextView by lazy { viewGroup.findViewById<TextView>(R.id.stock_price) }
    val companyType: TextView by lazy { viewGroup.findViewById<TextView>(R.id.company_type) }

    fun setOnClickListener(id: String) {
        viewGroup.setOnClickListener { stockTickerBindingSubject.onNext(LoadDetails(id)) }
    }
}
