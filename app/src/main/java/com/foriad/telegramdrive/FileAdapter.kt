package com.foriad.telegramdrive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Date

class FileAdapter(
    private var items: List<DriveFile>,
    private val onClick: (DriveFile) -> Unit,
    private val onLongClick: (DriveFile) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.fileIcon)
        val name: TextView = view.findViewById(R.id.fileName)
        val meta: TextView = view.findViewById(R.id.fileMeta)
    }

    fun submit(list: List<DriveFile>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = items[position]
        holder.icon.text = when (f.kind) {
            "photo" -> "🖼️"; "video" -> "🎬"; "audio" -> "🎵"
            "note", "link" -> "📝"; else -> "📄"
        }
        holder.name.text = f.name
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(f.uploadedAt))
        val size = if (f.sizeBytes > 0) formatSize(f.sizeBytes) + " • " else ""
        holder.meta.text = "$size${f.folder} • $date"
        holder.itemView.setOnClickListener { onClick(f) }
        holder.itemView.setOnLongClickListener { onLongClick(f); true }
    }

    companion object {
        fun formatSize(bytes: Long): String = when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
