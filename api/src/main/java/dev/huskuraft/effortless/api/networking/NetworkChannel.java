package dev.huskuraft.effortless.api.networking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import dev.huskuraft.effortless.api.core.Player;
import dev.huskuraft.effortless.api.core.ResourceLocation;
import dev.huskuraft.effortless.api.platform.Entrance;
import dev.huskuraft.effortless.api.platform.PlatformLoader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public abstract class NetworkChannel<P extends PacketListener> implements PacketChannel {

    private final Entrance entrance;
    private final String name;
    private final Side side;
    private final Map<UUID, Consumer<? extends ResponsiblePacket<?>>> responseMap = Collections.synchronizedMap(new HashMap<>());
    private PacketSet<P> packetSet = new PacketSet<>();
    private Networking networking;

    protected NetworkChannel(Entrance entrance, String name, Side side) {
        this.entrance = entrance;
        this.name = name;
        this.side = side;
        this.networking = PlatformLoader.getSingleton();
    }

    public Networking getPlatformChannel() {
        return networking;
    }

    @Override
    public void sendPacket(Packet packet, Player player) {
        sendBuffer(createBuffer(packet), player);
    }

    @Override
    public void sendBuffer(ByteBuf byteBuf, Player player) {
        switch (side) {
            case CLIENT -> getPlatformChannel().sendToServer(getSideChannelId(side), byteBuf, player);
            case SERVER -> getPlatformChannel().sendToClient(getSideChannelId(side), byteBuf, player);
        }
    }

    public <T extends ResponsiblePacket<?>> void sendPacket(T packet, Consumer<T> callback) {
        responseMap.put(packet.responseId(), callback);
        sendPacket(packet);
    }

    @Override
    public abstract void receivePacket(Packet packet, Player player);

    @Override
    public void receiveBuffer(ByteBuf byteBuf, Player player) {
        var packet = (Packet<P>) null;
        try {
            packet = createPacket(byteBuf);
            Objects.requireNonNull(packet);
        } catch (Exception e) {
            throw new RuntimeException("Could not create packet in channel '" + getSideChannelId(side) + "'", e);
        }
        try {
            var packet1 = packet;
            receivePacket(packet1, player);
            if (packet instanceof ResponsiblePacket<P> responsiblePacket) {
                var callback = responseMap.remove(responsiblePacket.responseId());
                if (callback != null) {
                    ((Consumer<ResponsiblePacket<P>>) callback).accept(responsiblePacket);
                }
            }
        } catch (RejectedExecutionException | ClassCastException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public Packet<P> createPacket(ByteBuf byteBuf) {
        return (Packet<P>) packetSet.createPacket(byteBuf);
    }

    public ByteBuf createBuffer(Packet<P> packet) {
        return packetSet.createBuffer(packet);
    }

    public <T extends Packet<P>> void registerPacket(Class<T> clazz, NetByteBufSerializer<T> serializer) {
        try {
            packetSet.addPacket(clazz, serializer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterPackets() {
        this.packetSet = new PacketSet<>();
    }

    public abstract int getCompatibilityVersion();

    public String getCompatibilityVersionStr() {
        return Integer.toString(getCompatibilityVersion());
    }

    public final ResourceLocation getSideChannelId(Side side) {
        return switch (side) {
            case CLIENT -> ResourceLocation.of(entrance.getId(), name + "_c2s");
            case SERVER -> ResourceLocation.of(entrance.getId(), name + "_s2c");
        };
    }

    public final ResourceLocation getChannelId() {
        return ResourceLocation.of("effortless", "default_channel");
    }

    private class PacketSet<T extends PacketListener> {

        private final Map<Class<?>, Integer> classToId = new LinkedHashMap<>();
        private final List<NetByteBufSerializer<? extends Packet<T>>> idToDeserializer = new ArrayList<>();

        public <P extends Packet<T>> PacketSet<T> addPacket(Class<P> clazz, NetByteBufSerializer<P> serializer) {

            if (classToId.containsKey(clazz)) {
                throw new IllegalArgumentException("Packet " + clazz + " is already registered to ID " + classToId.get(clazz));
            } else {
                classToId.put(clazz, idToDeserializer.size());
                idToDeserializer.add(serializer);
                return this;
            }
        }

        @Nullable
        public Integer getId(Class<?> clazz) {
            return classToId.getOrDefault(clazz, null);
        }

        public ByteBuf createBuffer(Packet<T> packet) {
            var id = getId(packet.getClass());
            if (id == null) {
                throw new IllegalArgumentException("Packet " + packet.getClass() + " is not registered");
            }
            var buffer = Unpooled.buffer();
            var serializer = (NetByteBufSerializer<Packet<T>>) idToDeserializer.get(getId(packet.getClass()));
            buffer.writeInt(id);
            serializer.write(new NetByteBuf(buffer), packet);
            return buffer;
        }

        @Nullable
        public Packet<?> createPacket(ByteBuf byteBuf) {
            var id = byteBuf.readInt();
            var serializer = idToDeserializer.get(id);
            if (serializer != null) return serializer.read(new NetByteBuf(byteBuf));
            return null;
        }
    }

}
