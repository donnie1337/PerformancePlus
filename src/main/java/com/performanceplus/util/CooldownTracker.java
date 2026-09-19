package com.performanceplus.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conta quantas vezes uma chave (ex: um chunk) foi "ativada" dentro de uma
 * janela de tempo deslizante simples (reinicia quando a janela expira).
 * Usado para limitar atividade de redstone, pistões, geração de chunks, etc,
 * sem precisar de estruturas de dados caras a cada chamada.
 */
public class CooldownTracker {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final long windowMillis;

    public CooldownTracker(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /**
     * Registra uma ativação para a chave informada e retorna quantas
     * ativações ocorreram dentro da janela de tempo atual (incluindo esta).
     */
    public int registerAndCount(String key) {
        long now = System.currentTimeMillis();
        Window w = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.windowStart > windowMillis) {
                w.windowStart = now;
                w.count = 0;
            }
            w.count++;
            return w.count;
        }
    }

    private static final class Window {
        long windowStart;
        int count;

        Window(long start) {
            this.windowStart = start;
        }
    }
}
