package org.nitri.opentopo.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.TextView
import org.jetbrains.anko.doAsync

import org.nitri.opentopo.R
import org.nitri.opentopo.model.WayPointHeaderItem
import org.nitri.opentopo.model.WayPointItem
import org.nitri.opentopo.model.WayPointListItem

class WayPointListAdapter(private val mItems: List<WayPointListItem>, private val mListener: OnItemClickListener) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return mItems[position].listItemType
    }
    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val view: View
        when (type) {
            WayPointListItem.HEADER -> {
                view = LayoutInflater
                        .from(viewGroup.context)
                        .inflate(R.layout.way_point_header_item, viewGroup, false)
                return ViewHolderHeader(view)
            }
            WayPointListItem.WAY_POINT -> {
                view = LayoutInflater
                        .from(viewGroup.context)
                        .inflate(R.layout.way_point_item, viewGroup, false)
                return ViewHolderWayPoint(view)
            }
            // TODO Find a better solution
            else -> {
                throw Exception()
            }
        }
    }

    override fun onBindViewHolder(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
        val item = mItems[pos]
        (viewHolder as ViewHolder).bindType(item)
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    abstract inner class ViewHolder internal constructor(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        abstract fun bindType(item: WayPointListItem)
    }

    inner class ViewHolderHeader internal constructor(itemView: View) : ViewHolder(itemView) {
        private val textView: TextView

        init {
            textView = itemView.findViewById(R.id.textView)
        }

        override fun bindType(item: WayPointListItem) {
            textView.text = (item as WayPointHeaderItem).header
        }
    }

    inner class ViewHolderWayPoint internal constructor(itemView: View) : ViewHolder(itemView), View.OnClickListener {
        private val textView: TextView

        init {
            textView = itemView.findViewById(R.id.textView)
            itemView.setOnClickListener(this)
        }

        override fun bindType(item: WayPointListItem) {
            textView.text = (item as WayPointItem).wayPoint!!.name
            val itemIndex = mItems.indexOf(item)
            itemView.tag = itemIndex
        }

        override fun onClick(view: View) {
            mListener.onItemClick(view.tag as Int)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(index: Int)
    }
}