package com.example.contentswiper.ui

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.example.contentswiper.ContentSwiperApp
import com.example.contentswiper.R
import com.example.contentswiper.model.Content

/**
 * Adapter for the card stack view.
 */
class CardStackAdapter(
    private val context: Context,
    private var contents: List<Content> = emptyList()
) : RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {
    
    private val TAG = "CardStackAdapter"
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        try {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater.inflate(R.layout.item_content_card, parent, false))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder: ${e.message}", e)
            if (context.applicationContext is ContentSwiperApp) {
                (context.applicationContext as ContentSwiperApp).logException("CardStackAdapter.onCreateViewHolder", e)
            }
            // Create a simple view as fallback
            val textView = TextView(parent.context)
            textView.text = "Error loading card: ${e.message}"
            textView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return ViewHolder(textView)
        }
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (position < 0 || position >= contents.size) {
                Log.e(TAG, "Invalid position: $position, contents size: ${contents.size}")
                return
            }
            
            val content = contents[position]
            holder.bind(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder at position $position: ${e.message}", e)
            if (context.applicationContext is ContentSwiperApp) {
                (context.applicationContext as ContentSwiperApp).logException("CardStackAdapter.onBindViewHolder", e)
            }
            holder.showError("Error displaying content: ${e.message}")
        }
    }
    
    override fun getItemCount(): Int = contents.size
    
    fun setItems(newContents: List<Content>) {
        try {
            Log.d(TAG, "Setting ${newContents.size} items")
            contents = newContents
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting items: ${e.message}", e)
            if (context.applicationContext is ContentSwiperApp) {
                (context.applicationContext as ContentSwiperApp).logException("CardStackAdapter.setItems", e)
            }
            Toast.makeText(context, "Error updating content: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Gets the content item at the specified position.
     */
    fun getItemAt(position: Int): Content? {
        return if (position in contents.indices) contents[position] else null
    }
    
    /**
     * Gets the content item at the specified position.
     * Alias for getItemAt for compatibility.
     */
    fun getContentAt(position: Int): Content {
        return contents[position]
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentImage: ImageView? = itemView.findViewById(R.id.contentImage)
        private val aiThoughtsText: TextView? = itemView.findViewById(R.id.aiThoughtsText)
        private val contentTitle: TextView? = itemView.findViewById(R.id.contentTitle)
        private val contentDescription: TextView? = itemView.findViewById(R.id.contentDescription)
        private val generatedByText: TextView? = itemView.findViewById(R.id.generatedByText)
        private val likeOverlay: ImageView? = itemView.findViewById(R.id.likeOverlay)
        private val rejectOverlay: ImageView? = itemView.findViewById(R.id.rejectOverlay)
        private val tagsContainer: ViewGroup? = itemView.findViewById(R.id.tagsContainer)
        
        // Card flipping elements
        private val cardView: CardView? = itemView.findViewById(R.id.contentCardView)
        private val cardFrontSide: ConstraintLayout? = itemView.findViewById(R.id.cardFrontSide)
        private val cardBackSide: ConstraintLayout? = itemView.findViewById(R.id.cardBackSide)
        private val tapToFlipText: TextView? = itemView.findViewById(R.id.tapToFlipText)
        private val tapToFlipBackText: TextView? = itemView.findViewById(R.id.tapToFlipBackText)
        private val creationSummaryText: TextView? = itemView.findViewById(R.id.creationSummaryText)
        
        // Track card state
        private var isShowingFront = true
        
        init {
            try {
                // Enable scrolling for AI thoughts
                aiThoughtsText?.movementMethod = ScrollingMovementMethod()
                // Enable scrolling for content description
                contentDescription?.movementMethod = ScrollingMovementMethod()
                
                // Set up card flipping
                cardView?.setOnClickListener {
                    flipCard()
                }
                
                // Allow tapping anywhere on the back of the card to flip back to front
                cardBackSide?.setOnClickListener {
                    if (!isShowingFront) {
                        flipCard()
                    }
                }
                
                tapToFlipBackText?.setOnClickListener {
                    flipCard()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in ViewHolder init: ${e.message}", e)
            }
        }
        
        fun bind(content: Content) {
            try {
                // Reset card to front side when binding new content
                resetCardToFront()
                
                // Bind front side content
                contentTitle?.text = content.title
                contentDescription?.text = content.description
                generatedByText?.text = itemView.context.getString(
                    R.string.generated_by_format, 
                    content.generatedBy
                )
                
                // Reset overlays
                likeOverlay?.alpha = 0f
                rejectOverlay?.alpha = 0f
                
                // Get AI thoughts from metadata if available
                val aiThoughts = content.metadata["aiThoughts"]
                
                // Load image if available, otherwise show AI thoughts
                if (!content.imageUrl.isNullOrEmpty() && contentImage != null) {
                    contentImage.visibility = View.VISIBLE
                    aiThoughtsText?.visibility = View.GONE
                    
                    try {
                        Glide.with(itemView.context)
                            .load(content.imageUrl)
                            .centerCrop()
                            .error(R.color.medium_gray)
                            .into(contentImage)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading image: ${e.message}", e)
                        contentImage.setImageResource(R.color.medium_gray)
                    }
                } else {
                    // No image available, show AI thoughts if we have them
                    if (!aiThoughts.isNullOrEmpty() && aiThoughtsText != null) {
                        contentImage?.visibility = View.GONE
                        aiThoughtsText.visibility = View.VISIBLE
                        aiThoughtsText.text = aiThoughts
                    } else {
                        // No image and no AI thoughts, show placeholder
                        contentImage?.visibility = View.VISIBLE
                        aiThoughtsText?.visibility = View.GONE
                        contentImage?.setImageResource(R.color.medium_gray)
                    }
                }
                
                // Display tags if available
                displayTags(content.tags)
                
                // Set creation summary for back of card
                val summary = content.creationSummary ?: "No creation details available for this content."
                creationSummaryText?.text = summary
                
                // Show or hide flip hint based on whether there's a creation summary
                tapToFlipText?.visibility = if (content.creationSummary != null) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Error binding content: ${e.message}", e)
                if (context.applicationContext is ContentSwiperApp) {
                    (context.applicationContext as ContentSwiperApp).logException("ViewHolder.bind", e)
                }
                showError("Error displaying content: ${e.message}")
            }
        }
        
        private fun displayTags(tags: List<String>) {
            try {
                tagsContainer?.let { container ->
                    container.removeAllViews()
                    
                    if (tags.isEmpty()) {
                        container.visibility = View.GONE
                        return
                    }
                    
                    container.visibility = View.VISIBLE
                    
                    for (tag in tags.take(3)) { // Limit to 3 tags
                        val tagView = LayoutInflater.from(context).inflate(
                            R.layout.item_tag, container, false
                        )
                        
                        val tagText = tagView.findViewById<TextView>(R.id.tagText)
                        tagText.text = tag
                        
                        container.addView(tagView)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying tags: ${e.message}", e)
                if (context.applicationContext is ContentSwiperApp) {
                    (context.applicationContext as ContentSwiperApp).logException("ViewHolder.displayTags", e)
                }
                // Just hide the tags container on error
                tagsContainer?.visibility = View.GONE
            }
        }
        
        /**
         * Flips the card to show either the front or back side.
         */
        private fun flipCard() {
            try {
                if (cardFrontSide == null || cardBackSide == null) {
                    Log.e(TAG, "Cannot flip card: front or back side is null")
                    return
                }
                
                // Simple flip animation
                if (isShowingFront) {
                    // Flip to back
                    cardFrontSide.visibility = View.GONE
                    cardBackSide.visibility = View.VISIBLE
                } else {
                    // Flip to front
                    cardFrontSide.visibility = View.VISIBLE
                    cardBackSide.visibility = View.GONE
                }
                
                isShowingFront = !isShowingFront
            } catch (e: Exception) {
                Log.e(TAG, "Error flipping card: ${e.message}", e)
            }
        }
        
        /**
         * Resets the card to show the front side.
         */
        private fun resetCardToFront() {
            try {
                if (cardFrontSide == null || cardBackSide == null) return
                
                cardFrontSide.visibility = View.VISIBLE
                cardBackSide.visibility = View.GONE
                isShowingFront = true
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting card: ${e.message}", e)
            }
        }
        
        fun showLikeOverlay(show: Boolean) {
            likeOverlay?.alpha = if (show) 1f else 0f
        }
        
        fun showRejectOverlay(show: Boolean) {
            rejectOverlay?.alpha = if (show) 1f else 0f
        }
        
        fun showError(errorMessage: String) {
            try {
                contentTitle?.text = "Error"
                contentDescription?.text = errorMessage
                contentImage?.setImageResource(R.color.medium_gray)
                contentImage?.visibility = View.VISIBLE
                aiThoughtsText?.visibility = View.GONE
                tagsContainer?.visibility = View.GONE
                generatedByText?.text = "Error occurred"
                tapToFlipText?.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Error showing error state: ${e.message}", e)
            }
        }
    }
} 