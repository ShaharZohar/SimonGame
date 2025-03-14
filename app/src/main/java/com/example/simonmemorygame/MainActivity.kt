// MainActivity.kt
package com.example.simonmemorygame

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.simonmemorygame.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var buttons: List<Button> = listOf() // Initialize as empty list
    private val sequence = mutableListOf<Int>()
    private var playerTurn = false
    private var playerIndex = 0
    private var level = 1
    private var score = 0
    private lateinit var soundPool: SoundPool
    private val soundIds = mutableListOf<Int>()
    private var gameOverSoundId: Int = 0
    private var numButtons = 4 // Default number of buttons
    private var maxLevel = 10 // Default max level
    private var unlimitedLevels = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        soundPool = SoundPool.Builder().setMaxStreams(5).build()
        gameOverSoundId = soundPool.load(this, R.raw.game_over, 1)

        binding.startButton.setOnClickListener { showDifficultyDialog() }
        updateLevelAndScore()
    }

    private fun setupButtons() {
        binding.buttonContainer.removeAllViews()
        soundIds.clear()
        val gridLayout = binding.buttonContainer
        gridLayout.columnCount = if (numButtons <= 4) 2 else if (numButtons <= 9) 3 else 4
        buttons = (0 until numButtons).map { index ->
            Button(this).apply {
                id = View.generateViewId()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 200
                    height = 200
                    setMargins(10, 10, 10, 10)
                }
                backgroundTintList = ColorStateList.valueOf(getRandomColor())//getColorStateList(getButtonColor(index))
                setOnClickListener { onButtonClick(index) }
                gridLayout.addView(this)
                soundIds.add(soundPool.load(this@MainActivity, getButtonSound(index), 1))
            }
        }
    }

    private fun getButtonColor(index: Int): Int {
        return when (index) {
                0 -> R.color.button_color_1
                1 -> R.color.button_color_2
                2 -> R.color.button_color_3
                3 -> R.color.button_color_4
                4 -> R.color.button_color_5
                5 -> R.color.button_color_6
                6 -> R.color.button_color_7
                7 -> R.color.button_color_8
                else -> getRandomColor()
            }
    }

    private fun getRandomColor(): Int {
        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        return Color.rgb(red, green, blue)
    }

    private fun getButtonSound(index: Int): Int {
        return when (index) {
            0 -> R.raw.sound1
            1 -> R.raw.sound2
            2 -> R.raw.sound3
            3 -> R.raw.sound4
            4 -> R.raw.sound5
            5 -> R.raw.sound6
            6 -> R.raw.sound7
            7 -> R.raw.sound8
            else -> R.raw.sound1 // Default
        }
    }

    private fun showDifficultyDialog() {
        val options = arrayOf("Easy", "Medium", "Hard", "Custom")
        AlertDialog.Builder(this)
            .setTitle("Choose Difficulty")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setDifficulty(4, 10, false) // Easy
                    1 -> setDifficulty(6, 15, false) // Medium
                    2 -> setDifficulty(8, 20, false) // Hard
                    3 -> showCustomDifficultyDialog() // Custom
                }
            }
            .show()
    }

    private fun showCustomDifficultyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.custom_difficulty_dialog, null)
        val numButtonsInput = dialogView.findViewById<android.widget.EditText>(R.id.numButtonsInput)
        val maxLevelInput = dialogView.findViewById<android.widget.EditText>(R.id.maxLevelInput)

        AlertDialog.Builder(this)
            .setTitle("Custom Difficulty")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val numButtonsValue = numButtonsInput.text.toString().toIntOrNull() ?: 4
                val maxLevelValue = maxLevelInput.text.toString().toIntOrNull() ?: 10
                val unlimited = maxLevelValue == 0
                setDifficulty(numButtonsValue, if (unlimited) 10 else maxLevelValue, unlimited)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setDifficulty(buttons: Int, level: Int, unlimited: Boolean) {
        numButtons = buttons
        maxLevel = level
        unlimitedLevels = unlimited
        soundIds.clear() // Clear sound IDs
        setupButtons()
        startGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    private fun playSound(index: Int) {
        if (soundIds.isNotEmpty() && index < soundIds.size) {
            soundPool.play(soundIds[index], 1f, 1f, 1, 0, 1f)
        }
    }

    private fun playGameOverSound() {
        if (gameOverSoundId != 0) {
            soundPool.play(gameOverSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun startGame() {
        sequence.clear()
        playerTurn = false
        playerIndex = 0
        level = 1
        score = 0
        updateLevelAndScore()
        generateSequence()
        playSequence()
    }

    private fun generateSequence() {
        repeat(level) {
            sequence.add(Random.nextInt(numButtons))
        }
    }

    private fun playSequence() {
        lifecycleScope.launch {
            buttons.forEach { it.isEnabled = false }
            delay(500)
            sequence.forEach {
                highlightButton(it)
                delay(500)
            }
            buttons.forEach { it.isEnabled = true }
            playerTurn = true
        }
    }

    private suspend fun highlightButton(index: Int) {
        val button = buttons[index]
        val originalColor = button.backgroundTintList
        button.backgroundTintList = getColorStateList(R.color.highlighted_color)
        playSound(index)
        delay(300)
        button.backgroundTintList = originalColor
    }

    private fun onButtonClick(index: Int) {
        if (playerTurn) {
            if (index == sequence[playerIndex]) {
                playSound(index)
                playerIndex++
                if (playerIndex == sequence.size) {
                    if (unlimitedLevels || level < maxLevel) {
                        level++
                        score++
                        updateLevelAndScore()
                        playerTurn = false
                        playerIndex = 0
                        generateSequence()
                        playSequence()
                    } else {
                        gameWon()
                    }
                }
            } else {
                gameOver()
            }
        }
    }

    private fun gameOver() {
        binding.resultTextView.text = "Game Over! Score: $score"
        binding.resultTextView.visibility = View.VISIBLE
        playerTurn = false
        buttons.forEach { it.isEnabled = false }
        playGameOverSound()
    }

    private fun gameWon(){
        binding.resultTextView.text = "You Won! Score: $score"
        binding.resultTextView.visibility = View.VISIBLE
        playerTurn = false
        buttons.forEach { it.isEnabled = false }
    }

    private fun updateLevelAndScore() {
        binding.levelTextView.text = "Level: $level"
        binding.scoreTextView.text = "Score: $score"
        binding.resultTextView.visibility = View.GONE
    }
}