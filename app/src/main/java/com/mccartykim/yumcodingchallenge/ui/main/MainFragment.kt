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
import com.mccartykim.yumcodingchallenge.R
import com.mccartykim.yumcodingchallenge.databinding.MainFragmentBinding
import com.mccartykim.yumcodingchallenge.ui.stockdetail.StockDetailFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

class MainFragment: Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        const val QUERY_KEY = "QUERY"
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var bind: MainFragmentBinding
    private lateinit var rvAdapter: StockAdapter

    private val disposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        bind = MainFragmentBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val loadQuery = savedInstanceState?.getString(QUERY_KEY)?:""
        viewModel = ViewModelProvider(this as ViewModelStoreOwner).get(MainViewModel::class.java)

        rvAdapter = StockAdapter(viewModel.stockTickerBindingSubject)
        bind.stockTickerRv.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rvAdapter
            itemAnimator = null
        }

        bind.stockTickerSearchbar.doAfterTextChanged { postQuery(it?.toString()?:"") }
        bind.stockTickerSearchbar.setText(loadQuery)
    }

    val postQuery = { text: String -> viewModel.filterQueryBindingSubject.onNext(text)}

    fun onShowDetails() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, StockDetailFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Necessary to re-apply query on back
        disposable.addAll(
            viewModel.diffFilteredStocks.observeOn(AndroidSchedulers.mainThread())
                .subscribe { rvAdapter.updateStocks(it.displayStocks, it.diff) },
            viewModel.viewDetailsSubject.observeOn(AndroidSchedulers.mainThread()).subscribe { onShowDetails() }
        )
        postQuery(bind.stockTickerSearchbar.text?.toString()?:"")
        viewModel.stockTickerBindingSubject.onNext(ResumeTicker)
    }

    override fun onPause() {
        super.onPause()
        disposable.clear()
        viewModel.stockTickerBindingSubject.onNext(PauseTicker)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        bind.stockTickerSearchbar.text?.let {
            outState.putString(QUERY_KEY, it.toString())
        }
    }

}

