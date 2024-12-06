package zone.vao.boundaryGuard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface Integration {
  boolean canBuild(Player player, Location location);
  boolean canBreak(Player player, Location location);
  boolean canInteract(Player player, Location location);
  boolean canUse(Player player, Location location);
}
