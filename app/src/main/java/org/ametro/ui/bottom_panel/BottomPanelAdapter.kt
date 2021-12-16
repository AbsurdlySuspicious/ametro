package org.ametro.ui.bottom_panel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.ametro.databinding.WidgetItemBotStationBinding

interface PanelAdapterBinder<V : View, B : ViewBinding> {
    fun bindItem(view: V, bind: B)
}

private abstract class BaseItem(
    val viewType: Int,
    val priority: Int
)

private object RouteItem : BaseItem(BottomPanelAdapter.TYPE_ROUTE, 1)
private object StationItem : BaseItem(BottomPanelAdapter.TYPE_STATION, 2)

class PanelHolder(val viewType: Int, view: View, val binding: ViewBinding) : RecyclerView.ViewHolder(view)

class BottomPanelAdapter(context: Context) : RecyclerView.Adapter<PanelHolder>() {
    companion object {
        const val TYPE_ROUTE = 1
        const val TYPE_STATION = 2
    }

    private val inflater = LayoutInflater.from(context)
    private lateinit var recyclerView: RecyclerView

    private val itemList: MutableList<BaseItem> = mutableListOf()

    var showRoute: Boolean = false
        set(value) {
            field = value; refreshList()
        }
    var routeBinder: PanelAdapterBinder<ViewGroup, ViewBinding>? = null
    var showStation: Boolean = false
        set(value) {
            field = value; refreshList()
        }
    var stationBinder: PanelAdapterBinder<ConstraintLayout, WidgetItemBotStationBinding>? = null

    private fun refreshList() {
        if (showRoute)
            itemList.add(RouteItem)
        else
            itemList.removeAll { it.viewType == TYPE_ROUTE }

        if (showStation)
            itemList.add(StationItem)
        else
            itemList.removeAll { it.viewType == TYPE_STATION }

        itemList.sortBy { it.priority }
        this.notifyDataSetChanged()
    }

    fun topmostHeight(): Int {
        return recyclerView.findViewHolderForLayoutPosition(0)?.itemView?.height ?: 0
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return itemList[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelHolder {
        return when (viewType) {
            TYPE_STATION -> {
                val bind = WidgetItemBotStationBinding.inflate(inflater, parent, false)
                PanelHolder(viewType, bind.root, bind)
            }
            TYPE_ROUTE -> TODO()
            else -> throw Exception("Unknown view $viewType")
        }
    }

    override fun onBindViewHolder(holder: PanelHolder, position: Int) {
        when (holder.viewType) {
            TYPE_STATION -> stationBinder?.bindItem(
                holder.itemView as ConstraintLayout,
                holder.binding as WidgetItemBotStationBinding
            )
            TYPE_ROUTE -> TODO()
        }
    }

}
