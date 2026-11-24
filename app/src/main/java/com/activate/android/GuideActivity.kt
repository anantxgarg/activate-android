package com.activate.android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.activate.android.databinding.ActivityGuideBinding
import com.activate.android.databinding.ItemGuideStepBinding

class GuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuideBinding

    data class Step(
        val stepNumber: Int,
        val totalSteps: Int,
        val title: String,
        val description: String,
        val imageRes: Int
    )

    private val steps = listOf(
        Step(
            1, 9,
            "1. Locate Activate Android",
            "In your phone's 'Display over other apps' settings list, find and tap Activate Android.",
            R.drawable.guide_step_1
        ),
        Step(
            2, 9,
            "2. Restricted by Android",
            "Notice the 'Allow display over other apps' switch is disabled/greyed out because the app was sideloaded.",
            R.drawable.guide_step_2
        ),
        Step(
            3, 9,
            "3. Tap to Trigger Prompt",
            "Tap on the disabled switch. A system prompt will say 'App was denied access'. Tap Close.",
            R.drawable.guide_step_3
        ),
        Step(
            4, 9,
            "4. Go to App Info",
            "Go to device Settings > Apps > Activate Android. Notice the three vertical dots (⋮) in the top-right corner.",
            R.drawable.guide_step_4
        ),
        Step(
            5, 9,
            "5. Allow Restricted Settings",
            "Tap the three dots (⋮) and choose 'Allow restricted settings'. Confirm with your fingerprint or PIN.",
            R.drawable.guide_step_5
        ),
        Step(
            6, 9,
            "6. Confirmation Toast",
            "A confirmation message will appear: 'Restricted settings allowed for Activate Android'.",
            R.drawable.guide_step_6
        ),
        Step(
            7, 9,
            "7. Enable the Permission",
            "Return to 'Display over other apps'—the switch is now unlocked! Turn it ON.",
            R.drawable.guide_step_7
        ),
        Step(
            8, 9,
            "8. Allow Background Run",
            "When prompted 'Let app always run in background?', select 'Allow' so the watermark stays active.",
            R.drawable.guide_step_8
        ),
        Step(
            9, 9,
            "9. Turn On Watermark!",
            "Return to Activate Android and turn ON 'Enable Watermark'. Enjoy your authentic watermark!",
            R.drawable.guide_step_9
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupListeners()
    }

    private fun setupViewPager() {
        val adapter = GuidePagerAdapter(steps)
        binding.viewPagerGuide.adapter = adapter

        binding.viewPagerGuide.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateControls(position)
            }
        })
        updateControls(0)
    }

    private fun setupListeners() {
        binding.btnSkip.setOnClickListener {
            markGuideSeenAndFinish()
        }

        binding.btnPrev.setOnClickListener {
            val current = binding.viewPagerGuide.currentItem
            if (current > 0) {
                binding.viewPagerGuide.currentItem = current - 1
            }
        }

        binding.btnNext.setOnClickListener {
            val current = binding.viewPagerGuide.currentItem
            if (current < steps.size - 1) {
                binding.viewPagerGuide.currentItem = current + 1
            } else {
                markGuideSeenAndFinish()
            }
        }
    }

    private fun updateControls(position: Int) {
        binding.tvPageIndicator.text = "${position + 1} / ${steps.size}"
        binding.btnPrev.isEnabled = position > 0

        if (position == steps.size - 1) {
            binding.btnNext.text = "Get Started"
        } else {
            binding.btnNext.text = "Next"
        }
    }

    private fun markGuideSeenAndFinish() {
        val prefs = getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_seen_guide", true).apply()
        finish()
    }

    inner class GuidePagerAdapter(private val items: List<Step>) :
        RecyclerView.Adapter<GuidePagerAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemGuideStepBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemGuideStepBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val step = items[position]
            with(holder.itemBinding) {
                tvStepBadge.text = "STEP ${step.stepNumber} OF ${step.totalSteps}"
                tvStepTitle.text = step.title
                tvStepDesc.text = step.description
                ivStepImage.setImageResource(step.imageRes)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
