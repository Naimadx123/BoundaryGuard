package zone.vao.boundaryGuard.integrations;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zone.vao.boundaryGuard.BoundaryGuard;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;


@Getter
public class TownyInteg extends BoundaryGuard implements Integration{

  IntegrationType type = IntegrationType.TOWNY;

  public TownyInteg(Location location) {
    super(location);
  }

  @Override
  public boolean canBuild(Player player) {
    return PlayerCacheUtil
        .getCachePermission(player, getLocation(), getLocation().getBlock().getType(), TownyPermission.ActionType.BUILD);
  }

  @Override
  public boolean canBreak(Player player) {
    return PlayerCacheUtil
        .getCachePermission(player, getLocation(), getLocation().getBlock().getType(), TownyPermission.ActionType.DESTROY);
  }

  @Override
  public boolean canInteract(Player player) {
    return PlayerCacheUtil
        .getCachePermission(player, getLocation(), getLocation().getBlock().getType(), TownyPermission.ActionType.SWITCH);
  }

  @Override
  public boolean canUse(Player player) {
    return PlayerCacheUtil
        .getCachePermission(player, getLocation(), getLocation().getBlock().getType(), TownyPermission.ActionType.ITEM_USE);
  }
}
