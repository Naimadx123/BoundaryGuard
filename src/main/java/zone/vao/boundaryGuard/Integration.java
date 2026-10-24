package zone.vao.boundaryGuard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import zone.vao.boundaryGuard.enums.IntegrationType;

/** Checks must run on the server thread. A false result vetoes the action. */
public interface Integration {
  default IntegrationType getType() { return IntegrationType.CUSTOM; }

  /** Whether the player may place a block at the target location. */
  boolean canBuild(Player player, Location location);

  /** Custom integrations may override this to inspect the placed material. */
  default boolean canPlace(Player player, Location location, Material material) {
    return canBuild(player, location);
  }

  /** Whether the player may break the target block. */
  boolean canBreak(Player player, Location location);

  /** Whether the player may interact with the target block. */
  boolean canInteract(Player player, Location location);

  /** Custom integrations may override this; the default checks interaction at the entity location. */
  default boolean canInteract(Player player, Entity entity) {
    return canInteract(player, entity.getLocation());
  }

  /** Whether the player may use an item at the target location. */
  boolean canUse(Player player, Location location);

  /** Whether the player may damage the entity. Checks its location, not the attacker's. */
  boolean canDamage(Player player, Entity entity);
}
