package com.simplequarries.client;

import com.simplequarries.SimpleQuarries;
import com.simplequarries.screen.QuarryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class SimpleQuarriesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(SimpleQuarries.QUARRY_SCREEN_HANDLER, QuarryScreen::new);
    }
}
