package com.example.contentswiper

import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.contentswiper.databinding.ActivityMainBinding
import com.example.contentswiper.model.Content
import com.example.contentswiper.ui.CardStackAdapter
import com.example.contentswiper.ui.MainViewModel
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeableMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CardStackListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var cardStackAdapter: CardStackAdapter
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    
    private val viewModel: MainViewModel by viewModels {
        val app = application as ContentSwiperApp
        MainViewModel.Factory(
            app.contentRepository,
            app.userPreferenceRepository,
            app.agentRepository,
            app.contentGenerator,
            app.agentManager
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupCardStackView()
        setupObservers()
        setupListeners()
        
        // Initialize default agents if needed
        CoroutineScope(Dispatchers.IO).launch {
            val app = application as ContentSwiperApp
            val agentCount = app.agentRepository.getActiveAgentCount()
            if (agentCount == 0) {
                app.agentManager.createDefaultAgents()
            }
        }
    }
    
    private fun setupCardStackView() {
        cardStackLayoutManager = CardStackLayoutManager(this, this).apply {
            setStackFrom(StackFrom.Top)
            setVisibleCount(3)
            setTranslationInterval(8.0f)
            setScaleInterval(0.95f)
            setSwipeThreshold(0.3f)
            setMaxDegree(20.0f)
            setDirections(Direction.HORIZONTAL)
            setCanScrollHorizontal(true)
            setCanScrollVertical(false)
            setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
            setOverlayInterpolator(LinearInterpolator())
        }
        
        cardStackAdapter = CardStackAdapter()
        
        binding.cardStackView.apply {
            layoutManager = cardStackLayoutManager
            adapter = cardStackAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.contents.observe(this, Observer { contents ->
            cardStackAdapter.setContents(contents)
        })
        
        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
        
        viewModel.isEmpty.observe(this, Observer { isEmpty ->
            binding.emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        })
    }
    
    private fun setupListeners() {
        binding.refreshButton.setOnClickListener {
            viewModel.loadContents()
        }
    }
    
    // CardStackListener implementation
    override fun onCardDragging(direction: Direction, ratio: Float) {
        val viewHolder = binding.cardStackView.findViewHolderForAdapterPosition(cardStackLayoutManager.topPosition) as? CardStackAdapter.ViewHolder
        
        when (direction) {
            Direction.Right -> {
                viewHolder?.showLikeOverlay(ratio > 0)
                viewHolder?.showRejectOverlay(false)
            }
            Direction.Left -> {
                viewHolder?.showLikeOverlay(false)
                viewHolder?.showRejectOverlay(ratio > 0)
            }
            else -> {
                viewHolder?.showLikeOverlay(false)
                viewHolder?.showRejectOverlay(false)
            }
        }
    }
    
    override fun onCardSwiped(direction: Direction) {
        val position = cardStackLayoutManager.topPosition - 1
        val content = cardStackAdapter.getContentAt(position) ?: return
        
        when (direction) {
            Direction.Right -> {
                // Like
                viewModel.recordPreference(content, true)
                binding.instructionText.text = getString(R.string.swipe_left_to_reject)
            }
            Direction.Left -> {
                // Dislike
                viewModel.recordPreference(content, false)
                binding.instructionText.text = getString(R.string.swipe_right_to_like)
            }
            else -> {
                // Do nothing
            }
        }
        
        // If we're at the end of the stack, load more content
        if (cardStackLayoutManager.topPosition >= cardStackAdapter.itemCount - 1) {
            viewModel.loadContents()
        }
    }
    
    override fun onCardRewound() {
        // Not used
    }
    
    override fun onCardCanceled() {
        val viewHolder = binding.cardStackView.findViewHolderForAdapterPosition(cardStackLayoutManager.topPosition) as? CardStackAdapter.ViewHolder
        viewHolder?.showLikeOverlay(false)
        viewHolder?.showRejectOverlay(false)
    }
    
    override fun onCardAppeared(view: View, position: Int) {
        // Not used
    }
    
    override fun onCardDisappeared(view: View, position: Int) {
        // Not used
    }
} 