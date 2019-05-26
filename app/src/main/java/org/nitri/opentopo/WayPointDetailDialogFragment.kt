package org.nitri.opentopo

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.TextView

import org.nitri.opentopo.model.WayPointItem


class WayPointDetailDialogFragment : androidx.fragment.app.DialogFragment() {

    private var mCallback: Callback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if (requireActivity().supportFragmentManager != null)
            mCallback = requireActivity().supportFragmentManager.findFragmentByTag(MainActivity.GPX_DETAIL_FRAGMENT_TAG) as Callback?

        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater

        // Pass null as the parent view because it's going in the dialog layout
        @SuppressLint("InflateParams")
        val rootView = inflater.inflate(R.layout.fragment_way_point_detail, null)

        val tvName = rootView.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = rootView.findViewById<TextView>(R.id.tvDescription)

        tvDescription.movementMethod = LinkMovementMethod.getInstance()

        builder.setView(rootView)

        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        if (mCallback != null) {
            val item = mCallback!!.selectedWayPointItem
            if (item?.wayPoint != null) {
                tvName.text = item.wayPoint?.name
                tvDescription.text = Util.fromHtml(item.wayPoint?.desc?.replace("href=\"//", "href=\"http://") ?: "")
            }
        }

        return dialog
    }


    internal interface Callback {
        val selectedWayPointItem: WayPointItem?
    }
}
