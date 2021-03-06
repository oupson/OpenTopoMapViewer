package org.nitri.opentopo

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.util.TypedValue

import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

import io.ticofab.androidgpxparser.parser.domain.Point
import java.util.ArrayList
import java.util.Collections

import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Track
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.TrackSegment
import io.ticofab.androidgpxparser.parser.domain.WayPoint

object Util {

    /**
     * Distance between points
     *
     * @param point1
     * @param point2
     * @return meters
     */
    fun distance(point1: Point, point2: Point): Double {
        val lat1 = point1.latitude!!
        val lon1 = point1.longitude!!
        val lat2 = point2.latitude!!
        val lon2 = point2.longitude!!
        return distance(lat1, lon1, lat2, lon2)
    }

    /**
     * Distance between lat/lon pairs
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return meters
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == lat2 && lon1 == lon2)
            return 0.0
        val theta = lon1 - lon2
        var dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + (Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta)))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60.0 * 1.1515
        dist = dist * 1609.344
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    /**
     * Get GPX bounds
     *
     * @param gpx Gpx
     * @return BoundingBox
     */
    fun area(gpx: Gpx): BoundingBox {
        return area(getAllTrackGeoPoints(gpx))
    }

    /**
     * Get geo points bounds
     *
     * @param points List<GeoPoint>
     * @return BoundingBox
    </GeoPoint> */
    fun area(points: List<GeoPoint>): BoundingBox {

        var north = 0.0
        var south = 0.0
        var west = 0.0
        var east = 0.0

        for (i in points.indices) {
            if (points[i] == null) continue
            val lat = points[i].latitude
            val lon = points[i].longitude
            if (i == 0 || lat > north) north = lat
            if (i == 0 || lat < south) south = lat
            if (i == 0 || lon < west) west = lon
            if (i == 0 || lon > east) east = lon
        }
        return BoundingBox(north, east, south, west)
    }

    private fun getAllTrackGeoPoints(gpx: Gpx?): List<GeoPoint> {
        val geoPoints = ArrayList<GeoPoint>()
        if (gpx != null && gpx.tracks != null) {
            for (track in gpx.tracks) {
                if (track.trackSegments != null) {
                    for (segment in track.trackSegments) {
                        if (segment.trackPoints != null) {
                            for (trackPoint in segment.trackPoints) {
                                geoPoints.add(GeoPoint(trackPoint.latitude!!, trackPoint.longitude!!))
                            }
                        }
                    }
                }
            }
        }
        return geoPoints
    }

    /**
     * Get way point types (categories) from GPX
     *
     * @param gpx
     * @param defaultType
     * @return
     */
    fun getWayPointTypes(gpx: Gpx, defaultType: String): List<String> {
        val types = ArrayList<String>()
        if (gpx.wayPoints != null) {
            for (wayPoint in gpx.wayPoints) {
                var type = defaultType
                if (!TextUtils.isEmpty(wayPoint.type))
                    type = wayPoint.type
                if (!types.contains(type))
                    types.add(type)
            }
        }
        Collections.sort(types)
        return types
    }

    /**
     * Get a list of way points by type (categpry)
     *
     * @param gpx
     * @param type
     * @return
     */
    fun getWayPointsByType(gpx: Gpx, type: String): MutableList<WayPoint> {
        val wayPoints = ArrayList<WayPoint>()
        if (gpx.wayPoints != null) {
            for (wayPoint in gpx.wayPoints) {
                if (!TextUtils.isEmpty(wayPoint.type) && wayPoint.type == type)
                    wayPoints.add(wayPoint)
                else if (TextUtils.isEmpty(wayPoint.type) && TextUtils.isEmpty(type))
                    wayPoints.add(wayPoint)
            }
        }
        return wayPoints
    }

    private fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int): TypedValue {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue
    }

    /**
     * Get color integer by attribute
     *
     * @param context
     * @param colorAttr
     * @return
     */
    @ColorInt
    fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
        val resolvedAttr = resolveThemeAttr(context, colorAttr)
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        val colorRes = if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
        return ContextCompat.getColor(context, colorRes)
    }

    /**
     * Spanned text from HTML (compat)
     *
     * @param source
     * @return
     */
    fun fromHtml(source: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(source)
        }
    }

}
