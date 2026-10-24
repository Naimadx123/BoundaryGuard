package zone.vao.boundaryGuard.integrations;

import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.listeners.FactionsBlockListener;
import com.massivecraft.factions.listeners.FactionsEntityListener;
import com.massivecraft.factions.listeners.FactionsPlayerListener;
import com.massivecraft.factions.perms.PermissibleActions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class FactionsUuidIntg implements Integration {
  @Override
  public IntegrationType getType() { return IntegrationType.FACTIONS; }

  private final FactionsPlugin plugin;

  public FactionsUuidIntg(JavaPlugin owner, Plugin plugin) {
    this.plugin = (FactionsPlugin) plugin;
  }

  @Override
  public boolean canBuild(Player player, Location location) {
    return !plugin.worldUtil().isEnabled(location.getWorld())
        || FactionsBlockListener.playerCanBuildDestroyBlock(player, location, PermissibleActions.BUILD, false);
  }

  @Override
  public boolean canBreak(Player player, Location location) {
    return !plugin.worldUtil().isEnabled(location.getWorld())
        || FactionsBlockListener.playerCanBuildDestroyBlock(player, location, PermissibleActions.DESTROY, false);
  }

  @Override
  public boolean canInteract(Player player, Location location) {
    return !plugin.worldUtil().isEnabled(location.getWorld())
        || FactionsPlayerListener.canUseBlock(player, location.getBlock().getType(), location, true);
  }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    Location location = entity.getLocation();
    return !plugin.worldUtil().isEnabled(location.getWorld())
        || FactionsPlayerListener.canInteractHere(player, location)
        && (!(entity instanceof InventoryHolder)
            || FactionsPlayerListener.canUseBlock(player, Material.CHEST, location, true));
  }

  @Override
  public boolean canUse(Player player, Location location) {
    return !plugin.worldUtil().isEnabled(location.getWorld())
        || FactionsPlayerListener.canInteractHere(player, location);
  }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    return !plugin.worldUtil().isEnabled(entity.getWorld())
        || FactionsEntityListener.canDamage(player, entity, false);
  }
}
