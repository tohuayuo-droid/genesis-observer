package jp.tohuayuo.genesisobserver.target;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class VillageFinder implements TargetFinder {

    private static final int SEARCH_RADIUS_CHUNKS = 128;
    private static final int SEARCH_OFFSET = 700;
    private static final int SAME_VILLAGE_DISTANCE = 250;
    private static final int VILLAGE_PRIORITY = 50;

    private BlockPos lastVillagePosition;

    @Override
    public ObservationTarget find(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }

        IntegratedServer server = client.getServer();
        if (server == null) {
            return null;
        }

        ServerWorld serverWorld =
                server.getWorld(client.world.getRegistryKey());

        if (serverWorld == null) {
            return null;
        }

        BlockPos playerPosition = client.player.getBlockPos();

        BlockPos villagePosition;

        if (lastVillagePosition == null) {
            villagePosition = locateVillage(
                    serverWorld,
                    playerPosition
            );
        } else {
            villagePosition = findDifferentVillage(
                    serverWorld,
                    lastVillagePosition
            );
        }

        if (villagePosition == null) {
            return null;
        }

        lastVillagePosition = villagePosition;

        return new ObservationTarget(
                ObservationType.VILLAGE,
                villagePosition,
                VILLAGE_PRIORITY
        );
    }

    private BlockPos findDifferentVillage(
            ServerWorld world,
            BlockPos previousVillage
    ) {
        BlockPos[] searchOrigins = {
                previousVillage.add(SEARCH_OFFSET, 0, 0),
                previousVillage.add(-SEARCH_OFFSET, 0, 0),
                previousVillage.add(0, 0, SEARCH_OFFSET),
                previousVillage.add(0, 0, -SEARCH_OFFSET)
        };

        for (BlockPos searchOrigin : searchOrigins) {
            BlockPos result = locateVillage(
                    world,
                    searchOrigin
            );

            if (result != null
                    && !isSameVillage(result, previousVillage)) {
                return result;
            }
        }

        return null;
    }

    private BlockPos locateVillage(
            ServerWorld world,
            BlockPos origin
    ) {
        return world.locateStructure(
                StructureTags.VILLAGE,
                origin,
                SEARCH_RADIUS_CHUNKS,
                false
        );
    }

    private boolean isSameVillage(
            BlockPos first,
            BlockPos second
    ) {
        double xDifference = first.getX() - second.getX();
        double zDifference = first.getZ() - second.getZ();

        return Math.hypot(
                xDifference,
                zDifference
        ) < SAME_VILLAGE_DISTANCE;
    }
}
