package com.tunnellight.airport_terminal.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tunnellight.airport_terminal.R
import com.tunnellight.airport_terminal.model.Airport

/** Renders the list of airports on the search screen. */
class AirportAdapter(
    private val onClick: (Airport) -> Unit
) : RecyclerView.Adapter<AirportAdapter.AirportViewHolder>() {

    private val items = mutableListOf<Airport>()

    fun submit(airports: List<Airport>) {
        items.clear()
        items.addAll(airports)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_airport, parent, false)
        return AirportViewHolder(view)
    }

    override fun onBindViewHolder(holder: AirportViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AirportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val codeBadge: TextView = view.findViewById(R.id.codeBadge)
        private val name: TextView = view.findViewById(R.id.airportName)
        private val meta: TextView = view.findViewById(R.id.airportMeta)

        fun bind(airport: Airport) {
            codeBadge.text = airport.code
            name.text = airport.name
            meta.text = when {
                !airport.isDetailed -> "${airport.location} · Map & info"
                airport.terminals.size == 1 -> "${airport.location} · 1 terminal"
                else -> "${airport.location} · ${airport.terminals.size} terminals"
            }
            itemView.setOnClickListener { onClick(airport) }
        }
    }
}
