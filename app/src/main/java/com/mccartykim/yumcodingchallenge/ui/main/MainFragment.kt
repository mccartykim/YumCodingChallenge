package com.mccartykim.yumcodingchallenge.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mccartykim.yumcodingchallenge.databinding.MainFragmentBinding
import io.reactivex.rxjava3.disposables.CompositeDisposable

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var bind: MainFragmentBinding
    private val rvAdapter = StockAdapter()

    private val disposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        bind = MainFragmentBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(this as ViewModelStoreOwner).get(MainViewModel::class.java)
        bind.model = viewModel
        bind.stockTickerRv.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = rvAdapter
            itemAnimator = null
        }

        bind.stockTickerSearchbar.doAfterTextChanged {
            e -> viewModel.filterQueryBindingSubject.onNext(NewQuery(e.toString()))
        }
    }

    override fun onResume() {
        super.onResume()
        disposable.addAll(viewModel.diffFilteredStocks.subscribe { rvAdapter.updateStocks(it.displayStocks, it.diff) })
        viewModel.stockTickerBindingSubject.onNext(ResumeTicker)
    }

    override fun onPause() {
        super.onPause()
        disposable.clear()
        viewModel.stockTickerBindingSubject.onNext(PauseTicker)
    }

}

