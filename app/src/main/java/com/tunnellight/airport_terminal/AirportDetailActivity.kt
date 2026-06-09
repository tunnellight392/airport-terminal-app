package com.tunnellight.airport_terminal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tunnellight.airport_terminal.data.AirportRepository
import com.tunnellight.airport_terminal.ui.TerminalAdapter

class AirportDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "extra_airport_code"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val code = intent.getStringExtra(EXTRA_CODE)
        val airport = code?.let { AirportRepository.findByCode(this, it) }

        if (airport == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.detailCode).text = airport.code
        findViewById<TextView>(R.id.detailName).text = airport.name
        val count = airport.terminals.size
        val terminalLabel = if (count == 1) "1 terminal" else "$count terminals"
        findViewById<TextView>(R.id.detailMeta).text = "${airport.location} · $terminalLabel"

        val terminalList = findViewById<RecyclerView>(R.id.terminalList)
        val emptyDetail = findViewById<LinearLayout>(R.id.emptyDetail)

        if (airport.isDetailed) {
            emptyDetail.visibility = View.GONE
            terminalList.visibility = View.VISIBLE
            terminalList.layoutManager = LinearLayoutManager(this)
            terminalList.adapter = TerminalAdapter(airport.code, airport.mapUrl, airport.terminals)
        } else {
            terminalList.visibility = View.GONE
            emptyDetail.visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.officialMapButton).setOnClickListener {
                val url = airport.mapUrl ?: return@setOnClickListener
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "No app available to open the map", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
