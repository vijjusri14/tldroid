package io.github.hidroh.tldroid

import android.content.Intent
import android.graphics.Typeface

class Application : android.app.Application() {
  companion object {
    var MONOSPACE_TYPEFACE: Typeface? = null
    var indexIntent = Intent().apply{
      putExtra(SyncService.EXTRA_ASSET_TYPE, SyncService.ASSET_TYPE_INDEX)
    }
    var zipIntent = Intent().apply{
      putExtra(SyncService.EXTRA_ASSET_TYPE, SyncService.ASSET_TYPE_ZIP)
    }
  }

  override fun onCreate() {
    super.onCreate()
    MONOSPACE_TYPEFACE = Typeface.createFromAsset(assets, "RobotoMono-Regular.ttf")
    SyncService.enqueueWork(this, indexIntent)
    SyncService.enqueueWork(this, zipIntent)
  }

}
