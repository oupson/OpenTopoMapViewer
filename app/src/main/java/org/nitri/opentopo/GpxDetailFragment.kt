package org.nitri.opentopo

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

import org.nitri.opentopo.adapter.WayPointListAdapter
import org.nitri.opentopo.domain.DistancePoint
import org.nitri.opentopo.model.WayPointHeaderItem
import org.nitri.opentopo.model.WayPointItem
import org.nitri.opentopo.model.WayPointListItem
import org.nitri.opentopo.view.ChartValueMarkerView

import java.util.ArrayList
import java.util.Locale

import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Track
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.TrackSegment
import io.ticofab.androidgpxparser.parser.domain.WayPoint


class GpxDetailFragment : androidx.fragment.app.Fragment(), WayPointListAdapter.OnItemClickListener, WayPointDetailDialogFragment.Callback {

    private var mListener: OnFragmentInteractionListener? = null
    private var mGpx: Gpx? = null
    private var mTrackDistanceLine: MutableList<DistancePoint>? = null
    private var mDistance: Double = 0.toDouble()
    private var mElevation: Boolean = false
    private var mElevationChart: LineChart? = null
    private var tvName: TextView? = null
    private var tvDescription: TextView? = null
    private var tvLength: TextView? = null
    private var mTfRegular: Typeface? = null
    private var mTfLight: Typeface? = null

    private var mMinElevation = 0.0
    private var mMaxElevation = 0.0
    internal var mWayPointListItems: MutableList<WayPointListItem> = ArrayList()
    private var mWayPointListAdapter: WayPointListAdapter? = null
    private var wvDescription: WebView? = null
    private var mSelectedIndex: Int = 0

