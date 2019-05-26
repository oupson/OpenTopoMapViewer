package org.nitri.opentopo

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.widget.Toast

import net.danlew.android.joda.JodaTimeAndroid

import org.nitri.opentopo.nearby.entity.NearbyItem
import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.io.InputStream

import de.k3b.geo.api.GeoPointDto
import de.k3b.geo.io.GeoUri
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx


class MainActivity : AppCompatActivity(), MapFragment.OnFragmentInteractionListener, GpxDetailFragment.OnFragmentInteractionListener, NearbyFragment.OnFragmentInteractionListener {
    private var mGeoPointFromIntent: GeoPointDto? = null
    private var mGpxUriString: String? = null
    private var mGpxUri: Uri? = null
    override var gpx: Gpx? = null
        private set(value: Gpx?) {
            field = value
        }
    private var mZoomToGpx: Boolean = false
    override var selectedNearbyPlace: NearbyItem? = null
        private set(value: NearbyItem?) {
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState != null) {
            mGpxUriString = savedInstanceState.getString(GPX_URI_STATE)
        }

        val intent = intent

        if (intent != null && intent.data != null) {
            handleIntent(intent)
        }

        JodaTimeAndroid.init(this)
        // TODO ADD MULTIPLE REQUEST
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
        } else {
            addMapFragment()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.data != null) {
            val scheme = intent.data!!.scheme
            if (scheme != null) {
                when (scheme) {
                    "geo" -> mGeoPointFromIntent = getGeoPointDtoFromIntent(intent)
                    "file" -> {
                        mGpxUri = intent.data
                        mGpxUriString = mGpxUri!!.toString()
                        Log.i(TAG, "Uri: " + mGpxUriString!!)
                        mZoomToGpx = true
                    }
                    "content" -> {
                        mGpxUri = intent.data
                        mGpxUriString = mGpxUri!!.toString()
                        Log.i(TAG, "Uri: " + mGpxUriString!!)
                        mZoomToGpx = true
                    }
                }
            }
        }
    }


    private fun addMapFragment() {
        if (mapFragmentAdded()) {
            return
        }
        var mapFragment: MapFragment?
        if (mGeoPointFromIntent == null) {
            mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
            if (mapFragment == null) {
                mapFragment = MapFragment.newInstance()
            }
        } else {
            mapFragment = MapFragment.newInstance(mGeoPointFromIntent!!.latitude, mGeoPointFromIntent!!.longitude)
        }
        supportFragmentManager.beginTransaction()
                .add(R.id.map_container, mapFragment, MAP_FRAGMENT_TAG)
                .commit()
        setGpx()
    }

    private fun mapFragmentAdded(): Boolean {
        return supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) != null
    }

    override fun addGpxDetailFragment() {
        val gpxDetailFragment = GpxDetailFragment.newInstance()
        supportFragmentManager.beginTransaction().addToBackStack(null)
                .replace(R.id.map_container, gpxDetailFragment, GPX_DETAIL_FRAGMENT_TAG)
                .commit()
    }

    override fun addNearbyFragment(location: Location) {
        val gpxDetailFragment = NearbyFragment.newInstance(location.latitude, location.longitude)
        supportFragmentManager.beginTransaction().addToBackStack(null)
                .replace(R.id.map_container, gpxDetailFragment, NEARBY_FRAGMENT_TAG)
                .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
                } else {
                    addMapFragment()
                }
            }
            REQUEST_STORAGE_PERMISSION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addMapFragment()
            } else {
                finish()
            }
        }
    }

    override fun selectGpx() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun setGpx() {
        if (!TextUtils.isEmpty(mGpxUriString)) {
            parseGpx(Uri.parse(mGpxUriString))
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int,
                                         resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            if (resultData != null) {
                mGpxUri = resultData.data
                mZoomToGpx = true
                if (mGpxUri != null) {
                    Log.i(TAG, "Uri: " + mGpxUri!!.toString())
                    parseGpx(mGpxUri!!)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!TextUtils.isEmpty(mGpxUriString)) {
            outState.putString(GPX_URI_STATE, mGpxUriString)
        }
        super.onSaveInstanceState(outState)
    }

    override fun setUpNavigation(upNavigation: Boolean) {
        if (supportFragmentManager != null && supportActionBar != null) {
            if (upNavigation) {
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                supportActionBar!!.setDisplayShowHomeEnabled(true)
            } else {
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                supportActionBar!!.setDisplayShowHomeEnabled(false)
            }
        }
    }

    private fun parseGpx(uri: Uri) {
        val parser = GPXParser()
        val contentResolver = contentResolver
        if (contentResolver != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    gpx = parser.parse(inputStream)
                    val mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
                    if (mapFragment != null && gpx != null) {
                        mapFragment.setGpx(gpx!!, mZoomToGpx)
                        mGpxUriString = uri.toString()
                        mZoomToGpx = false
                    }
                }

            } catch (e: XmlPullParserException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.invalid_gpx) + ": " + e.message,
                        Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.invalid_gpx) + ": " + e.message, Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun getGeoPointDtoFromIntent(intent: Intent?): GeoPointDto? {
        val uri = intent?.data
        val uriAsString = uri?.toString()
        var pointFromIntent: GeoPointDto? = null
        if (uriAsString != null) {
            val parser = GeoUri(GeoUri.OPT_PARSE_INFER_MISSING)
            pointFromIntent = parser.fromUri(uriAsString, GeoPointDto())
        }
        return pointFromIntent
    }

    override fun showNearbyPlace(nearbyItem: NearbyItem) {
        selectedNearbyPlace = nearbyItem
        supportFragmentManager.popBackStack()
        addMapFragment()
        val mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as MapFragment?
        mapFragment?.setNearbyPlace()
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName

        const val MAP_FRAGMENT_TAG = "map_fragment"
        const val GPX_DETAIL_FRAGMENT_TAG = "gpx_detail_fragment"
        const val WAY_POINT_DETAIL_FRAGMENT_TAG = "way_point_detail_fragment"
        const val NEARBY_FRAGMENT_TAG = "nearby_fragment"

        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val REQUEST_STORAGE_PERMISSION = 2
        private const val READ_REQUEST_CODE = 69

        private const val GPX_URI_STATE = "gpx_uri"
    }
}