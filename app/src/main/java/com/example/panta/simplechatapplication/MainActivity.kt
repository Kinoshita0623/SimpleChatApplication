package com.example.panta.simplechatapplication

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class MainActivity : AppCompatActivity() {

    val gson = Gson()

    lateinit var connectModeLayout: LinearLayout
    private lateinit var connectDomainEditText: EditText
    lateinit var nameEditText: EditText
    lateinit var connectButton: Button

    lateinit var chatModeLayout: LinearLayout
    private lateinit var sendEditText: EditText
    lateinit var chatView: ListView
    lateinit var sendButton: Button

    private var chatSocket: Socket? = null
    lateinit var name: String
    val chatList = ArrayList<String>()

    var connectCounter: Int = 0


    inner class Socket(serverUri: URI): WebSocketClient(serverUri){

        @SuppressLint("ShowToast")
        override fun onOpen(handshakedata: ServerHandshake?) {
            runOnUiThread{

                Toast.makeText(applicationContext, "接続が開始されました", Toast.LENGTH_LONG).show()

            }
            connectCounter++
            Log.d("Socket","ConnectCounterの値 $connectCounter")
        }

        @SuppressLint("ShowToast")
        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            runOnUiThread{
                connectModeView()
                Toast.makeText(applicationContext, "通信が切断されました。", Toast.LENGTH_LONG).show()
            }
            connectCounter--
            Log.d("Socket","ConnectCounterの値 $connectCounter")
            Log.d("Socket", "接続が切断されました")
        }

        override fun onMessage(message: String?) {
            Log.d("Chat", message)
            val json = gson.fromJson(message, ReceiveMessageBean::class.java)
            val text = "name:${json.name}, msg:${json.message}"
            chatList.add(text)
            runOnUiThread{
                val adapter = ArrayAdapter(applicationContext,android.R.layout.simple_list_item_1, chatList)
                chatView.adapter = adapter
                chatView.setSelection(chatList.size)
            }
        }

        @SuppressLint("ShowToast")
        override fun onError(ex: Exception?) {
            runOnUiThread{
                connectModeView()
                Toast.makeText(applicationContext, "WebSocketでエラーが発生しました", Toast.LENGTH_LONG).show()
                ex?.printStackTrace()
            }
        }
    }

    @SuppressLint("ShowToast")
    private fun socketConnect(uri: URI){
        chatModeView()

        when {
            chatSocket == null -> {
                chatSocket = Socket(uri)
                chatSocket!!.connect()
            }
            chatSocket!!.isOpen -> Toast.makeText(applicationContext, "既に接続済みです", Toast.LENGTH_LONG)
            //chatSocket!!.isClosing -> chatSocket!!.connect()
            chatSocket!!.isClosed || chatSocket!!.isClosing -> {
                chatSocket = Socket(endpointMaker())
                chatSocket!!.connect()
            }
        }
    }

    @SuppressLint("ShowToast")
    private fun socketClose(){
        connectModeView()

        if(chatSocket == null){
            Toast.makeText(applicationContext, "切断されました", Toast.LENGTH_LONG)
        }else if(chatSocket!!.isOpen || chatSocket!!.isConnecting){
            chatSocket!!.close()
            chatSocket = null
        }else{
            chatSocket = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatView = findViewById(R.id.chatView)
        /*接続前のView*/
        connectModeLayout = findViewById(R.id.connectionModeLayout)
        connectDomainEditText = findViewById(R.id.connectDomain)
        nameEditText = findViewById(R.id.nameEditText)
        connectButton = findViewById(R.id.connectionButton)

        //接続後のView
        chatModeLayout = findViewById(R.id.chatModeLayout)
        sendEditText = findViewById(R.id.sendText)
        sendButton = findViewById(R.id.sendButton)

        connectModeView()


    }
    private fun endpointMaker(): URI{
        val domain = connectDomainEditText.text.toString()
        val url = "ws://$domain/chat/socket"
        return URI(url)
    }

    override fun onStart(){
        super.onStart()


        connectButton.setOnClickListener{
            name = nameEditText.text.toString()


            socketConnect(endpointMaker())
        }

        sendButton.setOnClickListener {
            val sendText = sendEditText.text.toString()
            val sendBean = SendMessageBean(name = name, message = sendText)
            if(chatSocket != null && chatSocket!!.isOpen){
                chatSocket!!.send(gson.toJson(sendBean))
            }else{
                socketConnect(endpointMaker())
            }

        }

    }

    override fun onStop(){
        super.onStop()

        when {
            chatSocket == null -> {

            }
            chatSocket!!.isOpen -> chatSocket!!.close()

        }

        chatSocket = null
    }

    private fun connectModeView(){
        connectModeLayout.visibility = View.VISIBLE
        chatModeLayout.visibility = View.GONE
        chatView.visibility = View.GONE
        connectButton.isEnabled = true
    }

    private fun chatModeView(){
        connectModeLayout.visibility = View.GONE
        chatModeLayout.visibility = View.VISIBLE
        chatView.visibility = View.VISIBLE
        connectButton.isEnabled = false
    }

   // inner class Socket:
}