    override val selectedWayPointItem: WayPointItem?
        get() = mWayPointListItems[mSelectedIndex] as WayPointItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        mGpx = mListener!!.gpx
        if (mGpx != null && mGpx!!.tracks != null) {
            for (track in mGpx!!.tracks) {
                buildTrackDistanceLine(track)
            }
            if (activity != null) {
                mTfRegular = Typeface.createFromAsset(activity!!.assets, "OpenSans-Regular.ttf")
                mTfLight = Typeface.createFromAsset(activity!!.assets, "OpenSans-Light.ttf")
            }
        }
        mWayPointListAdapter = WayPointListAdapter(mWayPointListItems, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_gpx_detail, container, false)
        tvName = rootView.findViewById(R.id.tvTitle)
        tvDescription = rootView.findViewById(R.id.tvDescription)
        wvDescription = rootView.findViewById(R.id.wvDescription)
        wvDescription!!.setBackgroundColor(Color.TRANSPARENT)
        tvLength = rootView.findViewById(R.id.tvLength)
        val chartContainer = rootView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.chartContainer)
        mElevationChart = rootView.findViewById(R.id.elevationChart)
        val wayPointRecyclerView = rootView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.way_point_recycler_view)
        wayPointRecyclerView.setHasFixedSize(true)
        wayPointRecyclerView.isNestedScrollingEnabled = false
        wayPointRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        wayPointRecyclerView.adapter = mWayPointListAdapter


        if (mElevation) {
            setUpElevationChart()
            setChartData()
        } else {
            chartContainer.visibility = View.GONE
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // For now, use title and description of first track
        if (mGpx != null && mGpx!!.tracks != null && mGpx!!.tracks[0] != null) {
            if (TextUtils.isEmpty(mGpx!!.tracks[0].trackName)) {
                tvName!!.visibility = View.GONE
            } else {
                tvName!!.text = mGpx!!.tracks[0].trackName
            }
            val description = mGpx!!.tracks[0].trackDesc
            if (TextUtils.isEmpty(description)) {
                tvDescription!!.visibility = View.GONE
                wvDescription!!.visibility = View.GONE
            } else {
                if (description.matches(".*<\\s*img\\s.*>.*".toRegex())) {
                    tvDescription!!.visibility = View.GONE
                    wvDescription!!.visibility = View.VISIBLE
                    wvDescription!!.loadData(description, "text/html; charset=utf-8", "UTF-8")
                } else {
                    tvDescription!!.visibility = View.VISIBLE
                    wvDescription!!.visibility = View.GONE
                    tvDescription!!.text = Util.fromHtml(description)
                    tvDescription!!.movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }

        if (mDistance > 0) {
            tvLength!!.text = String.format(Locale.getDefault(), "%.2f km", mDistance / 1000f)
        } else {
            tvLength!!.visibility = View.GONE
        }

        if (mGpx != null && mGpx!!.wayPoints != null) {
            buildWayPointList()
            mWayPointListAdapter!!.notifyDataSetChanged()
        }
    }

    private fun buildTrackDistanceLine(track: Track) {
        mTrackDistanceLine = ArrayList()
        mDistance = 0.0
        mElevation = false
        var prevTrackPoint: TrackPoint? = null
        if (track.trackSegments != null) {
            for (segment in track.trackSegments) {
                if (segment.trackPoints != null) {
                    mMaxElevation = segment.trackPoints[0].elevation!!
                    mMinElevation = mMaxElevation
                    for (trackPoint in segment.trackPoints) {
                        if (prevTrackPoint != null) {
                            mDistance += Util.distance(prevTrackPoint, trackPoint)
                        }

                        val builder = DistancePoint.Builder()
                        builder.setDistance(mDistance)
                        if (trackPoint.elevation != null) {
                            val elevation = trackPoint.elevation!!
                            if (elevation < mMinElevation)
                                mMinElevation = elevation
                            if (elevation > mMaxElevation)
                                mMaxElevation = elevation
                            builder.setElevation(elevation)
                            mElevation = true

                            mTrackDistanceLine!!.add(builder.build())
                        }
                        prevTrackPoint = trackPoint
                    }
                }
            }
        }
    }

    private fun buildWayPointList() {
        val defaultType = getString(R.string.poi)
        var wayPoints: MutableList<WayPoint>
        mWayPointListItems.clear()
        for (type in Util.getWayPointTypes(mGpx!!, defaultType)) {
            wayPoints = Util.getWayPointsByType(mGpx!!, type)
            if (type == defaultType)
                wayPoints.addAll(Util.getWayPointsByType(mGpx!!, null!!))
            if (wayPoints.size > 0) {
                mWayPointListItems.add(WayPointHeaderItem(type))
                for (wayPoint in wayPoints) {
                    mWayPointListItems.add(WayPointItem(wayPoint))
                }
            }
        }
    }

    private fun setUpElevationChart() {
        val l = mElevationChart!!.legend
        l.isEnabled = false
        mElevationChart!!.description.isEnabled = false

        val mv = ChartValueMarkerView(activity!!, R.layout.chart_value_marker_view)
        mv.chartView = mElevationChart
        mElevationChart!!.marker = mv

        val primaryTextColorInt = Util.resolveColorAttr(activity!!, android.R.attr.textColorPrimary)

        val xAxis = mElevationChart!!.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.typeface = mTfLight
        xAxis.textSize = 10f
        xAxis.textColor = Color.WHITE
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        xAxis.textColor = primaryTextColorInt
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                return String.format(Locale.getDefault(), "%.1f", value / 1000)
            }
        }

        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = mDistance.toFloat()

        val yAxis = mElevationChart!!.axisLeft
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        yAxis.typeface = mTfLight
        yAxis.setDrawGridLines(false)
        yAxis.isGranularityEnabled = true
        yAxis.textColor = primaryTextColorInt

        val margin = mMaxElevation.toFloat() * .2f
        var yMin = mMinElevation.toFloat() - margin
        val yMax = mMaxElevation.toFloat() + margin
        if (yMin < 0 && mMinElevation >= 0)
            yMin = 0f

        yAxis.axisMinimum = yMin
        yAxis.axisMaximum = yMax

        mElevationChart!!.axisRight.setDrawLabels(false)

        mElevationChart!!.viewPortHandler.setMaximumScaleX(2f)
        mElevationChart!!.viewPortHandler.setMaximumScaleY(2f)
    }

    private fun setChartData() {
        val elevationValues = ArrayList<Entry>()
        for (point in mTrackDistanceLine!!) {
            if (point.elevation != null)
                elevationValues.add(Entry(point.distance!!.toFloat(), point.elevation!!.toFloat()))
        }
        val elevationDataSet = LineDataSet(elevationValues, getString(R.string.elevation))
        elevationDataSet.setDrawValues(false)
        elevationDataSet.lineWidth = 2f
        elevationDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        elevationDataSet.color = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
        elevationDataSet.setDrawCircles(false)
        elevationDataSet.axisDependency = YAxis.AxisDependency.LEFT
        val elevationData = LineData(elevationDataSet)
        mElevationChart!!.data = elevationData
        mElevationChart!!.invalidate()
    }

    override fun onItemClick(index: Int) {
        mSelectedIndex = index
        if (activity != null && !activity!!.isFinishing) {
            val wayPointDetailDialogFragment = WayPointDetailDialogFragment()
            wayPointDetailDialogFragment.show(activity!!.supportFragmentManager, MainActivity.WAY_POINT_DETAIL_FRAGMENT_TAG)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        mListener!!.setUpNavigation(true)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {

        /**
         * Retrieve the current GPX
         *
         * @return Gpx
         */
        val gpx: Gpx?

        /**
         * Set up navigation arrow
         */
        fun setUpNavigation(upNavigation: Boolean)
    }

    companion object {


        fun newInstance(): GpxDetailFragment {
            return GpxDetailFragment()
        }
    }
}// Required empty public constructor
