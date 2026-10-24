package zone.vao.boundaryGuard.integrations;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.minecart.StorageMinecart;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BentoBoxIntgTest {
  private final World interactionWorld = mock(World.class);

  static Stream<Arguments> entityInteractions() {
    return Stream.of(
        Arguments.of(ItemFrame.class, Flags.ITEM_FRAME),
        Arguments.of(Villager.class, Flags.TRADING),
        Arguments.of(ArmorStand.class, Flags.ARMOR_STAND),
        Arguments.of(Boat.class, Flags.BOAT),
        Arguments.of(StorageMinecart.class, Flags.CHEST));
  }

  @ParameterizedTest
  @MethodSource("entityInteractions")
  void entityInteractionUsesSpecificFlagAtTargetsLocation(Class<? extends Entity> type, Flag flag) {
    BentoBox plugin = mock(BentoBox.class, RETURNS_DEEP_STUBS);
    Player player = mock(Player.class);
    Entity entity = mock(type);
    Location target = new Location(interactionWorld, 1, 65, 3);
    when(entity.getLocation()).thenReturn(target);
    when(plugin.getIWM().inWorld(target)).thenReturn(true);
    Island island = mock(Island.class);
    User user = mock(User.class);
    when(plugin.getIslands().getIslandAt(target)).thenReturn(Optional.of(island));
    try (var users = mockStatic(User.class)) {
      users.when(() -> User.getInstance(player)).thenReturn(user);
      when(island.isAllowed(user, flag)).thenReturn(true);
      assertTrue(new BentoBoxIntg(null, plugin).canInteract(player, entity));
      verify(island).isAllowed(user, flag);
      verify(player, never()).getLocation();
    }
  }

  @ParameterizedTest
  @EnumSource(value = World.Environment.class, names = {"NORMAL", "NETHER", "THE_END"})
  void damageSelectsPvpFlagForTargetsDimension(World.Environment environment) {
    BentoBox plugin = mock(BentoBox.class, RETURNS_DEEP_STUBS);
    Player player = mock(Player.class);
    Player target = mock(Player.class);
    World world = mock(World.class);
    when(world.getEnvironment()).thenReturn(environment);
    Location location = new Location(world, 0, 64, 0);
    when(target.getLocation()).thenReturn(location);
    when(target.getWorld()).thenReturn(world);
    when(plugin.getIWM().inWorld(location)).thenReturn(true);
    Island island = mock(Island.class);
    User user = mock(User.class);
    when(plugin.getIslands().getIslandAt(location)).thenReturn(Optional.of(island));
    Flag selected = environment == World.Environment.NETHER ? Flags.PVP_NETHER
        : environment == World.Environment.THE_END ? Flags.PVP_END : Flags.PVP_OVERWORLD;
    try (var users = mockStatic(User.class)) {
      users.when(() -> User.getInstance(player)).thenReturn(user);
      when(island.isAllowed(user, selected)).thenReturn(true);
      assertTrue(new BentoBoxIntg(null, plugin).canDamage(player, target));
      verify(island).isAllowed(user, selected);
    }
  }
}
