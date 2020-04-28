package com.mccartykim.yumcodingchallenge.ui.detail

import androidx.lifecycle.ViewModel
import com.mccartykim.yumcodingchallenge.model.NetworkError
import com.mccartykim.yumcodingchallenge.model.StockDetail
import com.mccartykim.yumcodingchallenge.model.StockNetworkService
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject

class StockDetailViewModel : ViewModel() {
    val detailSubject: BehaviorSubject<DetailStatus> = BehaviorSubject.createDefault(Loading)
    private val disposables = CompositeDisposable()

    init {
        disposables.addAll(StockNetworkService.detailSubject.subscribe {
            detailSubject.onNext(
                when (it) {
                    is StockDetail -> StockDetailUpdate(it)
                    is NetworkError -> ErrorUpdate(it.message)
                }
            )
        })
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

sealed class DetailStatus
object Loading: DetailStatus()
class StockDetailUpdate(val detail: StockDetail): DetailStatus()
class ErrorUpdate(val message: String): DetailStatus()
