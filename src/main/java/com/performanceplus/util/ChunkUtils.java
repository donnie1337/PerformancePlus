package com.performanceplus.util;

import org.bukkit.Chunk;

public final class ChunkUtils {

    private ChunkUtils() {
    }

    /**
     * Gera uma chave única e estável para um chunk, usada nos mapas de
     * contadores (pistões, observers, atividade de redstone, etc).
     */
    public static String key(Chunk chunk) {
        return chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ();
    }
}
