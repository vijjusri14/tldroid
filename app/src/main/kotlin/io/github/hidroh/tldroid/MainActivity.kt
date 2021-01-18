package io.github.hidroh.tldroid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.cursoradapter.widget.ResourceCursorAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomsheet.BottomSheetDialog


class MainActivity : ThemedActivity() {
  private var mEditText: AutoCompleteTextView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView()
  }

  private fun setContentView() {
    DataBindingUtil.setContentView<ViewDataBinding>(this, R.layout.activity_main)
    findViewById<ImageButton>(R.id.update_button)!!.setOnClickListener { updatePackages() }
    findViewById<ImageButton>(R.id.info_button)!!.setOnClickListener { showInfo() }
    findViewById<ImageButton>(R.id.list_button)!!.setOnClickListener { showList() }
    findViewById<ImageButton>(R.id.settings_button)!!.setOnClickListener { showThemeOptions() }
    mEditText = findViewById<AutoCompleteTextView>(R.id.edit_text)
    mEditText!!.setOnEditorActionListener { v, actionId, _ ->
      actionId == EditorInfo.IME_ACTION_SEARCH && search(v.text.toString(), null)
    }
    mEditText!!.setAdapter(CursorAdapter(this))
    mEditText!!.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
      val text = (view.findViewById(android.R.id.text1) as TextView).text
      val platform = (view.findViewById(android.R.id.text2) as TextView).text
      search(text.toString(), platform)
    }
  }

  private fun search(query: CharSequence, platform: CharSequence?): Boolean {
    if (TextUtils.isEmpty(query)) {
      return false
    }
    mEditText!!.setText(query)
    mEditText!!.setSelection(query.length)
    startActivity(Intent(this, CommandActivity::class.java)
            .putExtra(CommandActivity.EXTRA_QUERY, query)
            .putExtra(CommandActivity.EXTRA_PLATFORM, platform))
    return true
  }

  private fun updatePackages() {
    SyncService.enqueueWork(this, Application.indexIntent)
    SyncService.enqueueWork(this, Application.zipIntent)
  }

  private fun showInfo() {
    closeSoftKeyboard()
    val binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater,
            R.layout.web_view, null, false)
    val lastRefreshed = getDefaultSharedPreferences(this)
        .getLong(SyncService.PREF_LAST_REFRESHED, 0L)
    val lastRefreshedText = if (lastRefreshed > 0)
      DateUtils.getRelativeDateTimeString(this, lastRefreshed,
              DateUtils.HOUR_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)
    else
      getString(R.string.never)
    val totalCommands = getDefaultSharedPreferences(this)
        .getInt(SyncService.PREF_COMMAND_COUNT, 0)
    binding.setVariable(io.github.hidroh.tldroid.BR.content,
            getString(R.string.info_html, lastRefreshedText, totalCommands) +
                    getString(R.string.about_html))
    binding.root.id = R.id.web_view
    val dialog = BottomSheetDialog(this)
    dialog.setContentView(binding.root)
    dialog.show()
  }

  private fun showList() {
    closeSoftKeyboard()
    val dialog = BottomSheetDialog(this)
    val view = layoutInflater.inflate(R.layout.dialog_commands, null, false)
    val listView = view.findViewById(android.R.id.list) as ListView
    val adapter = CursorAdapter(this)
    adapter.filter.filter("")
    listView.adapter = adapter
    listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
      val cursor = adapter.getItem(position) as Cursor? ?: return@OnItemClickListener
      search(cursor.getString(cursor.getColumnIndex(TldrProvider.CommandEntry.COLUMN_NAME)),
              cursor.getString(cursor.getColumnIndex(TldrProvider.CommandEntry.COLUMN_PLATFORM)))

    }
    dialog.setContentView(view)
    dialog.show()
  }

  private fun showThemeOptions() {
    val popupMenu = PopupMenu(this, findViewById(R.id.settings_button), Gravity.NO_GRAVITY)
    popupMenu.inflate(R.menu.menu_theme)
    popupMenu.setOnMenuItemClickListener { item ->
      Utils.saveTheme(this, when (item.itemId) {
        R.id.menu_afterglow -> R.style.AppTheme_Afterglow
        R.id.menu_tomorrow -> R.style.AppTheme_Tomorrow
        else -> R.style.AppTheme
      })
      this.recreate()
      setContentView()
      true
    }
    popupMenu.show()
  }

  private fun closeSoftKeyboard() {
    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
        .hideSoftInputFromWindow(mEditText!!.windowToken, 0)
  }

  private class CursorAdapter(context: Context) :
      ResourceCursorAdapter(context, R.layout.dropdown_item, null, false) {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mQueryString: String? = null

    init {
      filterQueryProvider = FilterQueryProvider { constraint ->
        mQueryString = constraint?.toString() ?: ""
        context.contentResolver.query(TldrProvider.URI_COMMAND,
                arrayOf(BaseColumns._ID,
                        TldrProvider.CommandEntry.COLUMN_NAME,
                        TldrProvider.CommandEntry.COLUMN_PLATFORM),
                "${TldrProvider.CommandEntry.COLUMN_NAME} LIKE ?",
                arrayOf("%$mQueryString%"),
                null)
      }
    }

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup): View {
      return newDropDownView(context, cursor, parent)
    }

    override fun newDropDownView(context: Context?, cursor: Cursor?, parent: ViewGroup): View {
      val holder = DataBindingUtil.inflate<ViewDataBinding>(mInflater, R.layout.dropdown_item,
              parent, false)
      val view = holder.root
      view.setTag(R.id.dataBinding, holder)
      return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
      val binding = view.getTag(R.id.dataBinding) as ViewDataBinding
      binding.setVariable(io.github.hidroh.tldroid.BR.command,
              Bindings.Command.fromProvider(cursor))
      binding.setVariable(io.github.hidroh.tldroid.BR.highlight, mQueryString)
    }

    override fun convertToString(cursor: Cursor?): CharSequence {
      return cursor!!.getString(cursor.getColumnIndexOrThrow(
              TldrProvider.CommandEntry.COLUMN_NAME))
    }
  }
}
