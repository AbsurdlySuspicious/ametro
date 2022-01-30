package org.ametro.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import org.ametro.R
import org.ametro.databinding.FragmentLanguageDialogBinding
import org.ametro.databinding.FragmentLanguageDialogItemBinding

data class LanguageListItem(
    val name: String,
    val code: String,
    val srcName: String
) {
    fun match(s: CharSequence): Boolean =
        listOf(code, name, srcName).any { it.contains(s, true) }
}

class LanguageListAdapter(val items: List<LanguageListItem>, private val inflater: LayoutInflater) : BaseAdapter() {
    private var filtered: List<LanguageListItem> = items

    override fun getCount() = filtered.size
    override fun getItem(position: Int): Any = filtered[position]
    override fun getItemId(position: Int): Long = position.toLong()
    fun getFilteredItem(pos: Int): LanguageListItem = filtered[pos]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val itemView = convertView ?: inflater.inflate(R.layout.fragment_language_dialog_item, parent, false)
        val binding = FragmentLanguageDialogItemBinding.bind(itemView)
        val item = filtered[position]
        binding.langCode.text = item.code
        binding.langName.text = item.name
        return itemView
    }

    fun setSearch(search: CharSequence?) {
        filtered = if (search == null || search == "")
            items
        else {
            val list = ArrayList<LanguageListItem>()
            items.forEach {
                if (it.match(search))
                    list.add(it)
            }
            list
        }
        this.notifyDataSetChanged()
    }
}

interface LanguageDialogListener {
    fun onResult(selectedItem: LanguageListItem?, forceDefault: Boolean, confirmed: Boolean)
}

class LanguageDialogFragment(
    private val languages: List<LanguageListItem>,
    private val listener: LanguageDialogListener?
) : DialogFragment() {
    constructor() : this(listOf(), null)

    private var selectedItem: LanguageListItem? = null
    private var forceDefault: Boolean = false
    private var confirmed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (listener == null)
            dismiss()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val layout = FragmentLanguageDialogBinding.inflate(activity.layoutInflater, null, false)
        val adapter = LanguageListAdapter(languages, activity.layoutInflater)

        val callListener = {
            listener?.onResult(selectedItem, forceDefault, confirmed)
        }

        val dialog = AlertDialog.Builder(activity)
            .setView(layout.root)
            .setPositiveButton("OK") { _, _ ->
                confirmed = true
                callListener()
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .create()

        layout.langList.also {
            it.adapter = adapter
            it.choiceMode = ListView.CHOICE_MODE_SINGLE
            it.setOnItemClickListener { _, _, pos, _ ->
                val item = adapter.getFilteredItem(pos)
                selectedItem = item
                it.setItemChecked(pos, true)
            }
        }
        layout.search.doOnTextChanged { text, _, _, _ ->
            adapter.setSearch(text)
            layout.langList.setItemChecked(-1, true)
            selectedItem = null
        }
        layout.useDefault.setOnClickListener {
            selectedItem = null
            forceDefault = true
            callListener()
            dialog.dismiss()
        }

        return dialog
    }
}
