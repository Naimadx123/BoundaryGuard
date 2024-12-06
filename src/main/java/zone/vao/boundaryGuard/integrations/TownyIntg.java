package zone.vao.boundaryGuard.integrations;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

@Getter
public class TownyIntg implements Integration {

  IntegrationType type = IntegrationType.TOWNY;

  @Override
  public boolean canBuild(Player player, Location location) {
    return hasPermission(player, location, TownyPermission.ActionType.BUILD);
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

  private boolean hasPermission(Player player, Location location, TownyPermission.ActionType actionType) {
    return PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), actionType);
  }
}
