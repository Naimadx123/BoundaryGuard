package zone.vao.boundaryGuard.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Boat;
import org.bukkit.entity.minecart.StorageMinecart;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldGuardIntgTest {
  static Stream<Arguments> entityInteractions() {
    return Stream.of(
        Arguments.of(ItemFrame.class, Flags.INTERACT),
        Arguments.of(Boat.class, Flags.RIDE),
        Arguments.of(StorageMinecart.class, Flags.CHEST_ACCESS));
  }

  @ParameterizedTest
  @MethodSource("entityInteractions")
  void entityInteractionSelectsFlagAndChecksTargetWorld(Class<? extends Entity> type, StateFlag flag) {
    World targetWorld = mock(World.class);
    Location targetLocation = new Location(targetWorld, 4, 70, 8);
    Player player = mock(Player.class);
    Entity entity = mock(type);
    when(entity.getLocation()).thenReturn(targetLocation);
    WorldGuard wg = mock(WorldGuard.class, RETURNS_DEEP_STUBS);
    WorldGuardPlugin plugin = mock(WorldGuardPlugin.class);
    LocalPlayer local = mock(LocalPlayer.class);
    when(plugin.wrapPlayer(player)).thenReturn(local);
    var world = mock(com.sk89q.worldedit.world.World.class);
    var location = mock(com.sk89q.worldedit.util.Location.class);
    RegionQuery query = mock(RegionQuery.class);
    when(wg.getPlatform().getRegionContainer().createQuery()).thenReturn(query);
    try (var worldGuard = mockStatic(WorldGuard.class);
         var plugins = mockStatic(WorldGuardPlugin.class);
         var adapter = mockStatic(BukkitAdapter.class)) {
      worldGuard.when(WorldGuard::getInstance).thenReturn(wg);
      plugins.when(WorldGuardPlugin::inst).thenReturn(plugin);
      adapter.when(() -> BukkitAdapter.adapt(targetWorld)).thenReturn(world);
      adapter.when(() -> BukkitAdapter.adapt(targetLocation)).thenReturn(location);
      WorldGuardIntg integration = new WorldGuardIntg();
      assertFalse(integration.canInteract(player, entity));
      verify(query).testBuild(location, local, flag);
      when(query.testBuild(location, local, flag)).thenReturn(true);
      assertTrue(integration.canInteract(player, entity));
      verify(player, never()).getLocation();
      verify(player, never()).getWorld();
    }
  }

  @Test
  void damageChecksPvpAndAnimalsAtTargetAndUsesTargetWorldForBypass() {
    World targetWorld = mock(World.class);
    Location targetLocation = new Location(targetWorld, 5, 60, 7);
    Player player = mock(Player.class);
    Player victim = mock(Player.class);
    Animals animal = mock(Animals.class);
    when(victim.getLocation()).thenReturn(targetLocation);
    when(animal.getLocation()).thenReturn(targetLocation);
    WorldGuard wg = mock(WorldGuard.class, RETURNS_DEEP_STUBS);
    WorldGuardPlugin plugin = mock(WorldGuardPlugin.class);
    LocalPlayer local = mock(LocalPlayer.class);
    when(plugin.wrapPlayer(player)).thenReturn(local);
    var world = mock(com.sk89q.worldedit.world.World.class);
    var location = mock(com.sk89q.worldedit.util.Location.class);
    RegionQuery query = mock(RegionQuery.class);
    when(wg.getPlatform().getRegionContainer().createQuery()).thenReturn(query);
    try (var worldGuard = mockStatic(WorldGuard.class);
         var plugins = mockStatic(WorldGuardPlugin.class);
         var adapter = mockStatic(BukkitAdapter.class)) {
      worldGuard.when(WorldGuard::getInstance).thenReturn(wg);
      plugins.when(WorldGuardPlugin::inst).thenReturn(plugin);
      adapter.when(() -> BukkitAdapter.adapt(targetWorld)).thenReturn(world);
      adapter.when(() -> BukkitAdapter.adapt(targetLocation)).thenReturn(location);
      WorldGuardIntg integration = new WorldGuardIntg();
      assertFalse(integration.canDamage(player, victim));
      verify(query).testState(location, local, Flags.PVP);
      assertFalse(integration.canDamage(player, animal));
      verify(query).testState(location, local, Flags.DAMAGE_ANIMALS);
      when(wg.getPlatform().getSessionManager().hasBypass(local, world)).thenReturn(true);
      assertTrue(integration.canDamage(player, victim));
      verify(player, never()).getWorld();
    }
  }

  @Test
  void damageToOtherEntityTypesIsAllowed() {
    assertTrue(new WorldGuardIntg().canDamage(mock(Player.class), mock(Monster.class)));
  }
}
