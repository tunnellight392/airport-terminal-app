package com.tunnellight.airport_terminal

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.tunnellight.airport_terminal.model.Airport
import com.tunnellight.airport_terminal.ui.AirportAdapter
import com.tunnellight.airport_terminal.ui.AirportListViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: AirportListViewModel by viewModels()

    private lateinit var adapter: AirportAdapter
    private lateinit var emptyView: TextView
    private lateinit var searchInput: TextInputEditText
    private lateinit var subtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        emptyView = findViewById(R.id.emptyView)
        subtitle = findViewById(R.id.subtitle)

        adapter = AirportAdapter { airport -> openAirport(airport) }

        findViewById<RecyclerView>(R.id.airportList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        searchInput = findViewById(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQueryChanged(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Collection is tied to STARTED, so it stops while the screen is in the background and
        // resumes with whatever state the ViewModel holds.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: AirportListViewModel.UiState) {
        adapter.submit(state.results)
        emptyView.visibility =
            if (state.loaded && state.results.isEmpty()) View.VISIBLE else View.GONE
        subtitle.text = when (val coverage = state.coverage) {
            is AirportListViewModel.Coverage.Loading ->
                getString(R.string.subtitle_loading)

            is AirportListViewModel.Coverage.Ready -> getString(
                R.string.subtitle_ready,
                resources.getQuantityString(
                    R.plurals.airport_count, coverage.total, coverage.total
                ),
                resources.getQuantityString(
                    R.plurals.detailed_count, coverage.detailed, coverage.detailed
                )
            )

            is AirportListViewModel.Coverage.Offline -> getString(
                R.string.subtitle_offline,
                resources.getQuantityString(
                    R.plurals.detailed_count, coverage.detailed, coverage.detailed
                )
            )
        }
    }

    private fun openAirport(airport: Airport) {
        val intent = Intent(this, AirportDetailActivity::class.java)
            .putExtra(AirportDetailActivity.EXTRA_CODE, airport.code)
        startActivity(intent)
    }
}
