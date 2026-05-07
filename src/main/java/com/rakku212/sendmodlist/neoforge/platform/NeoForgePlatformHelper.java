package com.rakku212.sendmodlist.neoforge.platform;

import com.rakku212.sendmodlist.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
//? if >=1.21.11 {
import net.neoforged.fml.loading.FMLEnvironment;
//?} else {
/*import net.neoforged.fml.loading.FMLLoader;
*///?}

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        //? if >=1.21.11 {
        return !FMLEnvironment.isProduction();
        //?} else {
        /*return !FMLLoader.isProduction();
        *///?}
    }
}
