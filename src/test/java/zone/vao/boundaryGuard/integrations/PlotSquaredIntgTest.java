package zone.vao.boundaryGuard.integrations;

import com.plotsquared.core.plot.Plot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlotSquaredIntgTest {
  @Test
  void visitorCannotBreakButAddedPlayerCan() {
    World world = mock(World.class);
    when(world.getName()).thenReturn("plots");
    Location location = new Location(world, 10, 64, 20);
    Player player = mock(Player.class);
    UUID uuid = UUID.randomUUID();
    when(player.getUniqueId()).thenReturn(uuid);
    Plot plot = mock(Plot.class);
    when(plot.hasOwner()).thenReturn(true);
    var adapted = mock(com.plotsquared.core.location.Location.class);
    when(adapted.isPlotArea()).thenReturn(true);
    try (var locations = mockStatic(com.plotsquared.core.location.Location.class);
         var plots = mockStatic(Plot.class)) {
      locations.when(() -> com.plotsquared.core.location.Location.at("plots", 10, 64, 20)).thenReturn(adapted);
      plots.when(() -> Plot.getPlot(adapted)).thenReturn(plot);
      PlotSquaredIntg integration = new PlotSquaredIntg();
      assertFalse(integration.canBuild(player, location));
      assertFalse(integration.canBreak(player, location));
      when(plot.isAdded(uuid)).thenReturn(true);
      assertTrue(integration.canBuild(player, location));
      assertTrue(integration.canBreak(player, location));
      when(plot.isDenied(uuid)).thenReturn(true);
      assertFalse(integration.canBreak(player, location));
    }
  }

  @Test
  void roadsAndUnownedPlotsNeedTheirOwnPermission() {
    World world = mock(World.class);
    when(world.getName()).thenReturn("plots");
    Location location = new Location(world, 0, 64, 0);
    Player player = mock(Player.class);
    Plot plot = mock(Plot.class);
    var adapted = mock(com.plotsquared.core.location.Location.class);
    when(adapted.isPlotArea()).thenReturn(true);
    try (var locations = mockStatic(com.plotsquared.core.location.Location.class);
         var plots = mockStatic(Plot.class)) {
      locations.when(() -> com.plotsquared.core.location.Location.at("plots", 0, 64, 0)).thenReturn(adapted);
      PlotSquaredIntg integration = new PlotSquaredIntg();
      assertFalse(integration.canBreak(player, location));
      when(player.hasPermission("plots.admin.build.road")).thenReturn(true);
      assertTrue(integration.canBreak(player, location));
      plots.when(() -> Plot.getPlot(adapted)).thenReturn(plot);
      assertFalse(integration.canBreak(player, location));
      when(player.hasPermission("plots.admin.build.unowned")).thenReturn(true);
      assertTrue(integration.canBreak(player, location));
    }
  }

  @Test
  void damageUsesTargetPlotAndDeniedList() {
    World world = mock(World.class);
    when(world.getName()).thenReturn("target");
    Location location = new Location(world, 1, 2, 3);
    Player player = mock(Player.class);
    UUID uuid = UUID.randomUUID();
    when(player.getUniqueId()).thenReturn(uuid);
    Entity entity = mock(Entity.class);
    when(entity.getLocation()).thenReturn(location);
    Plot plot = mock(Plot.class);
    var adapted = mock(com.plotsquared.core.location.Location.class);
    try (var locations = mockStatic(com.plotsquared.core.location.Location.class);
         var plots = mockStatic(Plot.class)) {
      locations.when(() -> com.plotsquared.core.location.Location.at("target", 1, 2, 3)).thenReturn(adapted);
      plots.when(() -> Plot.getPlot(adapted)).thenReturn(plot);
      PlotSquaredIntg integration = new PlotSquaredIntg();
      assertTrue(integration.canDamage(player, entity));
      when(plot.isDenied(uuid)).thenReturn(true);
      assertFalse(integration.canDamage(player, entity));
      verify(player, never()).getLocation();
    }
  }
}
