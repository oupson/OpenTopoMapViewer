package org.nitri.opentopo.model

interface WayPointListItem {

    val listItemType: Int

    companion object {

        val HEADER = 0
        val WAY_POINT = 1
    }
}
