package io.github.hidroh.tldroid

import android.content.Context
import androidx.annotation.StyleRes
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import okio.buffer
import okio.source
import java.io.IOException
import java.io.InputStream

internal object Utils {
  private const val KEY_THEME: String = "pref:theme"
  private const val VAL_THEME_SOLARIZED: String = "theme:solarized"
  private const val VAL_THEME_AFTERGLOW: String = "theme:afterglow"
  private const val VAL_THEME_TOMORROW: String = "theme:tomorrow"

  @Throws(IOException::class)
  fun readUtf8(inputStream: InputStream): String {
    return inputStream.source().buffer().readUtf8()
  }

  fun saveTheme(context: Context, @StyleRes themeRes: Int) {
    getDefaultSharedPreferences(context).edit()
      .putString(KEY_THEME, when (themeRes) {
        R.style.AppTheme_Afterglow -> VAL_THEME_AFTERGLOW
        R.style.AppTheme_Tomorrow -> VAL_THEME_TOMORROW
        else -> VAL_THEME_SOLARIZED
      }).apply()
  }

  @StyleRes
  fun loadTheme(context: Context): Int {
    val theme = getDefaultSharedPreferences(context).getString(KEY_THEME, VAL_THEME_SOLARIZED)
    return when (theme) {
      VAL_THEME_AFTERGLOW -> R.style.AppTheme_Afterglow
      VAL_THEME_TOMORROW -> R.style.AppTheme_Tomorrow
      else -> R.style.AppTheme
    }
  }
}