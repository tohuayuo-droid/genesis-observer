package jp.tohuayuo.genesisobserver.target;

import net.minecraft.client.MinecraftClient;

public interface TargetFinder {
    ObservationTarget find(MinecraftClient client);
}
