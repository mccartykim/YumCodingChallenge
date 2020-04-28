package com.mccartykim.yumcodingchallenge.model

import androidx.recyclerview.widget.DiffUtil
import com.mccartykim.yumcodingchallenge.model.StockListing

class StockDiffCallback(val oldList: List<StockListing>, val newList: List<StockListing>): DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}

