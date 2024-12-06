package zone.vao.boundaryGuard.integrations;

import com.plotsquared.core.plot.Plot;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

@Getter
public class PlotSquaredIntg implements Integration {

  IntegrationType type = IntegrationType.PLOTSQUARED;


  @Override
  public boolean canBuild(Player player, Location location) {
    return checkPermission(player, location, true);
  }

  @Override
  public boolean canBreak(Player player, Location location) {
    return checkPermission(player, location, false);
  }

  @Override
  public boolean canInteract(Player player, Location location) {
    return checkPermission(player, location, true);
  }

  @Override
  public boolean canUse(Player player, Location location) {
    return checkPermission(player, location, true);
  }

  private boolean checkPermission(Player player, Location location, boolean allow) {
    Plot plot = getPlotFromLocation(location);
    return plot == null || (allow ? plot.isAdded(player.getUniqueId()) : !plot.isDenied(player.getUniqueId()));
  }

  private Plot getPlotFromLocation(Location location) {
    com.plotsquared.core.location.Location plotLoc = adaptBukkitLocation(location);
    if (plotLoc == null) return null;
    return Plot.getPlot(plotLoc);
  }

  private com.plotsquared.core.location.Location adaptBukkitLocation(Location location) {
    if (!location.isWorldLoaded()) return null;
    return com.plotsquared.core.location.Location.at(
        location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
  }
}
