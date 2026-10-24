package zone.vao.boundaryGuard.integrations;

import com.plotsquared.core.plot.Plot;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class PlotSquaredIntg implements Integration {
  @Override
  public IntegrationType getType() { return IntegrationType.PLOTSQUARED; }

  public PlotSquaredIntg() {}
  public PlotSquaredIntg(JavaPlugin owner, Plugin plugin) {}

  @Override
  public boolean canBuild(Player player, Location location) { return canModify(player, location); }

  @Override
  public boolean canBreak(Player player, Location location) { return canModify(player, location); }

  @Override
  public boolean canInteract(Player player, Location location) { return canModify(player, location); }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    return canModify(player, entity.getLocation());
  }

  @Override
  public boolean canUse(Player player, Location location) { return canModify(player, location); }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    Plot plot = getPlotFromLocation(entity.getLocation());
    return plot == null || !plot.isDenied(player.getUniqueId());
  }

  private boolean canModify(Player player, Location location) {
    var adapted = adaptBukkitLocation(location);
    if (adapted == null) return false;
    if (!adapted.isPlotArea()) return true;
    Plot plot = Plot.getPlot(adapted);
    if (plot == null) return player.hasPermission("plots.admin.build.road");
    if (!plot.hasOwner()) return player.hasPermission("plots.admin.build.unowned");
    return player.hasPermission("plots.admin.build.other")
        || (!plot.isDenied(player.getUniqueId()) && plot.isAdded(player.getUniqueId()));
  }

  private Plot getPlotFromLocation(Location location) {
    var adapted = adaptBukkitLocation(location);
    return adapted == null ? null : Plot.getPlot(adapted);
  }

  private com.plotsquared.core.location.Location adaptBukkitLocation(Location location) {
    if (location.getWorld() == null) return null;
    return com.plotsquared.core.location.Location.at(
        location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
  }
}
