package org.nitri.opentopo

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.overlay.OverlayHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*
import androidx.fragment.app.Fragment
import org.osmdroid.tileprovider.tilesource.XYTileSource

class MapFragment : Fragment(), LocationListener, PopupMenu.OnMenuItemClickListener {

    private var mMapView: MapView? = null
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var mCompassOverlay: CompassOverlay? = null
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    private var mLocationManager: LocationManager? = null
    private var mCurrentLocation: Location? = null
    private var mOverlayHelper: OverlayHelper? = null
    private val mMapHandler = Handler()
    private val mCenterRunnable = object : Runnable {

        override fun run() {
            if (mMapView != null && mCurrentLocation != null) {
                mMapView!!.controller.animateTo(GeoPoint(mCurrentLocation!!))
            }
            mMapHandler.postDelayed(this, 5000)
        }
    }

    private val mDragListener = object : MapListener {
        override fun onScroll(event: ScrollEvent): Boolean {
            if (mFollow && mMapHandler != null && mCenterRunnable != null) {
                mMapHandler.removeCallbacks(mCenterRunnable)
                mMapHandler.postDelayed(mCenterRunnable, 6000)
            }
            return true
        }

        override fun onZoom(event: ZoomEvent): Boolean {
            return false
        }
    }

    private var mFollow: Boolean = false

    private var mListener: OnFragmentInteractionListener? = null

    private var mPrefs: SharedPreferences? = null


    private var mBaseMap = BASE_MAP_OTM
    private var mCopyRightView: TextView? = null
    private var mOverlay = OverlayHelper.OVERLAY_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        if (activity != null) {
            mPrefs = activity!!.getSharedPreferences(MAP_PREFS, Context.MODE_PRIVATE)
            mBaseMap = mPrefs!!.getInt(PREF_BASE_MAP, BASE_MAP_OTM)
            mOverlay = mPrefs!!.getInt(PREF_OVERLAY, OverlayHelper.OVERLAY_NONE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mMapView = view.findViewById(R.id.mapview)

        val dm = this.resources.displayMetrics

        val activity = activity

        if (activity != null) {
            mCompassOverlay = CompassOverlay(getActivity()!!, InternalCompassOrientationProvider(getActivity()!!),
                    mMapView)
            mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(getActivity()!!),
                    mMapView!!)

            val bmCrosshairs = BitmapFactory.decodeResource(resources,
                    R.drawable.ic_crosshairs)

            mLocationOverlay!!.setPersonIcon(bmCrosshairs)
            mLocationOverlay!!.setPersonHotspot(bmCrosshairs.width / 2f, bmCrosshairs.height / 2f)

            mScaleBarOverlay = ScaleBarOverlay(mMapView!!)
            mScaleBarOverlay!!.setCentred(true)
            mScaleBarOverlay!!.setScaleBarOffset(dm.widthPixels / 2, 10)

            mRotationGestureOverlay = RotationGestureOverlay(mMapView)
            mRotationGestureOverlay!!.isEnabled = true

            mMapView!!.controller.setZoom(15.0)
            mMapView!!.maxZoomLevel = 17.0
            mMapView!!.isTilesScaledToDpi = true
            mMapView!!.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            mMapView!!.setMultiTouchControls(true)
            mMapView!!.isFlingEnabled = true
            mMapView!!.overlays.add(this.mLocationOverlay)
            mMapView!!.overlays.add(this.mCompassOverlay)
            mMapView!!.overlays.add(this.mScaleBarOverlay)

            mMapView!!.addMapListener(DelayedMapListener(mDragListener))

            mCopyRightView = view.findViewById(R.id.copyrightView)

            setBaseMap()

            mLocationOverlay!!.enableMyLocation()
            mLocationOverlay!!.enableFollowLocation()
            mLocationOverlay!!.isOptionsMenuEnabled = true
            mCompassOverlay!!.enableCompass()
            mMapView!!.visibility = View.VISIBLE
            mOverlayHelper = OverlayHelper(getActivity()!!, mMapView)

            setTilesOverlay()
        }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mListener!!.setGpx()
        val arguments = arguments
        // Move to received geo intent coordinates
        if (arguments != null) {
            if (arguments.containsKey(PARAM_LATITUDE) && arguments.containsKey(PARAM_LONGITUDE)) {
                val lat = arguments.getDouble(PARAM_LATITUDE)
                val lon = arguments.getDouble(PARAM_LONGITUDE)
                animateToLatLon(lat, lon)
            }
            if (arguments.containsKey(PARAM_NEARBY_PLACE) && arguments.getBoolean(PARAM_NEARBY_PLACE)) {
                mListener!!.selectedNearbyPlace
            }
        }
        if (mListener!!.selectedNearbyPlace != null) {
            showNearbyPlace(mListener!!.selectedNearbyPlace)
        }

    }

