package com.shield.app.accessibility

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.shield.app.R

class BlockOverlayActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private var redirectUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.block_overlay)

        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You are Protected"
        val countdownSeconds = intent.getIntExtra(EXTRA_COUNTDOWN, 3)
        redirectUrl = intent.getStringExtra(EXTRA_REDIRECT_URL) ?: ""

        val messageView = findViewById<TextView>(R.id.overlay_message)
        val closeButton = findViewById<Button>(R.id.overlay_close_button)

        messageView.text = message

        if (countdownSeconds <= 0) {
            closeButton.isEnabled = true
            closeButton.text = getString(R.string.overlay_close)
        } else {
            closeButton.isEnabled = false
            closeButton.text = getString(R.string.overlay_close_countdown, countdownSeconds)
            countDownTimer = object : CountDownTimer(countdownSeconds * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = ((millisUntilFinished / 1000L) + 1).toInt()
                    closeButton.text = getString(R.string.overlay_close_countdown, secondsLeft)
                }

                override fun onFinish() {
                    closeButton.isEnabled = true
                    closeButton.text = getString(R.string.overlay_close)
                }
            }.start()
        }

        closeButton.setOnClickListener {
            handleClose()
        }
    }

    private fun handleClose() {
        val target = normalizeRedirectUrl(redirectUrl)
        if (target != null) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(browserIntent)
            } catch (e: Exception) {
                goHome()
            }
        } else {
            goHome()
        }
        finish()
    }

    /**
     * A URL saved without a scheme (e.g. "google.com") fails to resolve
     * with ACTION_VIEW, which silently sends the user Home instead of to
     * the site. New saves are already normalized in SettingsRepository,
     * but this defends against any value that predates that fix.
     */
    private fun normalizeRedirectUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onBackPressed() {
        // Intentionally disabled: the user must use the Close button so
        // the countdown gate can't be bypassed with the back gesture/key.
    }

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_COUNTDOWN = "extra_countdown"
        const val EXTRA_REDIRECT_URL = "extra_redirect_url"
    }
}
