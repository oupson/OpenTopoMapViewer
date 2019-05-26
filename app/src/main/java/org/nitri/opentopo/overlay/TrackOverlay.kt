package org.nitri.opentopo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import androidx.core.content.ContextCompat

import org.nitri.opentopo.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

import java.util.ArrayList

import io.ticofab.androidgpxparser.parser.domain.Track
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.TrackSegment

class TrackOverlay
/**
 * Layer to display a track
 *
 * @param context
 * @param track
 * @see Track
 */
internal constructor(private val mContext: Context, private val mTrack: Track) : Overlay() {

    private val mPointsSegments = ArrayList<List<Point>>()

    override fun draw(canvas: Canvas, osmv: MapView, shadow: Boolean) {

        //Log.d(TAG, "Zoom: " + osmv.getZoomLevelDouble());

        val routePaint: Paint
        routePaint = Paint()
        routePaint.color = ContextCompat.getColor(mContext, R.color.colorTrack)
        routePaint.isAntiAlias = true
        routePaint.alpha = 204
        routePaint.style = Paint.Style.STROKE
        routePaint.strokeJoin = Paint.Join.ROUND
        routePaint.strokeCap = Paint.Cap.ROUND
        routePaint.strokeWidth = 12f

        if (mTrack.trackSegments != null) {
            for (trackSegment in mTrack.trackSegments) {
                val path = Path()
                createPointsSegments(osmv, trackSegment)
                //Log.d(TAG, "Point segments: " + mPointsSegments.size());
                if (mPointsSegments.size > 0) {
                    for (pointSegment in mPointsSegments) {
                        val firstPoint = pointSegment[0]
                        path.moveTo(firstPoint.x.toFloat(), firstPoint.y.toFloat())
                        var prevPoint = firstPoint
                        for (point in pointSegment) {
                            path.quadTo(prevPoint.x.toFloat(), prevPoint.y.toFloat(), point.x.toFloat(), point.y.toFloat())
                            prevPoint = point
                        }
                        canvas.drawPath(path, routePaint)
                    }
                }
            }
        }
    }

    /**
     * Since off-canvas drawing may discard the rendering of the entire track segment, cut the
     * track segment into points segments that need to be rendered.
     *
     * @param mapView
     * @param trackSegment
     */
    private fun createPointsSegments(mapView: MapView, trackSegment: TrackSegment) {
        mPointsSegments.clear()

        val mapCenter = Point(mapView.width / 2, mapView.height / 2)

        val offCenterLimit = (if (mapCenter.x > mapCenter.y) mapCenter.x * 2.5 else mapCenter.y * 2.5).toInt()
        val projection = mapView.projection

        var adding = false
        val pointsSegment = ArrayList<Point>()

        for (trackPoint in trackSegment.trackPoints) {
            val gp = GeoPoint(trackPoint.latitude!!, trackPoint.longitude!!)
            val point = projection.toPixels(gp, null)
            if (pixelDistance(mapCenter, point) < offCenterLimit) {
                if (!adding) {
                    adding = true
                    pointsSegment.clear()
                }
                pointsSegment.add(point)
            } else {
                if (adding) {
                    mPointsSegments.add(ArrayList(pointsSegment))
                    pointsSegment.clear()
                }
                adding = false
            }
        }
        if (pointsSegment.size > 0) {
            mPointsSegments.add(pointsSegment)
        }
    }

    private fun pixelDistance(p1: Point, p2: Point): Int {
        val deltaX = (p2.x - p1.x).toDouble()
        val deltaY = (p2.y - p1.y).toDouble()
        return Math.sqrt(Math.pow(deltaX, 2.0) + Math.pow(deltaY, 2.0)).toInt()
    }

    companion object {

        private val TAG = TrackOverlay::class.java.simpleName
    }

}
