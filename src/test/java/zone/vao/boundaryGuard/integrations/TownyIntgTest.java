package zone.vao.boundaryGuard.integrations;

import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.object.TownyPermission;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TownyIntgTest {
  private final World world = mock(World.class);

  @Test
  void placementChecksProposedMaterialInsteadOfExistingBlock() {
    Player player = mock(Player.class);
    Location location = new Location(world, 1, 64, 2);
    try (var cache = mockStatic(PlayerCacheUtil.class)) {
      cache.when(() -> PlayerCacheUtil.getCachePermission(player, location, Material.DIAMOND_BLOCK,
          TownyPermission.ActionType.BUILD)).thenReturn(true);
      assertTrue(new TownyIntg().canPlace(player, location, Material.DIAMOND_BLOCK));
      cache.verify(() -> PlayerCacheUtil.getCachePermission(player, location, Material.DIAMOND_BLOCK,
          TownyPermission.ActionType.BUILD));
    }
  }

  @Test
  void invertsTownysPreventDamageResult() {
    Player player = mock(Player.class);
    Entity target = mock(Entity.class);
    try (var combat = mockStatic(CombatUtil.class)) {
      TownyIntg integration = new TownyIntg();
      combat.when(() -> CombatUtil.preventDamageCall(player, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK))
          .thenReturn(true);
      assertFalse(integration.canDamage(player, target));
      combat.when(() -> CombatUtil.preventDamageCall(player, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK))
          .thenReturn(false);
      assertTrue(integration.canDamage(player, target));
    }
  }
}
