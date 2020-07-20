package com.csh.networkmonitor.net

import okhttp3.*
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.atomic.AtomicLong

class HttpEventListener(
    private val callId: Long?, // 每次请求的标识
    private val url: HttpUrl?,
    private val callStartTime: Long, // 每次请求的开始时间，单位毫秒
    val listener: TimeConsumingListener?=null
) : EventListener() {

    var dnsStartTime = System.currentTimeMillis()
    var connectStartTime = System.currentTimeMillis()
    private fun recordEventLog(name: String) {
        if (name == "callStart") {
            listener?.startConnect()
        }
        if (name == "connectFailed"
            || name == "callFailed") {
            listener?.error(1000, "连接失败")
            return
        }

        if (name == "dnsStart") {
            dnsStartTime = System.currentTimeMillis()
        }
        if (name == "dnsEnd") {
            listener?.dnsParseTime(System.currentTimeMillis()-dnsStartTime)
        }

        if (name == "connectStart") {
            connectStartTime = System.currentTimeMillis()
        }
        if (name == "connectEnd") {
            listener?.connectTime(System.currentTimeMillis()-connectStartTime)
        }
        if (name == "callEnd") {
            listener?.complete()
        }
    }

    override fun callStart(call: Call) {
        super.callStart(call)
        recordEventLog("callStart")
    }

    override fun dnsStart(call: Call, domainName: String) {
        super.dnsStart(call, domainName)
        recordEventLog("dnsStart")
    }

    override fun dnsEnd(
        call: Call,
        domainName: String,
        inetAddressList: List<InetAddress>
    ) {
        super.dnsEnd(call, domainName, inetAddressList)
        recordEventLog("dnsEnd")
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy
    ) {
        super.connectStart(call, inetSocketAddress, proxy)
        recordEventLog("connectStart")
    }

    override fun secureConnectStart(call: Call) {
        super.secureConnectStart(call)
        recordEventLog("secureConnectStart")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        super.secureConnectEnd(call, handshake)
        recordEventLog("secureConnectEnd")
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?
    ) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol)
        recordEventLog("connectEnd")
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException
    ) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
        recordEventLog("connectFailed")
    }

    override fun connectionAcquired(
        call: Call,
        connection: Connection
    ) {
        super.connectionAcquired(call, connection)
        recordEventLog("connectionAcquired")
    }

    override fun connectionReleased(
        call: Call,
        connection: Connection
    ) {
        super.connectionReleased(call, connection)
        recordEventLog("connectionReleased")
    }

    override fun requestHeadersStart(call: Call) {
        super.requestHeadersStart(call)
        recordEventLog("requestHeadersStart")
    }

    override fun requestHeadersEnd(
        call: Call,
        request: Request
    ) {
        super.requestHeadersEnd(call, request)
        recordEventLog("requestHeadersEnd")
    }

    override fun requestBodyStart(call: Call) {
        super.requestBodyStart(call)
        recordEventLog("requestBodyStart")
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        super.requestBodyEnd(call, byteCount)
        recordEventLog("requestBodyEnd")
    }

    override fun responseHeadersStart(call: Call) {
        super.responseHeadersStart(call)
        recordEventLog("responseHeadersStart")
    }

    override fun responseHeadersEnd(
        call: Call,
        response: Response
    ) {
        super.responseHeadersEnd(call, response)
        recordEventLog("responseHeadersEnd")
    }

    override fun responseBodyStart(call: Call) {
        super.responseBodyStart(call)
        recordEventLog("responseBodyStart")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        super.responseBodyEnd(call, byteCount)
        recordEventLog("responseBodyEnd")
    }

    override fun callEnd(call: Call) {
        super.callEnd(call)
        recordEventLog("callEnd")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        super.callFailed(call, ioe)
        recordEventLog("callFailed")
    }

    companion object {
        /**
         * 自定义EventListener工厂
         */
        val FACTORY: Factory = object : Factory {

            val nextCallId =
                AtomicLong(1L)

            override fun create(call: Call): EventListener {
                val callId = nextCallId.getAndIncrement()
                return HttpEventListener(
                    callId,
                    call.request().url(),
                    System.currentTimeMillis()
                )
            }
        }
    }

    interface TimeConsumingListener {
        fun permissionCheckStart()
        fun permissionOK()

        fun netConnectCheckStart()
        fun netConnectOk()

        fun serverConnectCheckStart()
        fun serverConnectOk()

        fun startConnect()
        fun dnsParseTime(time: Long?)
        fun connectTime(time: Long?)

        fun error(type: Int, desc: String)
        fun complete()
    }

}