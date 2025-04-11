package com.opengps.locationsharing

import io.matthewnelson.kmp.tor.runtime.RuntimeEvent
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import io.matthewnelson.kmp.tor.runtime.core.net.Port.Ephemeral.Companion.toPortEphemeral

val runtime = TorRuntime.Builder(getPlatform().runtimeEnvironment) {
    RuntimeEvent.entries().forEach { event ->
        observerStatic(event, OnEvent.Executor.Immediate) { data ->
            println(data.toString())
        }
    }
    TorEvent.entries().forEach { event ->
        observerStatic(event, OnEvent.Executor.Immediate) { data ->
            println(data)
        }
    }
    config { environment ->
        TorOption.SocksPort.configure {
            port(42997.toPortEphemeral())
        }
    }
    required(TorEvent.ERR)
    required(TorEvent.WARN)
}