package com.tunnellight.airport_terminal.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tunnellight.airport_terminal.R
import com.tunnellight.airport_terminal.model.Airport

/**
 * Renders the list of airports on the search screen.
 *
 * Backed by [ListAdapter] so each search result set is diffed against the previous one and only
 * the rows that actually changed are rebound. Search re-runs on every keystroke, so the
 * alternative (notifyDataSetChanged) would rebind every visible row and drop the scroll position
 * on each character typed.
 */
class AirportAdapter(
    private val onClick: (Airport) -> Unit
) : ListAdapter<Airport, AirportAdapter.AirportViewHolder>(DIFF) {

    fun submit(airports: List<Airport>) = submitList(airports)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_airport, parent, false)
        return AirportViewHolder(view)
    }

    override fun onBindViewHolder(holder: AirportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AirportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val codeBadge: TextView = view.findViewById(R.id.codeBadge)
        private val name: TextView = view.findViewById(R.id.airportName)
        private val meta: TextView = view.findViewById(R.id.airportMeta)

        fun bind(airport: Airport) {
            codeBadge.text = airport.code
            name.text = airport.name
            // Shares terminal_count with the detail screen so the two never drift apart.
            val context = itemView.context
            val count = airport.terminals.size
            val detail = if (airport.isDetailed) {
                context.resources.getQuantityString(R.plurals.terminal_count, count, count)
            } else {
                context.getString(R.string.map_and_info)
            }
            meta.text = context.getString(R.string.airport_meta, airport.location, detail)
            itemView.setOnClickListener { onClick(airport) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Airport>() {
            /** The IATA code is unique across the merged dataset, so it identifies a row. */
            override fun areItemsTheSame(oldItem: Airport, newItem: Airport) =
                oldItem.code == newItem.code

            /** Airport is a data class, so this compares the rendered fields structurally. */
            override fun areContentsTheSame(oldItem: Airport, newItem: Airport) =
                oldItem == newItem
        }
    }
}
