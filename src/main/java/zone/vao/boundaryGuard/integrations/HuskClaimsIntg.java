package zone.vao.boundaryGuard.integrations;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;
import java.util.Collection;
import java.util.Optional;

/** Supports the HuskClaims 1.4 API. */
public class HuskClaimsIntg implements Integration {
  private final PluginApi api;
  private final Object common, bukkit, place, destroy, interact, entityInteract, playerDamage, entityDamage;

  public HuskClaimsIntg(JavaPlugin owner, Plugin plugin) {
    api = new PluginApi(plugin);
    String root = "net.william278.huskclaims";
    common = api.call(api.type(root + ".api.HuskClaimsAPI"), "getInstance");
    bukkit = api.call(api.type(root + ".api.BukkitHuskClaimsAPI"), "getInstance");
    String operations = root + ".libraries.cloplib.operation.OperationType";
    place = api.constant(operations, "BLOCK_PLACE");
    destroy = api.constant(operations, "BLOCK_BREAK");
    interact = api.constant(operations, "BLOCK_INTERACT");
    entityInteract = api.constant(operations, "ENTITY_INTERACT");
    playerDamage = api.constant(operations, "PLAYER_DAMAGE_PLAYER");
    entityDamage = api.constant(operations, "PLAYER_DAMAGE_ENTITY");
  }

  @Override public IntegrationType getType() { return IntegrationType.HUSKCLAIMS; }
  @Override public boolean canBuild(Player player, Location location) { return hasOperation(player, location, place); }
  @Override public boolean canBreak(Player player, Location location) { return hasOperation(player, location, destroy); }
  @Override public boolean canInteract(Player player, Location location) {
    return hasOperation(player, location, interact);
  }
  @Override public boolean canInteract(Player player, Entity entity) {
    return hasOperation(player, entity.getLocation(), entityInteract);
  }
  @Override public boolean canUse(Player player, Location location) { return hasOperation(player, location, interact); }
  @Override public boolean canDamage(Player player, Entity entity) {
    return hasOperation(player, entity.getLocation(), entity instanceof Player ? playerDamage : entityDamage);
  }

  private boolean hasOperation(Player player, Location location, Object operation) {
    Object user = api.call(common, "getOnlineUser", player.getUniqueId());
    Object position = api.call(bukkit, "getPosition", location);
    Optional<?> trust = (Optional<?>) api.call(bukkit, "getTrustLevelAt", position, user);
    return trust.isEmpty() || ((Collection<?>) api.call(trust.get(), "getFlags")).contains(operation);
  }
}
