package zone.vao.boundaryGuard.integrations;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Hanging;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class GriefPreventionIntg implements Integration {
  @Override
  public IntegrationType getType() { return IntegrationType.GRIEFPREVENTION; }

  private final GriefPrevention plugin;

  public GriefPreventionIntg(JavaPlugin owner, Plugin plugin) { this.plugin = (GriefPrevention) plugin; }

  @Override
  public boolean canBuild(Player player, Location location) { return hasPermission(player, location, ClaimPermission.Build); }

  @Override
  public boolean canPlace(Player player, Location location, Material material) {
    return plugin.allowBuild(player, location, material) == null;
  }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    ClaimPermission permission = entity instanceof Hanging || entity instanceof ArmorStand ? ClaimPermission.Build
        : entity instanceof InventoryHolder || entity instanceof Animals || entity instanceof AbstractVillager
            ? ClaimPermission.Inventory : ClaimPermission.Access;
    return hasPermission(player, entity.getLocation(), permission);
  }

  @Override
  public boolean canBreak(Player player, Location location) { return hasPermission(player, location, ClaimPermission.Build); }

  @Override
  public boolean canInteract(Player player, Location location) { return hasPermission(player, location, ClaimPermission.Access); }

  @Override
  public boolean canUse(Player player, Location location) { return hasPermission(player, location, ClaimPermission.Access); }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    return hasPermission(player, entity.getLocation(),
        entity instanceof Player ? ClaimPermission.Inventory : ClaimPermission.Build);
  }

  private boolean hasPermission(Player player, Location location, ClaimPermission permission) {
    var data = plugin.dataStore.getPlayerData(player.getUniqueId());
    if (data.ignoreClaims) return true;
    var claim = plugin.dataStore.getClaimAt(location, false, data.lastClaim);
    if (claim == null) return true;
    data.lastClaim = claim;
    return claim.checkPermission(player, permission, null) == null;
  }
}
