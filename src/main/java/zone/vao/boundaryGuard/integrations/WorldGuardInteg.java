package zone.vao.boundaryGuard.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zone.vao.boundaryGuard.BoundaryGuard;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

@Getter
public class WorldGuardInteg extends BoundaryGuard implements Integration {

  IntegrationType type = IntegrationType.WORLDGUARD;

  public WorldGuardInteg(Location location) {
    super(location);
  }

  @Override
  public boolean canBuild(Player player) {
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(getLocation()), (LocalPlayer) player, Flags.BLOCK_PLACE)
        || WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass((LocalPlayer) player, BukkitAdapter.adapt(player.getWorld()));
  }

  @Override
  public boolean canBreak(Player player) {
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(getLocation()), (LocalPlayer) player, Flags.BLOCK_BREAK)
        || WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass((LocalPlayer) player, BukkitAdapter.adapt(player.getWorld()));
  }

  @Override
  public boolean canInteract(Player player) {
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(getLocation()), (LocalPlayer) player, Flags.INTERACT)
        || WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass((LocalPlayer) player, BukkitAdapter.adapt(player.getWorld()));
  }

  @Override
  public boolean canUse(Player player) {
    return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
        .testBuild(BukkitAdapter.adapt(getLocation()), (LocalPlayer) player, Flags.USE)
        || WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass((LocalPlayer) player, BukkitAdapter.adapt(player.getWorld()));
  }
}
