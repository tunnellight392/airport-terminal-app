package com.tunnellight.airport_terminal

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tunnellight.airport_terminal.data.AirportRepository
import com.tunnellight.airport_terminal.ui.InteractiveTerminalMapView
import com.tunnellight.airport_terminal.ui.applySystemBarInsets

/** Full-screen interactive map for a single terminal. */
class TerminalMapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "extra_airport_code"
        const val EXTRA_TERMINAL_INDEX = "extra_terminal_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal_map)

        findViewById<View>(R.id.header).applySystemBarInsets(top = true, horizontal = true)
        // Keeps the reset button and the gate card clear of the navigation bar.
        findViewById<View>(R.id.mapContainer).applySystemBarInsets(bottom = true, horizontal = true)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val code = intent.getStringExtra(EXTRA_CODE)
        val index = intent.getIntExtra(EXTRA_TERMINAL_INDEX, -1)
        val airport = code?.let { AirportRepository.findByCode(this, it) }
        val terminal = airport?.terminals?.getOrNull(index)

        if (airport == null || terminal == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.mapTerminalName).text = terminal.name
        findViewById<TextView>(R.id.mapAirportName).text =
            getString(R.string.map_airport_subtitle, airport.code, airport.city)

        val transitBanner = findViewById<LinearLayout>(R.id.transitBanner)
        val transitText = findViewById<TextView>(R.id.transitText)
        if (terminal.transit != null) {
            transitBanner.visibility = View.VISIBLE
            transitText.text = getString(R.string.transit_label, terminal.transit)
        } else {
            transitBanner.visibility = View.GONE
        }

        val gateInfoCard = findViewById<MaterialCardView>(R.id.gateInfoCard)
        val selectedGate = findViewById<TextView>(R.id.selectedGate)
        val selectedGateConcourse = findViewById<TextView>(R.id.selectedGateConcourse)

        val map = findViewById<InteractiveTerminalMapView>(R.id.interactiveMap)
        map.setTerminal(terminal.concourses, terminal.transit)
        map.onGateSelected = { label, concourse ->
            gateInfoCard.visibility = View.VISIBLE
            selectedGate.text = label
            selectedGateConcourse.text = concourse
        }

        findViewById<FloatingActionButton>(R.id.resetButton).setOnClickListener {
            map.resetView()
            gateInfoCard.visibility = View.GONE
        }
    }
}
