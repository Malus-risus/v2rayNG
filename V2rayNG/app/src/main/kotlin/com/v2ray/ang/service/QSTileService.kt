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
import java.lang.ref.WeakReference

@TargetApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {

    private var mMsgReceive: BroadcastReceiver? = null

    private class ReceiveMessageHandler(service: QSTileService) : BroadcastReceiver() {
        private val serviceRef = WeakReference(service)
        override fun onReceive(ctx: Context?, intent: Intent?) {
            serviceRef.get()?.let { service ->
                when (intent?.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING,
                    AppConfig.MSG_STATE_START_SUCCESS -> service.setState(Tile.STATE_ACTIVE)

                    AppConfig.MSG_STATE_NOT_RUNNING,
                    AppConfig.MSG_STATE_START_FAILURE,
                    AppConfig.MSG_STATE_STOP_SUCCESS -> service.setState(Tile.STATE_INACTIVE)
                }
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        setState(Tile.STATE_INACTIVE)
        val intentFilter = IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addFlags(IntentFilter.FLAG_RECEIVER_INCLUDE_BACKGROUND)
            }
        }
        mMsgReceive = ReceiveMessageHandler(this)
        registerReceiver(mMsgReceive, intentFilter)
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterReceiver(mMsgReceive)
        mMsgReceive = null
    }

    fun setState(state: Int) {
        qsTile?.apply {
            this.state = state
            label = if (state == Tile.STATE_ACTIVE) V2RayServiceManager.currentConfig?.remarks else getString(R.string.app_name)
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_stat_name)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        when (qsTile?.state) {
            Tile.STATE_INACTIVE -> Utils.startVServiceFromToggle(this)
            Tile.STATE_ACTIVE -> Utils.stopVService(this)
        }
    }
}
