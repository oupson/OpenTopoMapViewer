package org.nitri.opentopo.nearby.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.squareup.picasso.Picasso

import org.nitri.opentopo.R
import org.nitri.opentopo.nearby.entity.NearbyItem

class NearbyAdapter(private val mItems: List<NearbyItem>, private val mListener: NearbyAdapter.OnItemClickListener) : androidx.recyclerview.widget.RecyclerView.Adapter<NearbyAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ItemViewHolder {
        val view = LayoutInflater
                .from(viewGroup.context)
                .inflate(R.layout.nearby_item, viewGroup, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val item = mItems[position]
        Picasso.get().load(item.thumbnail).placeholder(R.drawable.ic_place).resize(60, 60).centerCrop().into(viewHolder.ivThumb)
        viewHolder.tvTitle.text = item.title
        viewHolder.tvDescription.text = item.description
        if (position == mItems.size - 1)
            viewHolder.divider.visibility = View.GONE
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun getItemId(position: Int): Long {
        return java.lang.Long.valueOf(mItems[position].pageid!!)
    }

    inner class ItemViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var ivThumb: ImageView
        var tvTitle: TextView
        var tvDescription: TextView
        var ivMap: ImageView
        var divider: View

        init {
            ivThumb = itemView.findViewById(R.id.ivThumb)
            tvTitle = itemView.findViewById(R.id.tvTitle)
            tvDescription = itemView.findViewById(R.id.tvDescription)
            ivMap = itemView.findViewById(R.id.ivMap)
            ivMap.setOnClickListener(this)
            divider = itemView.findViewById(R.id.divider)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.ivMap) {
                mListener.onMapItemClick(adapterPosition)
            } else {
                mListener.onItemClick(adapterPosition)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(index: Int)
        fun onMapItemClick(index: Int)
    }

}
