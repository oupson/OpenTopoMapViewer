package org.nitri.opentopo.model

import io.ticofab.androidgpxparser.parser.domain.WayPoint

class WayPointItem(var wayPoint: WayPoint?) : WayPointListItem {

    override val listItemType: Int
        get() = WayPointListItem.WAY_POINT
}
