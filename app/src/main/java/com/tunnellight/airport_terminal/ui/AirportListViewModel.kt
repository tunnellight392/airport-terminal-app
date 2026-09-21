package com.tunnellight.airport_terminal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tunnellight.airport_terminal.data.AirportRepository
import com.tunnellight.airport_terminal.model.Airport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds the search screen's state.
 *
 * Living in a ViewModel means the query, the results and the bulk-load outcome survive rotation,
 * so the screen no longer re-runs its search and reloads the dataset every time the Activity is
 * recreated. It is also the natural place to keep search off the main thread and to debounce it.
 */
class AirportListViewModel(application: Application) : AndroidViewModel(application) {

    /** How much of the dataset is actually available, for the subtitle. */
    sealed interface Coverage {
        object Loading : Coverage
        data class Ready(val total: Int, val detailed: Int) : Coverage

        /**
         * The bulk dataset could not be fetched and no cache was available, so search is limited
         * to the curated airports. Previously this failure was swallowed and the user was simply
         * shown a smaller list with no explanation.
         */
        data class Offline(val detailed: Int) : Coverage
    }

    data class UiState(
        val results: List<Airport> = emptyList(),
        val coverage: Coverage = Coverage.Loading,
        /** False until the first search resolves, so the empty state isn't shown while loading. */
        val loaded: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentQuery = ""
    private var searchJob: Job? = null

    init {
        // Show the curated airports straight away, then widen coverage once the bulk data lands.
        runSearch(currentQuery, debounce = false)
        viewModelScope.launch {
            val app = getApplication<Application>()
            val status = AirportRepository.ensureRemoteLoaded(app)
            val coverage = withContext(Dispatchers.Default) {
                val detailed = AirportRepository.detailedAirports(app).size
                if (status == AirportRepository.RemoteStatus.Loaded) {
                    Coverage.Ready(AirportRepository.airports(app).size, detailed)
                } else {
                    Coverage.Offline(detailed)
                }
            }
            _uiState.update { it.copy(coverage = coverage) }
            runSearch(currentQuery, debounce = false)
        }
    }

    fun onQueryChanged(query: String) {
        // Guards the spurious callback when the EditText restores its text after rotation.
        if (query == currentQuery) return
        currentQuery = query
        runSearch(query, debounce = true)
    }

    /**
     * Cancelling the previous job both debounces and guarantees last-write-wins: a superseded
     * search can never deliver its results after a newer one.
     */
    private fun runSearch(query: String, debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce && query.isNotEmpty()) delay(SEARCH_DEBOUNCE)
            val results = AirportRepository.search(getApplication(), query)
            _uiState.update { it.copy(results = results, loaded = true) }
        }
    }

    private companion object {
        /** Long enough to skip the intermediate states of a fast typist, short enough to feel live. */
        val SEARCH_DEBOUNCE = 150.milliseconds
    }
}
