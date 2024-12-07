package zone.vao.boundaryGuard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.integrations.*;

import java.util.ArrayList;
import java.util.List;

public class BoundaryGuard {

  private final List<Integration> integrations = new ArrayList<>();
  private static boolean debug = false;

  public void init(JavaPlugin plugin) {
    loadIntegration("Towny", plugin, TownyIntg::new);
    loadIntegration("WorldGuard", plugin, WorldGuardIntg::new);
    loadIntegration("PlotSquared", plugin, PlotSquaredIntg::new);
  }

  public boolean canBuild(Player player, Location location) {
    return check(player, location, Integration::canBuild);
  }

  public boolean canBreak(Player player, Location location) {
    return check(player, location, Integration::canBreak);
  }

  public boolean canInteract(Player player, Location location) {
    return check(player, location, Integration::canInteract);
  }

  public boolean canUse(Player player, Location location) {
    return check(player, location, Integration::canUse);
  }

  public void setDebug(boolean debugMode) {
    debug = debugMode;
  }

  private boolean check(Player player, Location location, PermissionCheck check) {
    return integrations.stream().allMatch(integration -> check.test(integration, player, location));
  }

  private void loadIntegration(String pluginName, JavaPlugin mainPlugin, IntegrationFactory factory) {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
    if (plugin != null && plugin.isEnabled()) {
      try {
        integrations.add(factory.create());
      } catch (Exception e) {
        if (debug) e.printStackTrace();
      }
    }
  }

  @FunctionalInterface
  private interface PermissionCheck {
    boolean test(Integration integration, Player player, Location location);
  }

  @FunctionalInterface
  private interface IntegrationFactory {
    Integration create();
  }
}
