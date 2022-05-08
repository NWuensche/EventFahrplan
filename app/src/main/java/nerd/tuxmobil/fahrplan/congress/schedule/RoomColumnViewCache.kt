package nerd.tuxmobil.fahrplan.congress.schedule

import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nerd.tuxmobil.fahrplan.congress.models.RoomData
import nerd.tuxmobil.fahrplan.congress.schedule.RoomColumnViewCache.RoomColumnView

internal interface IRoomColumnViewCache {

    /**
     * Look for a [RoomColumnView] for the room with the name [roomName].
     * If such a view does not exist, create one.
     */
    fun getOrCreateRoomColumnView(
        roomName: String,
        columnWidth: Int
    ): RoomColumnViewCache.RoomColumnView
}

/**
 * Cache for reusing RecycleView/Adapter inside the [FahrplanFragment] when stuff updates in the columns/Rooms
 */
internal class RoomColumnViewCache(private val fragment: FahrplanFragment): IRoomColumnViewCache {

    /**
     * Cache of RecyclerViews
     * Key is [nerd.tuxmobil.fahrplan.congress.models.RoomData.roomName]
     */
    private val cache: MutableMap<String, RoomColumnView> = mutableMapOf()

    private val layoutCalculator = LayoutCalculator(fragment.getNormalizedBoxHeight())

    override fun getOrCreateRoomColumnView(
        roomName: String,
        columnWidth: Int,
    ): RoomColumnView {
        return cache.getOrPut(roomName) {
            createFilledRecyclerView(
                columnWidth
            )
        }
    }

    /**
     * In case there is no corresponding RecyclerView in the cache, create one and fill it with data
     */
    private fun createFilledRecyclerView(
        columnWidth: Int,
    ): RoomColumnView {
        return RecyclerView(fragment.requireContext()).apply {
            setHasFixedSize(true)
            setFadingEdgeLength(0)
            isNestedScrollingEnabled = false // enables flinging
            layoutManager = LinearLayoutManager(context)
            layoutParams =
                RecyclerView.LayoutParams(columnWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }.let {
            RoomColumnView(it)
        }
    }
    
    inner class RoomColumnView(val rv: RecyclerView) {

        /**
         * add session data to adapter
         * Only updates adapter if the [roomData] really changed.
         */
        fun updateData(
            roomData: RoomData,
            conference: Conference,
            drawer: SessionViewDrawer,
        ) {
            val adapter = rv.adapter
            val currAdapterData = (adapter as? SessionViewColumnAdapter)?.sessions

            if (currAdapterData == roomData.sessions) {
                adapter.notifyDataSetChanged() //e.g. favour article
            } else {
                /**
                 * Complicated to just update the old adapter with new data,
                 * because Adapter-Layout-Parameter might have changed as well (which we would need to recalculate as well)
                 * Hence, we just create a new adapter
                 */
                addNewAdapter(roomData, conference, drawer)
            }
        }

        private fun addNewAdapter(
            roomData: RoomData,
            conference: Conference,
            drawer: SessionViewDrawer
        ) {
            val layoutParamsBySession = layoutCalculator.calculateLayoutParams(roomData, conference)

            rv.adapter = SessionViewColumnAdapter(
                sessions = roomData.sessions,
                layoutParamsBySession = layoutParamsBySession,
                drawer = drawer,
                eventsHandler = fragment
            )
        }
    }
}