    private fun animateToLatLon(lat: Double, lon: Double) {
        mMapHandler.postDelayed({
            if (mMapView != null) {
                disableFollow()
                mMapView!!.controller.animateTo(GeoPoint(lat, lon))
            }
        }, 500)
    }
    val osmFrTileSource : XYTileSource by lazy {
        XYTileSource("osmfr", 1, 19, 256, ".png",
                arrayOf("https://a.tile.openstreetmap.fr/osmfr/", "https://b.tile.openstreetmap.fr/osmfr/", "https://c.tile.openstreetmap.fr/osmfr/"))
    }
    private fun setBaseMap() {
        when (mBaseMap) {
            BASE_MAP_OTM -> mMapView!!.setTileSource(TileSourceFactory.OpenTopo)
            BASE_MAP_OSM -> mMapView!!.setTileSource(TileSourceFactory.MAPNIK)
            BASE_MAP_OSM_FR -> mMapView?.setTileSource(osmFrTileSource)
        }
        mMapView!!.invalidate()

        //final OnlineTileSourceBase localTopo = new XYTileSource("OpenTopoMap", 0, 19, 256, ".png",
        //        new String[]{"http://192.168.2.108/hot/"}, "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)");
        //mMapView.setTileSource(localTopo);

        setCopyrightNotice()
    }

    private fun setTilesOverlay() {
        mOverlayHelper!!.setTilesOverlay(mOverlay)
        setCopyrightNotice()
    }

