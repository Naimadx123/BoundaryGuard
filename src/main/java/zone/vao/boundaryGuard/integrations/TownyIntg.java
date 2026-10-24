package zone.vao.boundaryGuard.integrations;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.util.EntityLists;
import com.palmergames.bukkit.util.ItemLists;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class TownyIntg implements Integration {
  @Override
  public IntegrationType getType() { return IntegrationType.TOWNY; }

  public TownyIntg() {}
  public TownyIntg(JavaPlugin owner, Plugin plugin) {}

  @Override
  public boolean canBuild(Player player, Location location) {
    return hasPermission(player, location, TownyPermission.ActionType.BUILD);
  }

  @Override
  public boolean canPlace(Player player, Location location, Material material) {
    return PlayerCacheUtil.getCachePermission(player, location, material, ActionType.BUILD);
  }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    Location location = entity.getLocation();
    if (!TownyAPI.getInstance().isTownyWorld(location.getWorld())) return true;
    EntityType type = entity.getType();
    Material item = player.getInventory().getItemInMainHand().getType();
    Material material = null;
    ActionType action = ActionType.DESTROY;
    if (EntityLists.SWITCH_PROTECTED.contains(type)) {
      material = EntityTypeUtil.parseEntityToMaterial(type);
      action = ActionType.SWITCH;
    } else if (EntityLists.DYEABLE.contains(type) && ItemLists.DYES.contains(item)) {
      material = item;
    } else if (item == Material.BUCKET && EntityLists.MILKABLE.contains(type)) {
      material = EntityTypeUtil.parseEntityToMaterial(type);
      action = ActionType.ITEM_USE;
    } else if (item == Material.COOKIE && type == EntityType.PARROT
        || EntityLists.RIGHT_CLICK_PROTECTED.contains(type)) {
      material = EntityTypeUtil.parseEntityToMaterial(type);
    }
    if (material != null && (action == ActionType.DESTROY || action == ActionType.ITEM_USE
        || TownySettings.isSwitchMaterial(material, location))) {
      return PlayerCacheUtil.getCachePermission(player, location, material, action);
    }
    if (item == Material.SHEARS && type == EntityType.SHEEP && !TownyAPI.getInstance().isWilderness(location)
        || item == Material.NAME_TAG) {
      return PlayerCacheUtil.getCachePermission(player, location, item, ActionType.DESTROY);
    }
    return !TownySettings.isItemUseMaterial(item, location)
        || PlayerCacheUtil.getCachePermission(player, location, item, ActionType.ITEM_USE);
  }

  @Override
  public boolean canBreak(Player player, Location location) {
    return hasPermission(player, location, TownyPermission.ActionType.DESTROY);
  }

  @Override
  public boolean canInteract(Player player, Location location) {
    return hasPermission(player, location, TownyPermission.ActionType.SWITCH);
  }

  @Override
  public boolean canUse(Player player, Location location) {
    return hasPermission(player, location, TownyPermission.ActionType.ITEM_USE);
  }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    return !CombatUtil.preventDamageCall(player, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
  }

  private boolean hasPermission(Player player, Location location, TownyPermission.ActionType action) {
    return PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), action);
  }
}
