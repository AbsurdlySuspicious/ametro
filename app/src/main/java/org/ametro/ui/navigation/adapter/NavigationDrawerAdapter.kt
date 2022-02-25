package org.ametro.ui.navigation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import org.ametro.ui.navigation.entities.*

class NavigationDrawerAdapter(context: Context, items: Array<NavigationItem>) : BaseAdapter() {
    private val items: MutableList<NavigationItem>
    private val inflater: LayoutInflater
    private val viewItemTypes = HashMap<Class<*>, Int>().also {
        it[NavigationHeader::class.java] = 0
        it[NavigationTextItem::class.java] = 1
        it[NavigationSubHeader::class.java] = 2
        it[NavigationCheckBoxItem::class.java] = 3
        it[NavigationPaddingItem::class.java] = 4
    }

    init {
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
        val holder: Holder
        if (view == null) {
            val factory = items[position].factory
            view = factory.createView(inflater, parent)!!
            holder = factory.createHolder(view)
        } else {
            holder = view.tag as Holder
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
            if (item is NavigationItemGroup) {
                val group = item as NavigationItemGroup
                flattenList.addAll(flattenItems(group.items))
            }
        }
        return flattenList
    }
}