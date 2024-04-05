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
import java.lang.ref.WeakReference

@TargetApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {

    private lateinit var appName: String
    private lateinit var activeIcon: Icon
    private lateinit var inactiveIcon: Icon
    private var mMsgReceive: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        appName = getString(R.string.app_name)
        activeIcon = Icon.createWithResource(applicationContext, R.drawable.ic_stat_name)
        inactiveIcon = Icon.createWithResource(applicationContext, R.drawable.ic_stat_name)
    }

    fun setState(state: Int) {
        val label = if (state == Tile.STATE_ACTIVE) V2RayServiceManager.currentConfig?.remarks ?: appName else appName
        val icon = if (state == Tile.STATE_ACTIVE) activeIcon else inactiveIcon

        qsTile?.state = state
        qsTile?.label = label
        qsTile?.icon = icon

        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        setState(Tile.STATE_INACTIVE)
        mMsgReceive = ReceiveMessageHandler(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mMsgReceive, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(mMsgReceive, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))
        }

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

    private class ReceiveMessageHandler(service: QSTileService) : BroadcastReceiver() {
        private val serviceRef: WeakReference<QSTileService> = WeakReference(service)

        override fun onReceive(ctx: Context?, intent: Intent?) {
            serviceRef.get()?.let { service ->
                when (intent?.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING -> service.setState(Tile.STATE_ACTIVE)
                    AppConfig.MSG_STATE_NOT_RUNNING -> service.setState(Tile.STATE_INACTIVE)
                    AppConfig.MSG_STATE_START_SUCCESS -> service.setState(Tile.STATE_ACTIVE)
                    AppConfig.MSG_STATE_START_FAILURE -> service.setState(Tile.STATE_INACTIVE)
                    AppConfig.MSG_STATE_STOP_SUCCESS -> service.setState(Tile.STATE_INACTIVE)
                }
            }
        }
    }
}
