package zone.vao.boundaryGuard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntegrationTest {
  @Test
  void newDefaultsKeepExistingCustomIntegrationsCompatible() {
    Integration integration = mock(Integration.class, CALLS_REAL_METHODS);
    Player player = mock(Player.class);
    Entity target = mock(Entity.class);
    Location location = mock(Location.class);
    when(target.getLocation()).thenReturn(location);
    when(integration.canBuild(player, location)).thenReturn(true);
    when(integration.canInteract(player, location)).thenReturn(true);
    assertTrue(integration.canPlace(player, location, Material.STONE));
    assertTrue(integration.canInteract(player, target));
  }
}
