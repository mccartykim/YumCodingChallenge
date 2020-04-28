package com.mccartykim.yumcodingchallenge.ui.detail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mccartykim.yumcodingchallenge.R

import com.mccartykim.yumcodingchallenge.databinding.StockDetailFragmentBinding
import com.mccartykim.yumcodingchallenge.model.StockDetail
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer

class StockDetailFragment : Fragment() {

    companion object {
        fun newInstance() =
            StockDetailFragment()
    }

    private lateinit var bind: StockDetailFragmentBinding

    private lateinit var viewModel: StockDetailViewModel
    private val disposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        bind = StockDetailFragmentBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this as ViewModelStoreOwner).get(StockDetailViewModel::class.java)
    }

    // The BehaviorSubject that detailSubject is based on will repeat the last status next time the view resubscribes,
    // so resubscribing onResume and disposing of the subscription on pause makes sense
    override fun onResume() {
        super.onResume()
        disposable.addAll(viewModel.detailSubject.observeOn(AndroidSchedulers.mainThread()).subscribe(detailUpdateConsumer))
    }

    override fun onPause() {
        super.onPause()
        disposable.clear()
    }

    private val detailUpdateConsumer = Consumer<DetailStatus> {
        when (it) {
            Loading -> displayLoading()
            is StockDetailUpdate -> displayStockDetails(it.detail)
            is ErrorUpdate -> displayError(it)
        }
    }

    private fun displayLoading() = with(bind) {
        successLayout.visibility = View.GONE
        errorLayout.visibility = View.GONE
        loadingProgressBar.visibility = View.VISIBLE
    }

    private fun displayStockDetails(update: StockDetail) = with(bind) {
        successLayout.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
        loadingProgressBar.visibility = View.GONE
        stockId.text = update.id
        stockName.text = update.name
        detailPrice.text = getString(R.string.price_format, update.price)
        allTimeHigh.text = getString(R.string.all_time_high_price_format, update.allTimeHigh)
        detailTags.text = update.companyType.joinToString(" | ")
        address.text = update.address
        website.text = update.website
        Picasso.get()
            .load(update.imageUrl)
            .resize(400, 400)
            .centerInside()
            .into(logo)
        logo.contentDescription = update.name
    }

    private fun displayError(update: ErrorUpdate) = with(bind) {
        successLayout.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
        loadingProgressBar.visibility = View.GONE
        errorMessage.text = update.message
    }
}
