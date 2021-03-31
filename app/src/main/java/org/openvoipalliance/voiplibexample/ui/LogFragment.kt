package org.openvoipalliance.voiplibexample.ui

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_dialer.*
import kotlinx.android.synthetic.main.fragment_log.*
import kotlinx.android.synthetic.main.log_view.view.*
import org.openvoipalliance.voiplibexample.R
import org.openvoipalliance.voiplibexample.logging.LogEntry
import org.openvoipalliance.voiplibexample.logging.LogViewModel
import java.util.*


class LogFragment : Fragment() {

    private lateinit var logViewModel: LogViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_log, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logViewModel = ViewModelProviders.of(this).get(LogViewModel::class.java)

        // Specify layout for recycler view
        val linearLayoutManager = LinearLayoutManager(
                requireContext(), RecyclerView.VERTICAL, false)
        logContainer.layoutManager = linearLayoutManager

        // Observe the model
        logViewModel.allLogs.observe(requireActivity(), { logs ->
            if (isAdded) {
                logContainer.adapter = LogAdapter(logs)
            }
        })

    }
}

class LogAdapter(val logs: List<LogEntry>): RecyclerView.Adapter<LogAdapter.LogHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.log_view, null)
        return LogHolder(itemView = v)
    }

    override fun onBindViewHolder(holder: LogHolder, position: Int) {
        holder.datetime.text = logs[position].datetime
        holder.message.text = logs[position].message
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    override fun getItemCount(): Int = logs.size

    class LogHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val datetime = itemView.datetime
        val message = itemView.message
    }
}