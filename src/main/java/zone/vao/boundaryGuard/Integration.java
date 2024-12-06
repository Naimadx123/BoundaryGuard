package zone.vao.boundaryGuard;

import org.bukkit.entity.Player;

public interface Integration {

  /**
   * Determines if the player can build at this location.
   *
   * @param player The player attempting to build.
   * @return True if the player can build, false otherwise.
   */
  public boolean canBuild(Player player);

  /**
   * Determines if the player can break a block at this location.
   *
   * @param player The player attempting to break a block.
   * @return True if the player can break, false otherwise.
   */
  public boolean canBreak(Player player);

  /**
   * Determines if the player can interact with objects at this location.
   *
   * @param player The player attempting to interact.
   * @return True if interaction is allowed, false otherwise.
   */
  public boolean canInteract(Player player);


  /**
   * Determines if the player can use an item or block at this location.
   *
   * @param player The player attempting to use an item/block.
   * @return True if usage is allowed, false otherwise.
   */
  public boolean canUse(Player player);

}
