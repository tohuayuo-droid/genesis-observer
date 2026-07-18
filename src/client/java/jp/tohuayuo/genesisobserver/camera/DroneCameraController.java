package jp.tohuayuo.genesisobserver.camera;

import jp.tohuayuo.genesisobserver.target.ObservationTarget;
import jp.tohuayuo.genesisobserver.target.TargetFinder;
import jp.tohuayuo.genesisobserver.target.VillageFinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.Heightmap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class DroneCameraController {
    private static final Random RANDOM = new Random();
    
    private static final double SAFE_MIN_Y = 105.0;
    private static final double TRAVEL_HEIGHT = 125.0;
    private static final double APPROACH_HEIGHT = 82.0;
    private static final double ORBIT_RADIUS = 42.0;

    private static final int APPROACH_TICKS = 8 * 20;
    private static final int ORBIT_TICKS = 28 * 20;
    private static final int DEPART_TICKS = 8 * 20;

    private boolean active;
    private DroneCameraState state = DroneCameraState.TRAVEL;

    private Vec3d target = Vec3d.ZERO;
    private Vec3d departureDirection = new Vec3d(1.0, 0.0, 0.0);

    private int stateTicks;
    private double orbitAngle;
    private float smoothedYaw;
    private float smoothedPitch;

    private final TargetFinder villageFinder = new VillageFinder();

    public boolean isActive() {
        return active;
    }

    public void start(MinecraftClient client) {
    if (client.player == null || client.world == null) {
        return;
    }

    double groundY = getGroundY(
            client,
            client.player.getX(),
            client.player.getZ()
    );

    double safeStartY = groundY + 40.0;

    if (client.player.getY() < groundY + 10.0) {
        client.player.setPosition(
                client.player.getX(),
                safeStartY,
                client.player.getZ()
        );

        client.player.setVelocity(Vec3d.ZERO);
    }

    active = true;
    smoothedYaw = client.player.getYaw();
    smoothedPitch = client.player.getPitch();
    chooseNextTarget(client);
    changeState(DroneCameraState.TRAVEL);
}

public void stop() {
    active = false;
}

public void skipToNextTarget(MinecraftClient client) {
    if (!active || client.player == null || client.world == null) {
        return;
    }

    chooseNextTarget(client);
    changeState(DroneCameraState.TRAVEL);
}
    public void tick(MinecraftClient client) {
        if (!active || client.player == null || client.world == null) {
            return;
        }

        stateTicks++;

        switch (state) {
            case TRAVEL -> tickTravel(client);
            case APPROACH -> tickApproach(client);
            case ORBIT -> tickOrbit(client);
            case DEPART -> tickDepart(client);
        }
    }

    private void tickTravel(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        double groundY = getGroundY(client, target.x, target.z);

        Vec3d cruiseTarget = new Vec3d(
                target.x,
                Math.max(groundY + 60.0, TRAVEL_HEIGHT),
                target.z
        );

        moveTowards(player, cruiseTarget, 0.010, 0.55);
        lookTowards(player, new Vec3d(target.x, groundY + 8.0, target.z), 0.035f);

        if (horizontalDistance(player.getPos(), target) < 95.0) {
            changeState(DroneCameraState.APPROACH);
        }
    }

    private void tickApproach(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        double groundY = getGroundY(client, target.x, target.z);

        Vec3d approachPoint = new Vec3d(
                target.x + Math.cos(orbitAngle) * ORBIT_RADIUS,
                Math.max(groundY + 34.0, APPROACH_HEIGHT),
                target.z + Math.sin(orbitAngle) * ORBIT_RADIUS
        );

        moveTowards(player, approachPoint, 0.025, 0.90);
        lookTowards(player, new Vec3d(target.x, groundY + 6.0, target.z), 0.045f);

        if (stateTicks >= APPROACH_TICKS || player.getPos().distanceTo(approachPoint) < 4.0) {
            orbitAngle = Math.atan2(player.getZ() - target.z, player.getX() - target.x);
            changeState(DroneCameraState.ORBIT);
        }
    }

    private void tickOrbit(MinecraftClient client) {
    ClientPlayerEntity player = client.player;
    double groundY = getGroundY(client, target.x, target.z);

    Vec3d observationPoint = new Vec3d(
            target.x + 28.0,
            groundY + 18.0,
            target.z + 28.0
    );

    moveTowards(player, observationPoint, 0.04, 0.30);
    lookTowards(
            player,
            new Vec3d(target.x, groundY + 5.0, target.z),
            0.025f
    );

    if (stateTicks >= ORBIT_TICKS) {
        Vec3d away = new Vec3d(
                player.getX() - target.x,
                0.0,
                player.getZ() - target.z
        );

        departureDirection = away.lengthSquared() < 0.01
                ? new Vec3d(1.0, 0.0, 0.0)
                : away.normalize();

        changeState(DroneCameraState.DEPART);
    }
}

    private void tickDepart(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        double progress = Math.min(1.0, stateTicks / (double) DEPART_TICKS);

        Vec3d departurePoint = target
                .add(departureDirection.multiply(80.0 + progress * 120.0))
                .add(0.0, TRAVEL_HEIGHT - target.y, 0.0);

        moveTowards(player, departurePoint, 0.025, 1.30);

        Vec3d forwardLook = player.getPos()
                .add(departureDirection.multiply(80.0))
                .add(0.0, -18.0, 0.0);

        lookTowards(player, forwardLook, 0.035f);

        if (stateTicks >= DEPART_TICKS) {
            chooseNextTarget(client);
            changeState(DroneCameraState.TRAVEL);
        }
    }

    private void chooseNextTarget(MinecraftClient client) {
    ObservationTarget observation = villageFinder.find(client);

    if (observation != null) {
        target = observation.position().toCenterPos();
        orbitAngle = RANDOM.nextDouble() * Math.PI * 2.0;
        return;
    }

    ClientPlayerEntity player = client.player;

    double distance = 240.0 + RANDOM.nextDouble() * 260.0;
    double direction = RANDOM.nextDouble() * Math.PI * 2.0;

    double x = player.getX() + Math.cos(direction) * distance;
    double z = player.getZ() + Math.sin(direction) * distance;
    double groundY = getGroundY(client, x, z);

    target = new Vec3d(x, groundY, z);
    orbitAngle = RANDOM.nextDouble() * Math.PI * 2.0;
}

    private void moveTowards(
            ClientPlayerEntity player,
            Vec3d desiredPosition,
            double smoothing,
            double maximumStep
    ) {
        Vec3d current = player.getPos();
        Vec3d difference = desiredPosition.subtract(current);
        double distance = difference.length();

        if (distance < 0.001) {
            player.setVelocity(Vec3d.ZERO);
            return;
        }

        double step = Math.min(maximumStep, distance * smoothing + 0.025);
        Vec3d next = current.add(difference.normalize().multiply(step));

double safeY = getSafeFlightY(player, current, next);

player.setPosition(
        next.x,
        Math.max(next.y, safeY),
        next.z
);
        player.setVelocity(Vec3d.ZERO);
    }
private double getSafeFlightY(
        ClientPlayerEntity player,
        Vec3d current,
        Vec3d next
) {
    Vec3d movement = next.subtract(current);
    double horizontalLength = Math.hypot(movement.x, movement.z);

    double highestGroundY =
            getGroundYAtPlayerWorld(player, next.x, next.z);

    if (horizontalLength < 0.001) {
        return highestGroundY + 45.0;
    }

    double directionX = movement.x / horizontalLength;
    double directionZ = movement.z / horizontalLength;

    for (int i = 1; i <= 8; i++) {
        double lookAheadDistance = i * 12.0;

        double sampleX =
                current.x + directionX * lookAheadDistance;
        double sampleZ =
                current.z + directionZ * lookAheadDistance;

        double sampleGroundY =
                getGroundYAtPlayerWorld(player, sampleX, sampleZ);

        highestGroundY =
                Math.max(highestGroundY, sampleGroundY);
    }

    return highestGroundY + 45.0;
}
    private void lookTowards(ClientPlayerEntity player, Vec3d lookTarget, float smoothing) {
        Vec3d difference = lookTarget.subtract(player.getPos());

        double horizontal = Math.hypot(difference.x, difference.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(difference.z, difference.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(difference.y, horizontal));

        smoothedYaw = smoothedYaw
                + MathHelper.wrapDegrees(targetYaw - smoothedYaw) * smoothing;
        smoothedPitch = MathHelper.lerp(smoothing, smoothedPitch, targetPitch);

        player.setYaw(smoothedYaw);
        player.setPitch(MathHelper.clamp(smoothedPitch, -85.0f, 85.0f));
    }

    private double getGroundY(MinecraftClient client, double x, double z) {
        int groundY = client.world.getTopY(
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                MathHelper.floor(x),
                MathHelper.floor(z)
        );

        return Math.max(groundY, 64.0);
    }

    private double horizontalDistance(Vec3d first, Vec3d second) {
        return Math.hypot(first.x - second.x, first.z - second.z);
    }

    private void changeState(DroneCameraState nextState) {
        state = nextState;
        stateTicks = 0;
    }
   private double getGroundYAtPlayerWorld(
        ClientPlayerEntity player,
        double x,
        double z
) {
    int groundY = player.getWorld().getTopY(
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            MathHelper.floor(x),
            MathHelper.floor(z)
    );

    return Math.max(groundY, 64.0);
} 
}
