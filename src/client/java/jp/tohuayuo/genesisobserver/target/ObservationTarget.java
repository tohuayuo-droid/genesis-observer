package jp.tohuayuo.genesisobserver.target;

import net.minecraft.util.math.BlockPos;

public record ObservationTarget(
        ObservationType type,
        BlockPos position,
        int priority
) {}
