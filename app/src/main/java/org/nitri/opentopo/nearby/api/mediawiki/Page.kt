package org.nitri.opentopo.nearby.api.mediawiki

class Page {

    var pageid: Int = 0

    var ns: Int = 0

    var title: String? = null

    var index: Int = 0

    var coordinates: List<PointCoordinates>? = null

    var thumbnail: Thumbnail? = null

    var terms: Terms? = null

    var fullurl: String? = null

    var canonicalurl: String? = null
}
