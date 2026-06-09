package com.tunnellight.airport_terminal.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tunnellight.airport_terminal.R
import com.tunnellight.airport_terminal.TerminalMapActivity
import com.tunnellight.airport_terminal.model.Terminal

/** Renders each terminal with its schematic map and the airlines that use it. */
class TerminalAdapter(
    private val airportCode: String,
    private val mapUrl: String?,
    private val terminals: List<Terminal>
) : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_terminal, parent, false)
        return TerminalViewHolder(view)
    }

    override fun onBindViewHolder(holder: TerminalViewHolder, position: Int) {
        holder.bind(airportCode, mapUrl, position, terminals[position])
    }

    override fun getItemCount(): Int = terminals.size

    class TerminalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.terminalName)
        private val description: TextView = view.findViewById(R.id.terminalDescription)
        private val map: TerminalMapView = view.findViewById(R.id.terminalMap)
        private val mapClickArea: FrameLayout = view.findViewById(R.id.mapClickArea)
        private val chips: ChipGroup = view.findViewById(R.id.airlineChips)

        fun bind(airportCode: String, mapUrl: String?, index: Int, terminal: Terminal) {
            name.text = terminal.name
            description.text = terminal.description
            map.setConcourses(terminal.concourses)

            // The map card opens the airport's official terminal-map page. If an airport has
            // no official URL, fall back to the in-app interactive schematic.
            mapClickArea.setOnClickListener {
                val context = it.context
                if (mapUrl != null) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl)))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "No app available to open the map", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    context.startActivity(
                        Intent(context, TerminalMapActivity::class.java)
                            .putExtra(TerminalMapActivity.EXTRA_CODE, airportCode)
                            .putExtra(TerminalMapActivity.EXTRA_TERMINAL_INDEX, index)
                    )
                }
            }

            chips.removeAllViews()
            val context = chips.context
            for (airline in terminal.airlines) {
                val chip = Chip(context).apply {
                    text = airline
                    isClickable = false
                    isCheckable = false
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E3F2FD"))
                    setTextColor(Color.parseColor("#0D47A1"))
                    chipStrokeWidth = 0f
                }
                chips.addView(chip)
            }
        }
    }
}
