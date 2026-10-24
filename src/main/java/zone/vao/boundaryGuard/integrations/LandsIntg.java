package zone.vao.boundaryGuard.integrations;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import org.bukkit.entity.Monster;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Vehicle;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class LandsIntg implements Integration {
  @Override
  public IntegrationType getType() { return IntegrationType.LANDS; }

  private final LandsIntegration lands;

  public LandsIntg(JavaPlugin owner, Plugin plugin) { lands = LandsIntegration.of(owner); }

  @Override
  public boolean canBuild(Player player, Location location) { return hasFlag(player, location, Flags.BLOCK_PLACE); }

  @Override
  public boolean canBreak(Player player, Location location) { return hasFlag(player, location, Flags.BLOCK_BREAK); }

  @Override
  public boolean canInteract(Player player, Location location) { return hasFlag(player, location, Flags.INTERACT_GENERAL); }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    RoleFlag flag = entity instanceof AbstractVillager ? Flags.INTERACT_VILLAGER
        : entity instanceof Vehicle || entity instanceof AbstractHorse ? Flags.VEHICLE_USE : Flags.INTERACT_GENERAL;
    return hasFlag(player, entity.getLocation(), flag);
  }

  @Override
  public boolean canUse(Player player, Location location) { return hasFlag(player, location, Flags.INTERACT_GENERAL); }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    RoleFlag flag = entity instanceof Player ? Flags.ATTACK_PLAYER
        : entity instanceof Monster ? Flags.ATTACK_MONSTER : Flags.ATTACK_ANIMAL;
    return hasFlag(player, entity.getLocation(), flag);
  }

  private boolean hasFlag(Player player, Location location, RoleFlag flag) {
    var world = lands.getWorld(location.getWorld());
    return world == null || world.hasRoleFlag(lands.getLandPlayer(player.getUniqueId()), location, flag, null, true);
  }
}
