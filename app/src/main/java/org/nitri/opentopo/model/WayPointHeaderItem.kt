package org.nitri.opentopo.model

class WayPointHeaderItem(var header: String?) : WayPointListItem {

    override val listItemType: Int
        get() = WayPointListItem.HEADER
}
