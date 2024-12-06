package zone.vao.boundaryGuard.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

@Getter
public class WorldGuardIntg implements Integration {

  IntegrationType type = IntegrationType.WORLDGUARD;

  @Override
  public boolean canBuild(Player player, Location location) {
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.BLOCK_PLACE) ||
        WorldGuard.getInstance().getPlatform().getSessionManager()
            .hasBypass(localPlayer, BukkitAdapter.adapt(player.getWorld()));
  }

  @Override
  public boolean canBreak(Player player, Location location) {
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.BLOCK_BREAK) ||
        WorldGuard.getInstance().getPlatform().getSessionManager()
            .hasBypass(localPlayer, BukkitAdapter.adapt(player.getWorld()));
  }

  @Override
  public boolean canInteract(Player player, Location location) {
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.INTERACT)
        || WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass(localPlayer, BukkitAdapter.adapt(player.getWorld()));
  }

  @Override
  public boolean canUse(Player player, Location location) {
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.USE)
        || WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass(localPlayer, BukkitAdapter.adapt(player.getWorld()));
  }
}
