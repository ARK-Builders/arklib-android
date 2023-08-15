package dev.arkbuilders.scorewidget

import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import dev.arkbuilders.scorewidget.databinding.ScoreWidgetBinding
import org.orbitmvi.orbit.viewmodel.observe

class ScoreWidget(
    val controller: ScoreWidgetController,
    val lifecycleOwner: LifecycleOwner,
) {

    var binding: ScoreWidgetBinding? = null

    fun init(binding: ScoreWidgetBinding) {
        this.binding = binding
        binding.increaseScore.setOnClickListener {
            controller.onIncrease()
        }
        binding.decreaseScore.setOnClickListener {
            controller.onDecrease()
        }
        controller.observe(lifecycleOwner, state = ::render)
    }

    fun onDestroyView() {
        binding = null
    }

    private fun render(state: ScoreWidgetState) {
        val score = state.score
        binding!!.root.isVisible = state.visible
        binding!!.scoreValue.text = if (score == 0) null else score.toString()
    }
}