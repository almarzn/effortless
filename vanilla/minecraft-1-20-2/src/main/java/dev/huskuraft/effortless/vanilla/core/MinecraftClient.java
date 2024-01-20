package dev.huskuraft.effortless.vanilla.core;

import com.mojang.blaze3d.platform.InputConstants;
import dev.huskuraft.effortless.api.core.*;
import dev.huskuraft.effortless.api.gui.Screen;
import dev.huskuraft.effortless.api.gui.Typeface;
import dev.huskuraft.effortless.api.platform.Client;
import dev.huskuraft.effortless.api.renderer.Camera;
import dev.huskuraft.effortless.api.renderer.Window;
import dev.huskuraft.effortless.vanilla.renderer.MinecraftCamera;
import dev.huskuraft.effortless.vanilla.renderer.MinecraftWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class MinecraftClient implements Client {

    private final Minecraft reference;

    public MinecraftClient(Minecraft reference) {
        this.reference = reference;
    }

    @Override
    public Minecraft referenceValue() {
        return reference;
    }

    @Override
    public Window window() {
        return new MinecraftWindow(reference.getWindow());
    }

    @Override
    public Camera camera() {
        return new MinecraftCamera(reference.gameRenderer.getMainCamera());
    }

    @Override
    public Screen getPanel() {
        if (reference.screen == null) {
            return null;
        }
        if (reference.screen instanceof MinecraftProxyScreen proxyScreen) {
            return proxyScreen.getProxy();
        }
        return new MinecraftScreen(reference.screen);
    }

    @Override
    public void setPanel(Screen screen) {
        if (screen == null) {
            reference.setScreen(null);
            return;
        }
        if (screen instanceof MinecraftScreen minecraftScreen) {
            reference.setScreen(minecraftScreen.getReference());
            return;
        }
        reference.setScreen(new MinecraftProxyScreen(screen));

    }

    @Override
    public Player getPlayer() {
        if (reference.player == null) {
            return null;
        }
        return new MinecraftPlayer(reference.player);
    }

    @Override
    public Typeface getTypeface() {
        return new MinecraftTypeface(reference.font);
    }

    @Override
    public World getWorld() {
        return new MinecraftWorld(reference.level);
    }

    @Override
    public boolean isLoaded() {
        return getWorld() != null;
    }

    @Override
    public Interaction getLastInteraction() {
        return MinecraftConvertor.fromPlatformInteraction(reference.hitResult);
    }

    @Override
    public boolean hasControlDown() {
        return net.minecraft.client.gui.screens.Screen.hasControlDown();
    }

    @Override
    public boolean hasShiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    @Override
    public boolean hasAltDown() {
        return net.minecraft.client.gui.screens.Screen.hasAltDown();
    }

    @Override
    public boolean isCut(int key) {
        return net.minecraft.client.gui.screens.Screen.isCut(key);
    }

    @Override
    public boolean isPaste(int key) {
        return net.minecraft.client.gui.screens.Screen.isPaste(key);
    }

    @Override
    public boolean isCopy(int key) {
        return net.minecraft.client.gui.screens.Screen.isCopy(key);
    }

    @Override
    public boolean isSelectAll(int key) {
        return net.minecraft.client.gui.screens.Screen.isSelectAll(key);
    }

    @Override
    public boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(reference.getWindow().getWindow(), key);
    }

    @Override
    public boolean isMouseButtonDown(int button) {
        return GLFW.glfwGetMouseButton(reference.getWindow().getWindow(), button) == 1;
    }

    @Override
    public String getClipboard() {
        return reference.keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String content) {
        reference.keyboardHandler.setClipboard(content);
    }

    @Override
    public void playButtonClickSound() {
        reference.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public Resource getResource(ResourceLocation location) {
        var resource = reference.getResourceManager().getResource(location.reference());
        return resource.map(value -> new MinecraftResource(value, location.reference())).orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MinecraftClient client && reference.equals(client.reference);
    }

    @Override
    public int hashCode() {
        return reference.hashCode();
    }
}