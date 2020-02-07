package org.bitcoindotcom.bchprocessor.bip70

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import org.bitcoindotcom.bchprocessor.bip70.model.Bip70Action
import java.io.IOException
import java.util.*

/**
 * Socket will reconnect even without calling again webSocketHandler.start()
 * and without ACTION_INTENT_RECONNECT being sent by the ConnectivityManager.
 * The number of Thread will stay constant about 17 or 18 on OS v5.
 */
abstract class WebSocketHandler {
    private val webSocketFactory = WebSocketFactory()
    protected var TAG = "WebSocketHandler"
    @Volatile
    private var handler: ConnectionHandler? = null

    fun start(): WebSocketHandler {
        try {
            Log.i(TAG, "start threads:" + Thread.activeCount())
            stop()
            ConnectionThread().start()
        } catch (e: Exception) {
            Log.e(TAG, "start", e)
        }
        return this
    }

    fun setAutoReconnect(autoReconnect: Boolean) {
        handler?.setAutoReconnect(autoReconnect)
    }

    fun stop() {
        handler?.stop()
    }

    val isConnected: Boolean
        get() {
            val h = handler
            return h != null && h.isConnected && !h.isBroken
        }

    private fun send(message: String) {
        handler?.send(message)
    }

    @Throws(IOException::class)
    protected abstract fun createWebSocket(factory: WebSocketFactory): WebSocket

    @Throws(Exception::class)
    protected abstract fun parseTx(message: String?)

    private inner class ConnectionThread : Thread() {
        override fun run() {
            // The loop is only required for the initial connection
            // after that reconnections are being handled by the WebSocket
            var doubleBackOff: Long = 1000
            while (true) {
                try {
                    handler = ConnectionHandler()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Connect", e)
                    try {
                        sleep(doubleBackOff)
                    } catch (ex: InterruptedException) { // fail silently
                    }
                    doubleBackOff *= 2
                }
            }
        }

        init {
            name = "Bip70WebSocket"
            isDaemon = true
        }
    }

    private inner class ConnectionHandler : WebSocketAdapter() {
        private val sentMessageSet: MutableSet<String> = HashSet()
        private val mConnection: WebSocket?
        private var timeLastAlive: Long
        @Volatile
        private var autoReconnect = true

        @Throws(Exception::class)
        override fun onPongFrame(websocket: WebSocket, frame: WebSocketFrame) {
            super.onPongFrame(websocket, frame)
            timeLastAlive = System.currentTimeMillis()
            Log.d(TAG, "PongSuccess threads:" + Thread.activeCount())
        }

        @Throws(Exception::class)
        override fun onPingFrame(websocket: WebSocket, frame: WebSocketFrame) {
            super.onPingFrame(websocket, frame)
            timeLastAlive = System.currentTimeMillis()
            Log.d(TAG, "PingSuccess threads:" + Thread.activeCount())
        }

        @Throws(Exception::class)
        override fun onConnected(websocket: WebSocket, headers: Map<String, List<String>>) {
            super.onConnected(websocket, headers)
            Log.i(TAG, "onConnected threads:" + Thread.activeCount())
        }

        @Throws(Exception::class)
        override fun onDisconnected(websocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
            Log.e(TAG, "onDisconnected threads:" + Thread.activeCount())
            if (autoReconnect) { // reconnect on involuntary disconnection
                start()
            }
        }

        override fun onTextMessage(websocket: WebSocket, message: String) {
            try {
                parseTx(message)
            } catch (e: Exception) {
                Log.e(TAG, message, e)
            }
        }

        fun setAutoReconnect(autoReconnect: Boolean) {
            this.autoReconnect = autoReconnect
        }

        val isConnected: Boolean
            get() = mConnection != null && mConnection.isOpen

        fun stop() {
            if (isConnected) {
                autoReconnect = false
                mConnection?.clearListeners()
                mConnection?.disconnect()
            }
        }

        fun send(message: String) {
            // Make sure each message is only sent once per socket lifetime
            if (!sentMessageSet.contains(message)) {
                try {
                    if (isConnected) {
                        directSend(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "send:$message")
                }
            }
        }

        private fun directSend(message: String) {
            mConnection?.sendText(message)
            sentMessageSet.add(message)
        }

        val MINUTE_IN_MS = 60 * 1000

        // considered broken when older than 1 minute and with no ping or pong during that time
        val isBroken: Boolean
            // considered broken when older than 1 minute and with no ping or pong during that time
            get() = timeLastAlive + MINUTE_IN_MS < System.currentTimeMillis()


        init {
            timeLastAlive = System.currentTimeMillis()
            mConnection = createWebSocket(webSocketFactory)
                    .recreate()
                    .addListener(this)
            mConnection.pingInterval = PING_INTERVAL
            mConnection.connect()
            timeLastAlive = System.currentTimeMillis()
        }
    }

    companion object {
        private const val PING_INTERVAL = 20 * 1000L // ping every 20 seconds
        fun notifyConnectionStatus(context: Context, connected: Boolean) {
            val i = Intent(Bip70Action.UPDATE_CONNECTION_STATUS)
            i.putExtra(Bip70Action.PARAM_CONNECTION_STATUS_ENABLED, connected)
            LocalBroadcastManager.getInstance(context).sendBroadcast(i)
        }
    }
}