package com.simplequarries.client;

import com.simplequarries.SimpleQuarries;
import com.simplequarries.screen.QuarryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class SimpleQuarriesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(SimpleQuarries.QUARRY_SCREEN_HANDLER, QuarryScreen::new);
    }
}
