package anon.def9a2a4.ropes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JumpInputListener extends PacketAdapter {
    private final Set<UUID> jumpingPlayers = ConcurrentHashMap.newKeySet();

    public JumpInputListener(Plugin plugin) {
        super(plugin, PacketType.Play.Client.STEER_VEHICLE);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        PacketContainer packet = event.getPacket();
        // The STEER_VEHICLE packet (PlayerInput) has jump as the first boolean field
        boolean isJumping = packet.getBooleans().read(0);

        UUID playerId = player.getUniqueId();
        if (isJumping) {
            jumpingPlayers.add(playerId);
        } else {
            jumpingPlayers.remove(playerId);
        }
    }

    public boolean isJumping(UUID playerId) {
        return jumpingPlayers.contains(playerId);
    }

    public void removePlayer(UUID playerId) {
        jumpingPlayers.remove(playerId);
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }
}
