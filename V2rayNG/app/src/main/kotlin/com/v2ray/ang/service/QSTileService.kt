package com.v2ray.ang.service

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.Utils
import java.lang.ref.SoftReference

@TargetApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {

    private val appName by lazy { getString(R.string.app_name) }
    private val inactiveIcon by lazy { Icon.createWithResource(applicationContext, R.drawable.ic_stat_name) }

    private fun setState(state: Int) {
        val tile = qsTile
        val label = if (state == Tile.STATE_ACTIVE) V2RayServiceManager.currentConfig?.remarks else appName
        tile?.apply {
            this.state = state
            this.label = label
            this.icon = inactiveIcon
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        setState(Tile.STATE_INACTIVE)
        mMsgReceive = ReceiveMessageHandler()
        val intentFilter = IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addFlags(Context.RECEIVER_EXPORTED)
            }
        }
        registerReceiver(mMsgReceive, intentFilter)
        MessageUtil.sendMsg2Service(this, AppConfig.MSG_REGISTER_CLIENT, "")
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterReceiver(mMsgReceive)
        mMsgReceive = null
    }

    override fun onClick() {
        super.onClick()
        when (qsTile?.state) {
            Tile.STATE_INACTIVE -> Utils.startVServiceFromToggle(this)
            Tile.STATE_ACTIVE -> Utils.stopVService(this)
        }
    }

    private var mMsgReceive: BroadcastReceiver? = null

    private inner class ReceiveMessageHandler : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING, 
                AppConfig.MSG_STATE_START_SUCCESS -> setState(Tile.STATE_ACTIVE)
                AppConfig.MSG_STATE_NOT_RUNNING, 
                AppConfig.MSG_STATE_START_FAILURE, 
                AppConfig.MSG_STATE_STOP_SUCCESS -> setState(Tile.STATE_INACTIVE)
            }
        }
    }
}
