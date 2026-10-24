package zone.vao.boundaryGuard.integrations;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class ResidenceIntg implements Integration {
  private final PluginApi api;
  private final Object manager;
  private final Object build, destroy, use, pvp, damage, container, trade, riding;

  public ResidenceIntg(JavaPlugin owner, Plugin plugin) {
    api = new PluginApi(plugin);
    manager = api.call(plugin, "getResidenceManager");
    String flags = "com.bekvon.bukkit.residence.containers.Flags";
    build = api.constant(flags, "build");
    destroy = api.constant(flags, "destroy");
    use = api.constant(flags, "use");
    pvp = api.constant(flags, "pvp");
    damage = api.constant(flags, "damage");
    container = api.constant(flags, "container");
    trade = api.constant(flags, "trade");
    riding = api.constant(flags, "riding");
  }

  @Override public IntegrationType getType() { return IntegrationType.RESIDENCE; }
  @Override public boolean canBuild(Player player, Location location) { return canDo(player, location, build); }
  @Override public boolean canBreak(Player player, Location location) { return canDo(player, location, destroy); }
  @Override public boolean canInteract(Player player, Location location) { return canDo(player, location, use); }
  @Override public boolean canInteract(Player player, Entity entity) {
    Object flag = entity instanceof AbstractVillager ? trade
        : entity instanceof InventoryHolder || entity instanceof ItemFrame ? container
        : entity instanceof Vehicle || entity instanceof AbstractHorse ? riding : use;
    return canDo(player, entity.getLocation(), flag);
  }
  @Override public boolean canUse(Player player, Location location) { return canDo(player, location, use); }
  @Override public boolean canDamage(Player player, Entity entity) {
    return canDo(player, entity.getLocation(), entity instanceof Player ? pvp : damage);
  }

  private boolean canDo(Player player, Location location, Object flag) {
    if (player.isOp()) return true;
    Object residence = api.call(manager, "getByLoc", location);
    return residence == null || (boolean) api.call(api.call(residence, "getPermissions"), "playerHas", player, flag, false);
  }
}
