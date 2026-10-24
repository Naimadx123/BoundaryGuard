package zone.vao.boundaryGuard.integrations;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class CrashClaimIntg implements Integration {
  private final PluginApi api;
  private final Object claims, bypass, build, interactions, entities;

  public CrashClaimIntg(JavaPlugin owner, Plugin plugin) {
    api = new PluginApi(plugin);
    claims = api.create("net.crashcraft.crashclaim.api.CrashClaimAPI",
        api.type("net.crashcraft.crashclaim.CrashClaim"), plugin);
    bypass = api.call(api.call(claims, "getPermissionHelper"), "getBypassManager");
    String routes = "net.crashcraft.crashclaim.permissions.PermissionRoute";
    build = api.constant(routes, "BUILD");
    interactions = api.constant(routes, "INTERACTIONS");
    entities = api.constant(routes, "ENTITIES");
  }

  @Override public IntegrationType getType() { return IntegrationType.CRASHCLAIM; }
  @Override public boolean canBuild(Player player, Location location) { return hasPermission(player, location, build); }
  @Override public boolean canBreak(Player player, Location location) { return hasPermission(player, location, build); }
  @Override public boolean canInteract(Player player, Location location) { return hasPermission(player, location, interactions); }
  @Override public boolean canInteract(Player player, Entity entity) {
    return hasPermission(player, entity.getLocation(), entities);
  }
  @Override public boolean canUse(Player player, Location location) { return hasPermission(player, location, interactions); }
  @Override public boolean canDamage(Player player, Entity entity) { return hasPermission(player, entity.getLocation(), entities); }

  private boolean hasPermission(Player player, Location location, Object route) {
    if ((boolean) api.call(bypass, "isBypass", player.getUniqueId())) return true;
    Object claim = api.call(claims, "getClaim", location);
    return claim == null || (boolean) api.call(claim, "hasPermission", player.getUniqueId(), location, route);
  }
}
