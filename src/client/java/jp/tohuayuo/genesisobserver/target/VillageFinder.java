package jp.tohuayuo.genesisobserver.target;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.util.math.BlockPos;

public final class VillageFinder implements TargetFinder {

    private static final int SEARCH_RADIUS_CHUNKS = 128;
    private static final int VILLAGE_PRIORITY = 50;

    @Override
    public ObservationTarget find(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }

        IntegratedServer server = client.getServer();
        if (server == null) {
            return null;
        }

        ServerWorld serverWorld = server.getWorld(client.world.getRegistryKey());
        if (serverWorld == null) {
            return null;
        }

        BlockPos origin = client.player.getBlockPos();

        BlockPos villagePosition = serverWorld.locateStructure(
                StructureTags.VILLAGE,
                origin,
                SEARCH_RADIUS_CHUNKS,
                false
        );

        if (villagePosition == null) {
            return null;
        }

        return new ObservationTarget(
                ObservationType.VILLAGE,
                villagePosition,
                VILLAGE_PRIORITY
        );
    }
}
