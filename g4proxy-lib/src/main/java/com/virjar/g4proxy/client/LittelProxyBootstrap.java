package com.virjar.g4proxy.client;

import org.littleshoot.proxy.Launcher;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import lombok.Setter;

public class LittelProxyBootstrap {
    private static AtomicBoolean isRunning = new AtomicBoolean(false);

    @Getter
    @Setter
    private static int littelServerPort = 3128;

    public static void makeSureLittelProxyStartup() {
        if (isRunning.compareAndSet(false, true)) {
            startInternal();
        }
    }

    private static void startInternal() {
        try {
            Launcher.startHttpProxyService(littelServerPort);
        } catch (RuntimeException e) {
            isRunning.set(false);
            throw e;
        }
    }
}
