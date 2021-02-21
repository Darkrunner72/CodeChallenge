package com.example.codechallenge

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.search_result.view.*

class SearchResults(val activity: MapsActivity, val items : ArrayList<SearchResult>, val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }

    /*// Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.search_result, parent, false))
    }

    // Binds each animal in the ArrayList to a view
    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder?.tvName?.text = items.get(position)
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //return ViewHolder(LayoutInflater.from(context).inflate(R.layout.search_result, parent, false))

        val view = LayoutInflater.from(context).inflate(R.layout.search_result, parent, false)
        return ViewHolder(view).listen { pos, type ->
            val item = items.get(pos)
            var search = R.id.etSearch
            var text: String = ""
            if(item.name.length>0){
                text+=item.name
            }
            if(item.state.length>0){
                text+=", "+item.state
            }
            if(item.country.length>0){
                text+=", "+item.country
            }
            activity.updateSearch(text)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val temp = items.get(position)
        holder?.tvName?.text = temp.name
        holder?.tvState?.text = temp.state
        holder?.tvCountry?.text = temp.country
    }
}

class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val tvName = view.tvName
    val tvState = view.tvState
    val tvCountry = view.tvCountry

    fun listen(event: (position: Int, type: Int) -> Unit): ViewHolder {
        itemView.setOnClickListener {
            event.invoke(getAdapterPosition(), getItemViewType())
        }
        return this
    }
}