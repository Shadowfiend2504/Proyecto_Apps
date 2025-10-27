package com.example.healthconnectai.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.healthconnectai.data.model.Tarea
import com.example.healthconnectai.databinding.ItemTareaBinding

class TareaAdapter :
    ListAdapter<Tarea, TareaAdapter.TareaViewHolder>(DiffCallback()) {

    class TareaViewHolder(private val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(tarea: Tarea) {
            binding.txtTitulo.text = tarea.titulo
            binding.txtDescripcion.text = tarea.descripcion
            binding.txtFecha.text = tarea.fecha
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Tarea>() {
        override fun areItemsTheSame(oldItem: Tarea, newItem: Tarea) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Tarea, newItem: Tarea) = oldItem == newItem
    }
}
