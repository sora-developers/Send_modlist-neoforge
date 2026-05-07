package com.rakku212.sendmodlist;

import net.minecraft.resources.Identifier;

public final class ModListProtocol {
    public static final String CHANNEL_NAMESPACE = "send_modlist";
    public static final String CHANNEL_PATH = "modlist";
    //? if <1.20.6 {
    /*public static final ResourceLocation CHANNEL_ID =
            new ResourceLocation(CHANNEL_NAMESPACE, CHANNEL_PATH);
    *///?} else {
    public static final Identifier CHANNEL_ID =
            Identifier.fromNamespaceAndPath(CHANNEL_NAMESPACE, CHANNEL_PATH);
    //?}

    public static final String REQUEST_MODLIST = "REQUEST_MODLIST";
    public static final String REQUEST_MODLIST_SEPARATOR = "|";
    public static final String MODLIST_ACCEPTED = "MODLIST_ACCEPTED";
    public static final int MAX_PLUGIN_MESSAGE_SIZE = 32767;
}
