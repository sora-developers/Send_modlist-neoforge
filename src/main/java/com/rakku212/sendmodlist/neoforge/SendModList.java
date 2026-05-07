package com.rakku212.sendmodlist.neoforge;

import com.rakku212.sendmodlist.CommonClass;
import com.rakku212.sendmodlist.Constants;
import com.rakku212.sendmodlist.ModListHmac;
import com.rakku212.sendmodlist.ModListPayloadBuilder;
import com.rakku212.sendmodlist.ModListProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
//? if >=1.20.6 {
import net.minecraft.network.codec.StreamCodec;
//?}
//? if <1.20.6 {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
//? if >=1.20.6 {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
import net.neoforged.fml.common.Mod;
//? if >=1.21.11 {
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
//?} else if >=1.20.6 {
/*import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
*///?}
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
//? if >=1.20.6 {
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
//? if >=1.20.6 {
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
//?}
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Mod(Constants.MOD_ID)
public class SendModList {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_NAME);

    public SendModList(IEventBus modEventBus) {
        CommonClass.init();
        //? if >=1.20.6 {
        modEventBus.addListener(SendModList::onRegisterPayloads);
        //?}
        //? if >=1.21.11 {
        if (FMLEnvironment.getDist().isClient()) {
            modEventBus.addListener(SendModList::onRegisterClientPayloadHandlers);
        }
        //?}
    }

    //? if >=1.20.6 {
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).optional();
        //? if >=1.21.11 {
        registrar.commonBidirectional(
                ModListPayload.ID,
                ModListPayload.STREAM_CODEC,
                (payload, context) -> {});
        //?} else if >=1.20.6 {
        /*registrar.commonBidirectional(
                ModListPayload.ID,
                ModListPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(SendModList::handlePayloadFromServer, (payload, context) -> {}));
        *///?} else {
        /*registrar.common(ModListPayload.ID, ModListPayload::new, handler -> handler
                .client(SendModList::handlePayloadFromServer)
                .server((payload, ctx) -> {}));
        *///?}
    }
    //?}

    //? if >=1.21.11 {
    private static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(ModListPayload.ID, SendModList::handlePayloadFromServer);
    }
    //?}

    //? if >=1.20.6 {
    static void handlePayloadFromServer(ModListPayload payload, IPayloadContext ctx) {
        //? if >=1.21.1 {
        boolean fromConfiguration = ctx.protocol() == ConnectionProtocol.CONFIGURATION;
        //?} else {
        /*boolean fromConfiguration = false;
        *///?}
        //? if >=1.21.1 {
        ctx.enqueueWork(() -> ClientState.handleServerMessage(
                new String(payload.payloadBytes(), StandardCharsets.UTF_8).trim(),
                fromConfiguration));
        //?} else {
        /*ClientState.handleServerMessage(
                new String(payload.payloadBytes(), StandardCharsets.UTF_8).trim(),
                fromConfiguration);
        *///?}
    }
    //?}

    //? if >=1.20.6 {
    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    //?} else {
    /*@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    *///?}
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
            ClientState.renderOverlayOnScreen(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height);
        }

        @SubscribeEvent
        public static void onRenderGuiPost(RenderGuiEvent.Post event) {
            ClientState.renderOverlayOnHud(event.getGuiGraphics());
            ClientState.tryDeliverModListPayload();
        }

        //? if >=1.20.6 {
        @SubscribeEvent
        public static void onClientTickPost(ClientTickEvent.Post event) {
            ClientState.tryDeliverModListPayload();
        }
        //?}

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientState.resetModListSession();
        }
    }

    static final class ClientState {
        private static final Component VERIFYING_MODLIST_MESSAGE = Component.literal("mod検証中...");
        private static boolean awaitingModListAccepted;
        private static boolean modListPayloadDelivered;
        private static byte[] sessionHmacKey;
        private static byte[] pendingModListPayloadBytes;
        private static boolean modListPayloadBuildFailed;
        private static int trySendLogCooldown;

        private ClientState() {
        }

        static void resetModListSession() {
            awaitingModListAccepted = false;
            modListPayloadDelivered = false;
            sessionHmacKey = null;
            pendingModListPayloadBytes = null;
            modListPayloadBuildFailed = false;
            trySendLogCooldown = 0;
        }

        static void handleServerMessage(String message, @SuppressWarnings("unused") boolean fromConfiguration) {
            String requestPrefix =
                    ModListProtocol.REQUEST_MODLIST + ModListProtocol.REQUEST_MODLIST_SEPARATOR;
            if (message.startsWith(requestPrefix)) {
                String keyB64 = message.substring(requestPrefix.length());
                byte[] decodedKey;
                try {
                    decodedKey = Base64.getDecoder().decode(keyB64);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid HMAC session key (base64): {}", e.getMessage());
                    return;
                }
                if (decodedKey.length == 0) {
                    LOGGER.warn("Empty HMAC session key");
                    return;
                }
                sessionHmacKey = decodedKey;
                awaitingModListAccepted = true;
                modListPayloadDelivered = false;
                pendingModListPayloadBytes = null;
                modListPayloadBuildFailed = false;
                trySendLogCooldown = 0;
                return;
            }

            if (ModListProtocol.REQUEST_MODLIST.equals(message)) {
                LOGGER.warn("Received REQUEST_MODLIST without HMAC session key; server requires signed payload");
                return;
            }
            if (ModListProtocol.MODLIST_ACCEPTED.equals(message)) {
                clearVerifyingOverlay();
                return;
            }
            LOGGER.debug("Received unknown modlist channel message: {}", message);
        }

        private static void clearVerifyingOverlay() {
            if (!awaitingModListAccepted) {
                return;
            }
            resetModListSession();
        }

        static void renderOverlayOnScreen(GuiGraphics graphics, int width, int height) {
            if (!awaitingModListAccepted) {
                return;
            }
            fillOverlay(graphics, width, height);
        }

        static void renderOverlayOnHud(GuiGraphics graphics) {
            if (!awaitingModListAccepted) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            if (client.screen != null) {
                return;
            }
            int w = client.getWindow().getGuiScaledWidth();
            int h = client.getWindow().getGuiScaledHeight();
            fillOverlay(graphics, w, h);
        }

        private static void fillOverlay(GuiGraphics graphics, int width, int height) {
            graphics.fill(0, 0, width, height, 0xC0101010);
            graphics.drawCenteredString(Minecraft.getInstance().font, VERIFYING_MODLIST_MESSAGE, width / 2, height / 2 - 9, 0xFFFFFF);
        }

        static void tryDeliverModListPayload() {
            if (!awaitingModListAccepted || modListPayloadDelivered) {
                return;
            }
            if (modListPayloadBuildFailed) {
                return;
            }
            if (pendingModListPayloadBytes == null) {
                pendingModListPayloadBytes = buildModListPayloadBytes();
                if (pendingModListPayloadBytes == null) {
                    modListPayloadBuildFailed = true;
                    return;
                }
            }

            ModListPayload payload = new ModListPayload(pendingModListPayloadBytes);
            ClientPacketListener listener = Minecraft.getInstance().getConnection();
            if (listener == null) {
                bumpWaitLog();
                return;
            }

            try {
                sendRaw(payload);
                modListPayloadDelivered = true;
                LOGGER.info("Sent mod list payload (play): {} bytes", pendingModListPayloadBytes.length);
            } catch (Exception e) {
                LOGGER.warn("Play modlist send failed (retry next tick): {}", e.getMessage());
            }
        }

        private static void bumpWaitLog() {
            if (trySendLogCooldown > 0) {
                trySendLogCooldown--;
            } else {
                trySendLogCooldown = 100;
                LOGGER.debug("Waiting for play connection before modlist send");
            }
        }

        private static byte[] buildModListPayloadBytes() {
            List<ModListPayloadBuilder.ModInfo> mods = new ArrayList<>();
            ModList.get().getMods().forEach(modInfo ->
                    mods.add(new ModListPayloadBuilder.ModInfo(
                            modInfo.getModId(),
                            modInfo.getDisplayName(),
                            modInfo.getVersion().toString(),
                            "CLIENT"
                    ))
            );
            String loaderVersion = ModList.get()
                    .getModContainerById("neoforge")
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");
            String jsonBody = ModListPayloadBuilder
                    .build("neoforge", loaderVersion, mods)
                    .toString();
            if (sessionHmacKey == null) {
                LOGGER.warn("Cannot build mod list payload: no HMAC session key");
                return null;
            }
            byte[] payloadBytes = ModListHmac.utf8SignedPayload(sessionHmacKey, jsonBody);
            if (payloadBytes.length > ModListProtocol.MAX_PLUGIN_MESSAGE_SIZE) {
                LOGGER.warn("Mod list payload is too large to send: {} bytes", payloadBytes.length);
                return null;
            }
            return payloadBytes;
        }

        private static void sendRaw(ModListPayload payload) throws Exception {
            ClientPacketListener packetListener = Minecraft.getInstance().getConnection();
            if (packetListener == null) {
                throw new IllegalStateException("No connection");
            }
            Field connectionField = ClientCommonPacketListenerImpl.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            Connection rawConnection = (Connection) connectionField.get(packetListener);
            rawConnection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }

    public record ModListPayload(byte[] payloadBytes) implements CustomPacketPayload {
        //? if <1.20.6 {
        /*public static final ResourceLocation ID = ModListProtocol.CHANNEL_ID;

        public ModListPayload(FriendlyByteBuf buf) {
            this(readBuf(buf));
        }

        private static byte[] readBuf(FriendlyByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBytes(payloadBytes);
        }

        @Override
        public @NotNull ResourceLocation id() {
            return ID;
        }
        *///?} else {
        public static final CustomPacketPayload.Type<ModListPayload> ID =
                new CustomPacketPayload.Type<>(ModListProtocol.CHANNEL_ID);
        public static final StreamCodec<FriendlyByteBuf, ModListPayload> STREAM_CODEC = StreamCodec.of(
                (FriendlyByteBuf buf, ModListPayload payload) -> buf.writeBytes(payload.payloadBytes()),
                buf -> {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return new ModListPayload(bytes);
                });

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
        //?}
    }
}
