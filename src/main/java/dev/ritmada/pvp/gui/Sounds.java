// Java 17+, toggle sound via id lookup (SoundEvents constants flip between SoundEvent/Holder across versions)
package dev.ritmada.pvp.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class Sounds {
    private Sounds() {}

    public static void toggle(boolean on) {
        ResourceLocation id = ResourceLocation.tryParse("minecraft:block.note_block.pling");
        if (id == null) return;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), on ? 1.6f : 0.6f, 1.0f));
    }
}
