package org.nitri.opentopo.nearby.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.nearby.repo.NearbyRepository

/**
 * Created by helfrich on 25/02/2018.
 */

class NearbyViewModel : ViewModel() {

    private var mRepository: NearbyRepository? = null

    private var mItems: LiveData<List<NearbyItem>>? = null

    val items: LiveData<List<NearbyItem>>?
        get() {
            mItems = mRepository!!.loadNearbyItems()
            return mItems
        }

    fun setRepository(repository: NearbyRepository) {
        mRepository = repository
    }

}
