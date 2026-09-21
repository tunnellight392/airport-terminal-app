package com.tunnellight.airport_terminal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tunnellight.airport_terminal.data.AirportRepository
import com.tunnellight.airport_terminal.ui.TerminalAdapter
import com.tunnellight.airport_terminal.ui.applySystemBarInsets

class AirportDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "extra_airport_code"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        findViewById<View>(R.id.header).applySystemBarInsets(top = true, horizontal = true)
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
        val terminalLabel = resources.getQuantityString(R.plurals.terminal_count, count, count)
        findViewById<TextView>(R.id.detailMeta).text =
            getString(R.string.airport_meta, airport.location, terminalLabel)

        val terminalList = findViewById<RecyclerView>(R.id.terminalList)
        val emptyDetail = findViewById<LinearLayout>(R.id.emptyDetail)

        if (airport.isDetailed) {
            emptyDetail.visibility = View.GONE
            terminalList.visibility = View.VISIBLE
            terminalList.applySystemBarInsets(bottom = true, horizontal = true)
            terminalList.layoutManager = LinearLayoutManager(this)
            terminalList.adapter = TerminalAdapter(airport.code, airport.mapUrl, airport.terminals)
        } else {
            terminalList.visibility = View.GONE
            emptyDetail.visibility = View.VISIBLE
            emptyDetail.applySystemBarInsets(bottom = true, horizontal = true)
            findViewById<MaterialButton>(R.id.officialMapButton).setOnClickListener {
                val url = airport.mapUrl ?: return@setOnClickListener
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.no_map_app, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
