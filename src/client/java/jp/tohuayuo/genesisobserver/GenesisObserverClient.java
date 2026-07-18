package jp.tohuayuo.genesisobserver;

import jp.tohuayuo.genesisobserver.camera.DroneCameraController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenesisObserverClient implements ClientModInitializer {
    public static final String MOD_ID = "genesis_observer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final DroneCameraController droneCamera = new DroneCameraController();

    private KeyBinding toggleKey;
    private KeyBinding nextKey;

    private boolean oldHudHidden;
    private boolean spectatorCommandSent;

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
        LOGGER.info("Genesis Observer Drone Camera v0.2 loaded");
    }

    private void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            setActive(client, !droneCamera.isActive());
        }

        while (nextKey.wasPressed()) {
            if (droneCamera.isActive()) {
                droneCamera.skipToNextTarget(client);

                if (client.player != null) {
                    client.player.sendMessage(Text.literal("次の観測地点へ向かいます"), true);
                }
            }
        }

        if (!droneCamera.isActive() || client.player == null || client.world == null) {
            return;
        }

        if (!spectatorCommandSent) {
            spectatorCommandSent = true;
            client.player.networkHandler.sendChatCommand("gamemode spectator");
        }

        droneCamera.tick(client);
    }

    private void setActive(MinecraftClient client, boolean enabled) {
        if (enabled) {
            if (client.player == null || client.world == null) {
                return;
            }

            oldHudHidden = client.options.hudHidden;
            client.options.hudHidden = true;
            spectatorCommandSent = false;

            droneCamera.start(client);
            client.player.sendMessage(
                    Text.literal("ドローン観測開始：Oで停止、Pで次の地点"),
                    true
            );
            return;
        }

        droneCamera.stop();
        client.options.hudHidden = oldHudHidden;

        if (client.player != null) {
            client.player.sendMessage(Text.literal("ドローン観測停止"), true);
        }
    }
}
