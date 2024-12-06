package zone.vao.boundaryGuard;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomEntity;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

@Getter
public class BoundaryGuard {

  private final Location location;

  public BoundaryGuard(Location location) {
    this.location = location;

  }

  /**
   * Factory method to create a BoundaryGuard instance for a given location.
   *
   * @param location The location to guard.
   * @return A new BoundaryGuard instance.
   */
  public static BoundaryGuard create(Location location) {
    return new BoundaryGuard(location);
  }


  public static boolean isNexo(Location location) {

    return NexoBlocks.isCustomBlock(location.getBlock()) ||
        NexoFurniture.isFurniture(location);
  }

  public static boolean isOraxen(Location location) {

    return OraxenBlocks.isOraxenBlock(location.getBlock()) ||
        OraxenFurniture.isFurniture(location.getBlock());
  }

  public static boolean isItemsAdder(Location location) {
    if (CustomBlock.byAlreadyPlaced(location.getBlock()) != null) {
      return true;
    }
    for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1)) {
      if (CustomEntity.byAlreadySpawned(entity) != null) {
        return true;
      }
    }
    return false;
  }

  public static boolean isItemsAdder(Entity entity) {

    return CustomEntity.byAlreadySpawned(entity) != null ||
        CustomBlock.byAlreadyPlaced(entity.getLocation().getBlock()) != null;
  }

  /**
   * Checks if the location contains a Custom block or furniture.
   *
   * @return CustomType if the location contains Custom content, null otherwise.
   */
  public static CustomType getType(Location location) {

    if(isNexo(location)) {
      return CustomType.NEXO;
    }
    if(isOraxen(location)) {
      return CustomType.ORAXEN;
    }
    if(isItemsAdder(location)) {
      return CustomType.ITEMS_ADDER;
    }
    return null;
  }
}
