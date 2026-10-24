package zone.vao.boundaryGuard.integrations;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class NoBuildPlusIntg implements Integration {
  private final PluginApi api;
  private final Object pluginApi;
  private final Object build, destroy, use, mobDamage, frame, armorStand, villager, ride;

  public NoBuildPlusIntg(JavaPlugin owner, Plugin plugin) {
    api = new PluginApi(plugin);
    pluginApi = api.call(plugin, "getAPI");
    String flags = "p1xel.nobuildplus.Flags";
    build = api.constant(flags, "build");
    destroy = api.constant(flags, "destroy");
    use = api.constant(flags, "use");
    mobDamage = api.constant(flags, "mob_damage");
    frame = api.constant(flags, "frame");
    armorStand = api.constant(flags, "armorstand");
    villager = api.constant(flags, "villager");
    ride = api.constant(flags, "ride");
  }

  @Override public IntegrationType getType() { return IntegrationType.NOBUILDPLUS; }
  @Override public boolean canBuild(Player player, Location location) { return canDo(location, build); }
  @Override public boolean canBreak(Player player, Location location) { return canDo(location, destroy); }
  @Override public boolean canInteract(Player player, Location location) { return canDo(location, use); }
  @Override public boolean canInteract(Player player, Entity entity) {
    Object flag = entity instanceof ItemFrame ? frame : entity instanceof ArmorStand ? armorStand
        : entity instanceof AbstractVillager ? villager
        : entity instanceof Vehicle || entity instanceof AbstractHorse ? ride : use;
    return canDo(entity.getLocation(), flag);
  }
  @Override public boolean canUse(Player player, Location location) { return canDo(location, use); }
  /** Uses mob_damage for all entities. */
  @Override public boolean canDamage(Player player, Entity entity) { return canDo(entity.getLocation(), mobDamage); }

  private boolean canDo(Location location, Object flag) {
    return !(boolean) api.call(pluginApi, "canExecute", location.getWorld().getName(), flag);
  }
}
