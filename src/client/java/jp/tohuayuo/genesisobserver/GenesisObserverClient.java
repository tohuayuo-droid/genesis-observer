package jp.tohuayuo.genesisobserver;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.world.Heightmap;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Genesis Observer v0.1
 *
 * Oキーで開始・停止、Pキーで次の地点へ移動します。
 * シングルプレイのスペクテイターモードで使う想定です。
 */
public final class GenesisObserverClient implements ClientModInitializer {
    public static final String MOD_ID = "genesis_observer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Random RANDOM = new Random();

    private static final double ORBIT_RADIUS = 38.0;
    private static final double CAMERA_HEIGHT = 22.0;
    private static final double ORBIT_SECONDS = 36.0;
    private static final double MIN_JUMP_DISTANCE = 150.0;
    private static final double MAX_JUMP_DISTANCE = 320.0;
    private static final int STAY_TICKS = 40 * 20;

    private KeyBinding toggleKey;
    private KeyBinding nextKey;

    private boolean active;
    private boolean oldHudHidden;
    private boolean spectatorCommandSent;

    private double centerX;
    private double centerZ;
    private double angle;
    private int ticksAtPoint;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.genesis_observer.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "key.category.genesis_observer"
        ));

        nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.genesis_observer.next",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.category.genesis_observer"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        LOGGER.info("Genesis Observer v0.1 loaded");
    }

    private void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            setActive(client, !active);
        }

        while (nextKey.wasPressed()) {
            if (client.player != null && client.world != null) {
                chooseNextPoint(client);
                client.player.sendMessage(Text.literal("次の観測地点へ移動します"), true);
            }
        }

        if (!active || client.player == null || client.world == null) {
            return;
        }

        // 初回だけスペクテイターモードを要求します。
        // チートを許可したシングルプレイワールドで動作します。
        if (!spectatorCommandSent) {
            spectatorCommandSent = true;
            client.player.networkHandler.sendChatCommand("gamemode spectator");
        }

        ticksAtPoint++;
        if (ticksAtPoint >= STAY_TICKS) {
            chooseNextPoint(client);
        }

        double radiansPerTick = Math.PI * 2.0 / (ORBIT_SECONDS * 20.0);
        angle += radiansPerTick;

        double x = centerX + Math.cos(angle) * ORBIT_RADIUS;
        double z = centerZ + Math.sin(angle) * ORBIT_RADIUS;

        int groundY = client.world.getTopY(
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(centerX),
                (int) Math.floor(centerZ)
        );
        // 未読み込みチャンクでは高さ情報が世界の最下部になることがあります。
        // カメラが地中へ潜らないよう、最低安全高度を設定します。
        double safeMinimumY = 110.0;
        double y = Math.max(groundY + CAMERA_HEIGHT, safeMinimumY);

        double lookY = Math.max(groundY + 4.0, 64.0);
        float yaw = (float) (Math.toDegrees(Math.atan2(centerZ - z, centerX - x)) - 90.0);
        double horizontalDistance = Math.hypot(centerX - x, centerZ - z);
        float pitch = (float) Math.toDegrees(Math.atan2(y - lookY, horizontalDistance));

        // スペクテイターのプレイヤーを滑らかに移動させます。
        client.player.setPosition(x, y, z);
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
        client.player.setVelocity(0.0, 0.0, 0.0);
    }

    private void setActive(MinecraftClient client, boolean enabled) {
        if (enabled && (client.player == null || client.world == null)) {
            return;
        }

        active = enabled;

        if (active) {
            oldHudHidden = client.options.hudHidden;
            client.options.hudHidden = true;
            spectatorCommandSent = false;

            centerX = client.player.getX();
            centerZ = client.player.getZ();
            angle = 0.0;
            ticksAtPoint = 0;

            client.player.sendMessage(
                    Text.literal("Genesis Observer開始：Oで停止、Pで次の地点"),
                    true
            );
        } else {
            client.options.hudHidden = oldHudHidden;
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Genesis Observer停止"), true);
            }
        }
    }

    private void chooseNextPoint(MinecraftClient client) {
        double distance = MIN_JUMP_DISTANCE
                + RANDOM.nextDouble() * (MAX_JUMP_DISTANCE - MIN_JUMP_DISTANCE);
        double direction = RANDOM.nextDouble() * Math.PI * 2.0;

        centerX = client.player.getX() + Math.cos(direction) * distance;
        centerZ = client.player.getZ() + Math.sin(direction) * distance;
        angle = RANDOM.nextDouble() * Math.PI * 2.0;
        ticksAtPoint = 0;
    }
}
