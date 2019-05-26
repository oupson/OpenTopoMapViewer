package org.nitri.opentopo.nearby.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Nearby", indices = [Index(value = ["pageid"], unique = true)])
class NearbyItem : Comparable<NearbyItem> {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    var pageid: String? = null

    var index: Int = 0

    var title: String? = null

    var description: String? = null

    var thumbnail: String? = null

    var width: Int = 0

    var height: Int = 0

    var lat: Double = 0.toDouble()

    var lon: Double = 0.toDouble()

    var url: String? = null

    @Ignore
    var distance: Int = 0

    override fun compareTo(other: NearbyItem): Int {
        return Integer.compare(distance, other.distance)
    }
}
