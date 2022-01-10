package org.ametro.ui.bottom_panel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.ametro.databinding.WidgetItemBotRouteBinding
import org.ametro.databinding.WidgetItemBotStationBinding

interface PanelAdapterBinder {
    fun bindItem(bind: ViewBinding)
    fun attachItem(holder: PanelHolder) {}
    fun detachItem(holder: PanelHolder) {}
}

private abstract class BaseItem(
    val viewType: Int
)

private object RouteItem : BaseItem(BottomPanelAdapter.TYPE_ROUTE)
private object StationItem : BaseItem(BottomPanelAdapter.TYPE_STATION)

class PanelHolder(val viewType: Int, view: View, val binding: ViewBinding) : RecyclerView.ViewHolder(view)

class BottomPanelAdapter(context: Context) : RecyclerView.Adapter<PanelHolder>() {
    companion object {
        const val TYPE_ROUTE = 1
        const val TYPE_STATION = 2
    }

    private val inflater = LayoutInflater.from(context)
    private lateinit var recyclerView: RecyclerView

    private var itemList: MutableList<BaseItem> = mutableListOf()

    var showRoute: Boolean = false
        set(value) {
            field = value; refreshList()
        }
    var routeBinder: PanelAdapterBinder? = null
    var showStation: Boolean = false
        set(value) {
            field = value; refreshList()
        }
    var stationBinder: PanelAdapterBinder? = null

    private fun enableItem(old: List<BaseItem>, cond: Boolean, item: BaseItem) {
        val idxOld = old
            .indexOfFirst { it.viewType == item.viewType }
        if (cond) {
            val idxNew = itemList.size
            itemList.add(item)
            if (idxOld != idxNew) {
                if (idxOld > -1)
                    this.notifyItemRemoved(idxOld)
                this.notifyItemInserted(idxNew)
            } else
                this.notifyItemChanged(idxNew)
        } else if (idxOld > -1)
            this.notifyItemRemoved(idxOld)
    }

    private fun refreshList() {
        val old = this.itemList
        this.itemList = mutableListOf()

        enableItem(old, showRoute, RouteItem)
        enableItem(old, showStation, StationItem)

        (recyclerView.layoutManager as LinearLayoutManager)
            .scrollToPosition(0)
    }

    fun topmostView(): View? =
        recyclerView.findViewHolderForLayoutPosition(0)?.itemView

    fun topmostHeight(): Int =
        topmostView()?.height ?: 0

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
        val bind = when (viewType) {
            TYPE_STATION -> WidgetItemBotStationBinding.inflate(inflater, parent, false)
            TYPE_ROUTE -> WidgetItemBotRouteBinding.inflate(inflater, parent, false)
            else -> throw Exception("Unknown view $viewType")
        }
        return PanelHolder(viewType, bind.root, bind)
    }

    private fun <R> invokeBinder(viewType: Int, action: (PanelAdapterBinder?) -> R): R =
        when (viewType) {
            TYPE_STATION -> action(stationBinder)
            TYPE_ROUTE -> action(routeBinder)
            else -> throw Exception("Unknown view $viewType")
        }

    override fun onBindViewHolder(holder: PanelHolder, position: Int): Unit =
        invokeBinder(holder.viewType) { it?.bindItem(holder.binding) }

    override fun onViewAttachedToWindow(holder: PanelHolder): Unit =
        invokeBinder(holder.viewType) { it?.attachItem(holder) }

    override fun onViewDetachedFromWindow(holder: PanelHolder): Unit =
        invokeBinder(holder.viewType) { it?.detachItem(holder) }
}
