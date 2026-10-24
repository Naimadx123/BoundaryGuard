package zone.vao.boundaryGuard.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.entity.Animals;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class WorldGuardIntg implements Integration {
  @Override
  public IntegrationType getType() { return IntegrationType.WORLDGUARD; }

  public WorldGuardIntg() {}
  public WorldGuardIntg(JavaPlugin owner, Plugin plugin) {}

  @Override
  public boolean canBuild(Player player, Location location) {
    return checkBuild(player, location, Flags.BLOCK_PLACE);
  }

  @Override
  public boolean canBreak(Player player, Location location) {
    return checkBuild(player, location, Flags.BLOCK_BREAK);
  }

  @Override
  public boolean canInteract(Player player, Location location) {
    return checkBuild(player, location, Flags.INTERACT);
  }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    StateFlag flag = entity instanceof InventoryHolder ? Flags.CHEST_ACCESS
        : entity instanceof Vehicle || entity instanceof AbstractHorse ? Flags.RIDE : Flags.INTERACT;
    return checkBuild(player, entity.getLocation(), flag);
  }

  @Override
  public boolean canUse(Player player, Location location) {
    return checkBuild(player, location, Flags.USE);
  }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    StateFlag flag;
    if (entity instanceof Player) flag = Flags.PVP;
    else if (entity instanceof Animals) flag = Flags.DAMAGE_ANIMALS;
    else return true;

    Location location = entity.getLocation();
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    return hasBypass(localPlayer, location)
        || WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
            .testState(BukkitAdapter.adapt(location), localPlayer, flag);
  }

  private boolean checkBuild(Player player, Location location, StateFlag flag) {
    LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    return hasBypass(localPlayer, location)
        || WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
            .testBuild(BukkitAdapter.adapt(location), localPlayer, flag);
  }

  private boolean hasBypass(LocalPlayer player, Location target) {
    return WorldGuard.getInstance().getPlatform().getSessionManager()
        .hasBypass(player, BukkitAdapter.adapt(target.getWorld()));
  }
}
