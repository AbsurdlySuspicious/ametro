package org.ametro.ui.navigation.adapter

import android.content.Context
import org.ametro.ui.navigation.entities.NavigationItem
import android.widget.BaseAdapter
import android.view.LayoutInflater
import android.view.View
import org.ametro.ui.navigation.entities.NavigationHeader
import org.ametro.ui.navigation.entities.NavigationTextItem
import org.ametro.ui.navigation.entities.NavigationSplitter
import org.ametro.ui.navigation.entities.NavigationSubHeader
import org.ametro.ui.navigation.entities.NavigationCheckBoxItem
import android.view.ViewGroup
import org.ametro.ui.navigation.entities.INavigationItemGroup
import java.util.ArrayList
import java.util.HashMap

class NavigationDrawerAdapter(context: Context, items: Array<NavigationItem>) : BaseAdapter() {
    private val items: MutableList<NavigationItem>
    private val inflater: LayoutInflater
    private val viewItemTypes: MutableMap<Class<*>, Int> = HashMap()
    private val viewItemHolderFactories: MutableMap<Class<*>, IHolderFactory> = HashMap()

    init {
        viewItemTypes[NavigationHeader::class.java] = 0
        viewItemTypes[NavigationTextItem::class.java] = 1
        viewItemTypes[NavigationSplitter::class.java] = 2
        viewItemTypes[NavigationSubHeader::class.java] = 3
        viewItemTypes[NavigationCheckBoxItem::class.java] = 4
        viewItemHolderFactories[NavigationHeader::class.java] = NavigationHeaderHolderFactory()
        viewItemHolderFactories[NavigationTextItem::class.java] = NavigationTextItemHolderFactory()
        viewItemHolderFactories[NavigationSplitter::class.java] = NavigationSplitterHolderFactory()
        viewItemHolderFactories[NavigationSubHeader::class.java] = NavigationSubHeaderHolderFactory()
        viewItemHolderFactories[NavigationCheckBoxItem::class.java] = NavigationCheckBoxItemHolderFactory()
        inflater = LayoutInflater.from(context)
        this.items = flattenItems(items)
    }

    fun setNavigationItems(items: Array<NavigationItem>) {
        this.items.clear()
        this.items.addAll(flattenItems(items))
        notifyDataSetChanged()
    }

    fun getPositionByTag(tag: String): Int =
        items.indexOfFirst { it.tag == tag }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun isEnabled(position: Int): Boolean {
        return items[position].isEnabled
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): NavigationItem {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: IHolder
        if (view == null) {
            val factory = viewItemHolderFactories[items[position].javaClass]!!
            view = factory.createView(inflater, parent)!!
            holder = factory.createHolder(view)
        } else {
            holder = view.tag as IHolder
        }
        holder.update(items[position])
        return view
    }

    override fun getItemViewType(position: Int): Int {
        return viewItemTypes[items[position].javaClass]!!
    }

    override fun getViewTypeCount(): Int {
        return viewItemTypes.size
    }

    override fun isEmpty(): Boolean {
        return items.size == 0
    }

    private fun flattenItems(items: Array<NavigationItem>): MutableList<NavigationItem> {
        val flattenList: MutableList<NavigationItem> = ArrayList()
        for (item in items) {
            flattenList.add(item)
            if (item is INavigationItemGroup) {
                val group = item as INavigationItemGroup
                flattenList.addAll(flattenItems(group.items))
            }
        }
        return flattenList
    }
}