package org.nitri.opentopo

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import org.nitri.opentopo.nearby.adapter.NearbyAdapter
import org.nitri.opentopo.nearby.api.NearbyDatabase
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiApi
import org.nitri.opentopo.nearby.da.NearbyDao
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.nearby.repo.NearbyRepository
import org.nitri.opentopo.nearby.viewmodel.NearbyViewModel

import java.util.ArrayList
import java.util.Collections

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class NearbyFragment : androidx.fragment.app.Fragment(), NearbyAdapter.OnItemClickListener {

    private var mListener: OnFragmentInteractionListener? = null
    private var mLatitude: Double = 0.toDouble()
    private var mLongitude: Double = 0.toDouble()

    internal var gson = GsonBuilder().setLenient().create()

    internal var retrofit = Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    internal var mNearbyItems: MutableList<NearbyItem> = ArrayList()
    private var mNearbyAdapter: NearbyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        if (arguments != null) {
            mLatitude = arguments!!.getDouble(PARAM_LATITUDE)
            mLongitude = arguments!!.getDouble(PARAM_LONGITUDE)
        }

        mNearbyAdapter = NearbyAdapter(mNearbyItems, this)
        mNearbyAdapter!!.setHasStableIds(true)

        val nearbyViewModel = ViewModelProviders.of(requireActivity()).get(NearbyViewModel::class.java)

        val api = retrofit.create(MediaWikiApi::class.java)
        val dao = NearbyDatabase.getDatabase(activity!!).nearbyDao()
        val nearbyRepository = NearbyRepository(dao, api, mLatitude, mLongitude)

        nearbyViewModel.setRepository(nearbyRepository)

        val nearbyObserver = Observer<List<NearbyItem>> { items ->
            mNearbyItems.clear()
            if (items != null) {
                mNearbyItems.addAll(items)
                setDistance()
                mNearbyItems.sort()
                mNearbyAdapter!!.notifyDataSetChanged()
            }
        }

        nearbyViewModel.items?.observe(this, nearbyObserver)

    }

    private fun setDistance() {
        for (item in mNearbyItems) {
            val distance = Math.round(Util.distance(mLatitude, mLongitude, item.lat, item.lon)).toInt()
            item.distance = distance
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_nearby, container, false)
        val nearbyRecyclerView = rootView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.nearby_recycler_view)
        nearbyRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        if (nearbyRecyclerView.itemAnimator != null)
            nearbyRecyclerView.itemAnimator!!.changeDuration = 0
        nearbyRecyclerView.adapter = mNearbyAdapter
        return rootView
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

    override fun onItemClick(index: Int) {
        val uri = Uri.parse(mNearbyItems[index].url)
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(browserIntent)
    }

    override fun onMapItemClick(index: Int) {
        mListener!!.showNearbyPlace(mNearbyItems[index])
    }

    interface OnFragmentInteractionListener {

        /**
         * Show nearby item on map
         *
         * @param nearbyItem
         */
        fun showNearbyPlace(nearbyItem: NearbyItem)

        /**
         * Set up navigation arrow
         */
        fun setUpNavigation(upNavigation: Boolean)
    }

    companion object {
        internal val PARAM_LATITUDE = "latitude"
        internal val PARAM_LONGITUDE = "longitude"

        private val TAG = NearbyFragment::class.java.simpleName

        private val URL = "https://en.wikipedia.org/"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param lat latitute
         * @param lon longitude
         * @return A new instance of fragment NearbyFragment.
         */
        fun newInstance(lat: Double, lon: Double): NearbyFragment {
            val fragment = NearbyFragment()
            val arguments = Bundle()
            arguments.putDouble(PARAM_LATITUDE, lat)
            arguments.putDouble(PARAM_LONGITUDE, lon)
            fragment.arguments = arguments
            return fragment
        }
    }
}// Required empty public constructor