    private fun setCopyrightNotice() {

        val copyrightStringBuilder = StringBuilder()
        val mapCopyRightNotice = mMapView!!.tileProvider.tileSource.copyrightNotice
        copyrightStringBuilder.append(mapCopyRightNotice)
        if (mOverlayHelper != null) {
            val overlayCopyRightNotice = mOverlayHelper!!.copyrightNotice
            if (!TextUtils.isEmpty(mapCopyRightNotice) && !TextUtils.isEmpty(overlayCopyRightNotice)) {
                copyrightStringBuilder.append(", ")
            }
            copyrightStringBuilder.append(overlayCopyRightNotice)
        }
        val copyRightNotice = copyrightStringBuilder.toString()

        if (!TextUtils.isEmpty(copyRightNotice)) {
            mCopyRightView!!.text = copyRightNotice
            mCopyRightView!!.visibility = View.VISIBLE
        } else {
            mCopyRightView!!.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {

        if (mFollow) {
            mLocationOverlay!!.enableFollowLocation()
            mMapHandler.removeCallbacks(mCenterRunnable)
            mMapHandler.post(mCenterRunnable)
        }
        mLocationOverlay!!.enableMyLocation()
        mCompassOverlay!!.enableCompass()
        mScaleBarOverlay!!.enableScaleBar()

        mMapView!!.invalidate()
    }

    private fun enableFollow() {
        mFollow = true
        if (activity != null)
            (activity as AppCompatActivity).supportInvalidateOptionsMenu()
        mLocationOverlay!!.enableFollowLocation()
        mMapHandler.removeCallbacks(mCenterRunnable)
        mMapHandler.post(mCenterRunnable)
    }

    private fun disableFollow() {
        mFollow = false
        if (activity != null)
            (activity as AppCompatActivity).supportInvalidateOptionsMenu()
        mLocationOverlay!!.disableFollowLocation()
        mMapHandler.removeCallbacks(mCenterRunnable)
    }

    override fun onPause() {
        super.onPause()
        try {
            mLocationManager!!.removeUpdates(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        mMapHandler.removeCallbacks(mCenterRunnable)

        mCompassOverlay!!.disableCompass()
        mLocationOverlay!!.disableFollowLocation()
        mLocationOverlay!!.disableMyLocation()
        mScaleBarOverlay!!.disableScaleBar()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        //File basePath = Configuration.getInstance().getOsmdroidBasePath();
        //File cache = Configuration.getInstance().getOsmdroidTileCache();
        initMap()
        if (activity != null) {
            mLocationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (mLocationManager != null) {
                try {
                    mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
                } catch (ex: Exception) {
                    ex.printStackTrace()

                }

                try {
                    mLocationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

            }
        }
    }

    internal fun setGpx(gpx: Gpx, zoom: Boolean) {
        mOverlayHelper!!.setGpx(gpx)
        if (activity != null)
            (activity as AppCompatActivity).supportInvalidateOptionsMenu()
        if (zoom) {
            disableFollow()
            zoomToBounds(Util.area(gpx))
        }
    }

    private fun showGpxdialog() {
        val builder: AlertDialog.Builder
        builder = AlertDialog.Builder(Objects.requireNonNull<androidx.fragment.app.FragmentActivity>(activity), R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.gpx))
                .setMessage(getString(R.string.discard_current_gpx))
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    if (mOverlayHelper != null) {
                        mOverlayHelper!!.clearGpx()
                        if (activity != null)
                            (activity as AppCompatActivity).supportInvalidateOptionsMenu()
                    }
                    if (mListener != null) {
                        mListener!!.selectGpx()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which -> dialog.cancel() }
                .setIcon(R.drawable.ic_alert)
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.show()

    }

    fun zoomToBounds(box: BoundingBox) {
        if (mMapView!!.height > 0) {
            mMapView!!.zoomToBoundingBox(box, true, 64)
        } else {
            val vto = mMapView!!.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mMapView!!.zoomToBoundingBox(box, true, 64)
                    val vto = mMapView!!.viewTreeObserver
                    vto.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    fun setNearbyPlace() {
        val nearbyPlace = mListener!!.selectedNearbyPlace
        showNearbyPlace(nearbyPlace)
    }

    private fun showNearbyPlace(nearbyPlace: NearbyItem?) {
        //TODO: set overlay item
        mOverlayHelper!!.setNearby(nearbyPlace!!)
        animateToLatLon(nearbyPlace.lat, nearbyPlace.lon)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        mListener!!.setUpNavigation(false)
        inflater!!.inflate(R.menu.menu_main, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        if (mFollow) {
            menu!!.findItem(R.id.action_follow).isVisible = false
            menu.findItem(R.id.action_no_follow).isVisible = true
        } else {
            menu!!.findItem(R.id.action_follow).isVisible = true
            menu.findItem(R.id.action_no_follow).isVisible = false
        }
        if (mOverlayHelper != null && mOverlayHelper!!.hasGpx()) {
            menu.findItem(R.id.action_gpx_details).isVisible = true
            menu.findItem(R.id.action_gpx_zoom).isVisible = true
        } else {
            menu.findItem(R.id.action_gpx_details).isVisible = false
            menu.findItem(R.id.action_gpx_zoom).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_gpx -> {
                if (mOverlayHelper != null && mOverlayHelper!!.hasGpx()) {
                    showGpxdialog()
                } else {
                    mListener!!.selectGpx()
                }
                return true
            }
            R.id.action_location -> {
                if (mCurrentLocation != null) {
                    mMapView!!.controller.animateTo(GeoPoint(mCurrentLocation!!))
                }
                return true
            }
            R.id.action_follow -> {
                enableFollow()
                Toast.makeText(activity, R.string.follow_enabled, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_no_follow -> {
                disableFollow()
                Toast.makeText(activity, R.string.follow_disabled, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_gpx_details -> {
                if (mListener != null)
                    mListener!!.addGpxDetailFragment()
                return true
            }
            R.id.action_nearby -> {
                if (mListener != null)
                    if (mCurrentLocation != null) {
                        mListener!!.addNearbyFragment(mCurrentLocation!!)
                    } else {
                        Toast.makeText(activity, R.string.location_unknown, Toast.LENGTH_SHORT).show()
                    }
                return true
            }
            R.id.action_gpx_zoom -> {
                disableFollow()
                if (mListener?.gpx != null) {
                    zoomToBounds(Util.area(mListener?.gpx!!))
                }
                return true
            }
            R.id.action_layers -> if (activity != null) {
                val anchorView = activity!!.findViewById<View>(R.id.popupAnchorView)
                val popup = PopupMenu(activity, anchorView)
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.menu_tile_sources, popup.menu)

                val openTopoMapItem = popup.menu.findItem(R.id.otm)
                val openStreetMapItem = popup.menu.findItem(R.id.osm)
                val openStreetMapItemFr = popup.menu.findItem(R.id.osmfr)
                val overlayNoneItem = popup.menu.findItem(R.id.none)
                val overlayHikingItem = popup.menu.findItem(R.id.lonvia_hiking)
                val overlayCyclingItem = popup.menu.findItem(R.id.lonvia_cycling)

                when (mBaseMap) {
                    BASE_MAP_OTM -> openTopoMapItem.isChecked = true
                    BASE_MAP_OSM -> openStreetMapItem.isChecked = true
                    BASE_MAP_OSM_FR -> openStreetMapItemFr.isChecked = true
                }

                when (mOverlay) {
                    OverlayHelper.OVERLAY_NONE -> overlayNoneItem.isChecked = true
                    OverlayHelper.OVERLAY_HIKING -> overlayHikingItem.isChecked = true
                    OverlayHelper.OVERLAY_CYCLING -> overlayCyclingItem.isChecked = true
                }

                popup.setOnMenuItemClickListener(this@MapFragment)
                popup.show()
                return true
            } else {
                return false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Popup menu click
     *
     * @param menuItem
     * @return
     */
    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
            when (menuItem.itemId) {
                R.id.otm -> mBaseMap = BASE_MAP_OTM
                R.id.osm -> mBaseMap = BASE_MAP_OSM
                R.id.osmfr -> mBaseMap = BASE_MAP_OSM_FR
                R.id.none -> mOverlay = OverlayHelper.OVERLAY_NONE
                R.id.lonvia_hiking -> mOverlay = OverlayHelper.OVERLAY_HIKING
                R.id.lonvia_cycling -> mOverlay = OverlayHelper.OVERLAY_CYCLING
            }
            mPrefs!!.edit().putInt(PREF_BASE_MAP, mBaseMap).apply()
            mPrefs!!.edit().putInt(PREF_OVERLAY, mOverlay).apply()
            setBaseMap()
            setTilesOverlay()
        }
        return true
    }

    override fun onLocationChanged(location: Location) {
        mCurrentLocation = location
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {

    }

    override fun onProviderEnabled(s: String) {

    }

    override fun onProviderDisabled(s: String) {

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

    override fun onDestroy() {
        super.onDestroy()
        mLocationManager = null
        mCurrentLocation = null
        mLocationOverlay = null
        mCompassOverlay = null
        mScaleBarOverlay = null
        mRotationGestureOverlay = null
        mOverlayHelper!!.destroy()
    }


    interface OnFragmentInteractionListener {

        /**
         * Retrieve the current GPX
         *
         * @return Gpx
         */
        val gpx: Gpx?

        /**
         * Get selected nearby item to show on map
         *
         * @return NearbyItem
         */
        val selectedNearbyPlace: NearbyItem?

        /**
         * Start GPX file selection flow
         */
        fun selectGpx()

        /**
         * Request to set a GPX layer, e.g. after a configuration change
         */
        fun setGpx()

        /**
         * Present GPX details
         */
        fun addGpxDetailFragment()

        /**
         * Present nearby items
         */
        fun addNearbyFragment(location: Location)

        /**
         * Set up navigation arrow
         */
        fun setUpNavigation(upNavigation: Boolean)
    }

    companion object {

        internal const val PARAM_LATITUDE = "latitude"
        internal const val PARAM_LONGITUDE = "longitude"
        internal const val PARAM_NEARBY_PLACE = "nearby_place"
        private const val MAP_PREFS = "map_prefs"

        internal const val PREF_BASE_MAP = "base_map"
        internal const val PREF_OVERLAY = "overlay"

        internal const val BASE_MAP_OTM = 1
        internal const val BASE_MAP_OSM = 2
        internal const val BASE_MAP_OSM_FR = 3
        private val TAG = MapFragment::class.java.simpleName

        fun newInstance(): MapFragment {
            return MapFragment()
        }

        fun newInstance(lat: Double, lon: Double): MapFragment {
            val mapFragment = MapFragment()
            val arguments = Bundle()
            arguments.putDouble(PARAM_LATITUDE, lat)
            arguments.putDouble(PARAM_LONGITUDE, lon)
            mapFragment.arguments = arguments
            return mapFragment
        }

        fun newInstance(showNearbyPlace: Boolean): MapFragment {
            val mapFragment = MapFragment()
            val arguments = Bundle()
            arguments.putBoolean(PARAM_NEARBY_PLACE, showNearbyPlace)
            mapFragment.arguments = arguments
            return mapFragment
        }
    }
}// Required empty public constructor

