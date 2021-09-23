package dev.dokup.mediastoresample.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.dokup.mediastoresample.databinding.ItemImageBinding
import dev.dokup.mediastoresample.entity.ImageEntity

class GridAdapter : ListAdapter<ImageEntity, GridAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: ImageEntity) {
            binding.run {
                Glide.with(binding.image)
                    .load(entity.uri)
                    .into(binding.image)
                executePendingBindings()
            }
        }
    }

    var onItemClickListener: ((ImageEntity, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemImageBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.binding.root.setOnClickListener {
            onItemClickListener?.invoke(getItem(position), position)
        }
    }
}

private object DiffCallback : DiffUtil.ItemCallback<ImageEntity>() {
    override fun areItemsTheSame(oldItem: ImageEntity, newItem: ImageEntity): Boolean {
        return oldItem.uri.toString() == newItem.uri.toString()
    }

    override fun areContentsTheSame(oldItem: ImageEntity, newItem: ImageEntity): Boolean {
        return oldItem == newItem
    }

}
