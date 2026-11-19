package io.github.gunnerhq22.chestlistview;

import io.github.gunnerhq22.chestlistview.client.ChestListClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "chestlistview", dist = Dist.CLIENT)
public final class ChestListView {
    public ChestListView() {
        ChestListClient.register();
    }
}
