package com.example.contentswiper.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contentswiper.R
import com.example.contentswiper.model.Content

/**
 * Adapter for the card stack view.
 */
class CardStackAdapter(
    private var contents: List<Content> = emptyList()
) : RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_content_card, parent, false))
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = contents[position]
        holder.bind(content)
    }
    
    override fun getItemCount(): Int = contents.size
    
    fun setContents(newContents: List<Content>) {
        contents = newContents
        notifyDataSetChanged()
    }
    
    fun getContentAt(position: Int): Content? {
        return if (position in contents.indices) contents[position] else null
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentImage: ImageView = itemView.findViewById(R.id.contentImage)
        private val contentTitle: TextView = itemView.findViewById(R.id.contentTitle)
        private val contentDescription: TextView = itemView.findViewById(R.id.contentDescription)
        private val generatedByText: TextView = itemView.findViewById(R.id.generatedByText)
        private val likeOverlay: ImageView = itemView.findViewById(R.id.likeOverlay)
        private val rejectOverlay: ImageView = itemView.findViewById(R.id.rejectOverlay)
        
        fun bind(content: Content) {
            contentTitle.text = content.title
            contentDescription.text = content.description
            generatedByText.text = itemView.context.getString(R.string.generated_by)
            
            // Reset overlays
            likeOverlay.alpha = 0f
            rejectOverlay.alpha = 0f
            
            // Load image if available, otherwise use a placeholder
            if (content.imageUrl != null) {
                Glide.with(itemView.context)
                    .load(content.imageUrl)
                    .centerCrop()
                    .into(contentImage)
            } else {
                // Use a placeholder image
                contentImage.setImageResource(R.color.medium_gray)
            }
        }
        
        fun showLikeOverlay(show: Boolean) {
            likeOverlay.alpha = if (show) 1f else 0f
        }
        
        fun showRejectOverlay(show: Boolean) {
            rejectOverlay.alpha = if (show) 1f else 0f
        }
    }
} 