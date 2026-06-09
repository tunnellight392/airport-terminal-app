package com.tunnellight.airport_terminal

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.tunnellight.airport_terminal.data.AirportRepository
import com.tunnellight.airport_terminal.model.Airport
import com.tunnellight.airport_terminal.ui.AirportAdapter

class MainActivity : AppCompatActivity() {

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
                runSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Show the curated airports immediately, then expand coverage from the network.
        runSearch()
        subtitle.text = "Loading more US airports…"
        AirportRepository.ensureRemoteLoaded(this) {
            val total = AirportRepository.airports(this).size
            val detailed = AirportRepository.detailedAirports(this).size
            subtitle.text = "$total US airports · $detailed with full terminal detail"
            runSearch()
        }
    }

    private fun runSearch() {
        val query = searchInput.text?.toString() ?: ""
        showResults(AirportRepository.search(this, query))
    }

    private fun showResults(airports: List<Airport>) {
        adapter.submit(airports)
        emptyView.visibility = if (airports.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openAirport(airport: Airport) {
        val intent = Intent(this, AirportDetailActivity::class.java)
            .putExtra(AirportDetailActivity.EXTRA_CODE, airport.code)
        startActivity(intent)
    }
}
