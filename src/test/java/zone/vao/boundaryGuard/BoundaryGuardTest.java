package zone.vao.boundaryGuard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import zone.vao.boundaryGuard.enums.IntegrationType;

import java.util.logging.Logger;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoundaryGuardTest {
  private BoundaryGuard guard;
  private Player player;
  private Entity target;
  private World world;
  private Location location;

  @BeforeEach
  void setup() {
    guard = new BoundaryGuard();
    player = mock(Player.class);
    target = mock(Entity.class);
    world = mock(World.class);
    location = new Location(world, 10, 64, -20);
    when(target.getLocation()).thenReturn(location);
  }

  private Integration integration(IntegrationType type, boolean allow) {
    Integration integration = mock(Integration.class);
    when(integration.getType()).thenReturn(type);
    when(integration.canBuild(player, location)).thenReturn(allow);
    when(integration.canBreak(player, location)).thenReturn(allow);
    when(integration.canInteract(player, location)).thenReturn(allow);
    when(integration.canUse(player, location)).thenReturn(allow);
    when(integration.canDamage(player, target)).thenReturn(allow);
    return integration;
  }

  @Test
  void protectionChecksRemainValidAfterGarbageCollection() {
    System.gc();
    assertSame(world, location.getWorld());
    assertTrue(guard.canBuild(player, location));
    assertTrue(guard.checkBuild(player, location).allowed());
  }

  @Test
  void noPluginsAllowAllFiveActions() {
    assertTrue(guard.canBuild(player, location));
    assertTrue(guard.canBreak(player, location));
    assertTrue(guard.canInteract(player, location));
    assertTrue(guard.canUse(player, location));
    assertTrue(guard.canDamage(player, target));
  }

  @Test
  void anyIntegrationCanVetoAllFiveActions() {
    guard.registerIntegration(integration(IntegrationType.TOWNY, true));
    guard.registerIntegration(integration(IntegrationType.WORLDGUARD, false));
    assertFalse(guard.canBuild(player, location));
    assertFalse(guard.canBreak(player, location));
    assertFalse(guard.canInteract(player, location));
    assertFalse(guard.canUse(player, location));
    assertFalse(guard.canDamage(player, target));
  }

  @Test
  void damageIsDelegatedWithTheActualEntity() {
    Integration integration = integration(IntegrationType.CUSTOM, true);
    guard.registerIntegration(integration);
    assertTrue(guard.canDamage(player, target));
    verify(integration).canDamage(player, target);
    verify(player, never()).getLocation();
  }

  @Test
  void runtimeFailureDeniesByDefault() {
    Integration integration = integration(IntegrationType.TOWNY, true);
    when(integration.canDamage(player, target)).thenThrow(new IllegalStateException("API unavailable"));
    guard.registerIntegration(integration);
    assertFalse(guard.canDamage(player, target));
  }

  @Test
  void linkageFailureDeniesByDefault() {
    Integration integration = integration(IntegrationType.TOWNY, true);
    when(integration.canBreak(player, location)).thenThrow(new NoClassDefFoundError("optional API"));
    guard.registerIntegration(integration);
    assertFalse(guard.canBreak(player, location));
  }

  @Test
  void failOpenStillEvaluatesOtherIntegrations() {
    guard.setFailOpen(true);
    Integration broken = integration(IntegrationType.TOWNY, true);
    when(broken.canBuild(player, location)).thenThrow(new IllegalStateException("broken"));
    guard.registerIntegration(broken);
    guard.registerIntegration(integration(IntegrationType.WORLDGUARD, false));
    assertFalse(guard.canBuild(player, location));
  }

  @Test
  void failOpenAllowsWhenOnlyIntegrationFails() {
    guard.setFailOpen(true);
    Integration broken = integration(IntegrationType.CUSTOM, true);
    when(broken.canUse(player, location)).thenThrow(new NoSuchMethodError("version mismatch"));
    guard.registerIntegration(broken);
    assertTrue(guard.canUse(player, location));
  }

  @Test
  void registrationReplacesTheSameTypeAndExposesImmutableSnapshot() {
    guard.registerIntegration(integration(IntegrationType.TOWNY, false));
    var snapshot = guard.getIntegrations();
    guard.registerIntegration(integration(IntegrationType.TOWNY, true));
    assertEquals(1, guard.getIntegrations().size());
    assertNotSame(snapshot.get(IntegrationType.TOWNY), guard.getIntegrations().get(IntegrationType.TOWNY));
    assertThrows(UnsupportedOperationException.class, snapshot::clear);
    assertTrue(guard.canBuild(player, location));
  }

  @Test
  void invalidArgumentsAreRejectedEvenWithoutPlugins() {
    assertThrows(NullPointerException.class, () -> guard.canBuild(null, location));
    assertThrows(NullPointerException.class, () -> guard.canDamage(player, null));
    assertThrows(IllegalArgumentException.class, () -> guard.canUse(player, new Location(null, 0, 0, 0)));
  }

  @Test
  void absentOptionalApisAreNotLoadedAndInitCanBeRepeated() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      PluginManager manager = mock(PluginManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
      JavaPlugin owner = mock(JavaPlugin.class);
      guard.init(owner);
      guard.init(owner);
      assertTrue(guard.getIntegrations().isEmpty());
      assertTrue(guard.getFailedIntegrations().isEmpty());
      assertTrue(guard.canDamage(player, target));
      guard.close();
      verify(manager, times(2)).registerEvents(any(Listener.class), same(owner));
    }
  }

  @Test
  void enabledIncompatiblePluginIsReportedAndDenied() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      PluginManager manager = mock(PluginManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
      JavaPlugin owner = mock(JavaPlugin.class);
      when(owner.getLogger()).thenReturn(Logger.getAnonymousLogger());
      Plugin incompatible = mock(Plugin.class);
      when(incompatible.isEnabled()).thenReturn(true);
      when(manager.getPlugin("Residence")).thenReturn(incompatible);
      guard.init(owner);
      assertTrue(guard.getFailedIntegrations().contains(IntegrationType.RESIDENCE));
      assertFalse(guard.canDamage(player, target));
      guard.setFailOpen(true);
      assertTrue(guard.canDamage(player, target));
      guard.close();
    }
  }

  @Test
  void pluginEnableAndDisableRefreshIntegration() throws Exception {
    try (var bukkit = mockStatic(Bukkit.class)) {
      PluginManager manager = mock(PluginManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
      JavaPlugin owner = mock(JavaPlugin.class);
      guard.init(owner);
      ArgumentCaptor<Listener> captor = ArgumentCaptor.forClass(Listener.class);
      verify(manager).registerEvents(captor.capture(), same(owner));
      Listener listener = captor.getValue();
      Plugin towny = mock(Plugin.class);
      when(towny.getName()).thenReturn("Towny");
      when(towny.isEnabled()).thenReturn(true);
      when(manager.getPlugin("Towny")).thenReturn(towny);
      var enable = listener.getClass().getMethod("onEnable", PluginEnableEvent.class);
      enable.invoke(listener, new PluginEnableEvent(towny));
      assertTrue(guard.getIntegrations().containsKey(IntegrationType.TOWNY));
      var disable = listener.getClass().getMethod("onDisable", PluginDisableEvent.class);
      disable.invoke(listener, new PluginDisableEvent(towny));
      assertTrue(guard.getIntegrations().isEmpty());
      guard.close();
    }
  }

  @Test
  void detailedChecksReportEveryDenialAndOriginalError() {
    Integration first = integration(IntegrationType.CUSTOM, false);
    Integration second = integration(IntegrationType.CUSTOM, true);
    var cause = new NoSuchMethodError("missing API");
    when(second.canBuild(player, location)).thenThrow(cause);
    guard.registerIntegration("first", first);
    guard.registerIntegration("second", second);
    ProtectionResult result = guard.checkBuild(player, location);
    assertFalse(result.allowed());
    assertEquals(ProtectionResult.Action.BUILD, result.action());
    assertEquals(List.of("first", "second"), result.issues().stream()
        .map(ProtectionResult.Issue::integrationId).toList());
    assertEquals(ProtectionResult.Reason.DENIED, result.issues().get(0).reason());
    assertNull(result.issues().get(0).error());
    assertEquals(ProtectionResult.Reason.CHECK_ERROR, result.issues().get(1).reason());
    assertSame(cause, result.issues().get(1).error());
    assertThrows(UnsupportedOperationException.class, () -> result.issues().clear());
  }

  @Test
  void allowedFailOpenResultStillContainsError() {
    guard.setFailOpen(true);
    Integration broken = integration(IntegrationType.CUSTOM, true);
    when(broken.canDamage(player, target)).thenThrow(new IllegalStateException("unavailable"));
    guard.registerIntegration("broken", broken);
    ProtectionResult result = guard.checkDamage(player, target);
    assertTrue(result.allowed());
    assertEquals(ProtectionResult.Reason.CHECK_ERROR, result.issues().getFirst().reason());
  }

  @Test
  void booleanChecksStopAtTheFirstDenial() {
    Integration first = integration(IntegrationType.CUSTOM, false);
    Integration second = integration(IntegrationType.CUSTOM, true);
    guard.registerIntegration("first", first);
    guard.registerIntegration("second", second);
    assertFalse(guard.canBuild(player, location));
    verify(second, never()).canBuild(player, location);
  }

  @Test
  void namedRegistrationsAreIndependentAndCanBeRemoved() {
    Integration allow = integration(IntegrationType.CUSTOM, true);
    Integration deny = integration(IntegrationType.CUSTOM, false);
    guard.registerIntegration("allow", allow);
    guard.registerIntegration("deny", deny);
    assertEquals(2, guard.getRegisteredIntegrations().size());
    assertFalse(guard.canUse(player, location));
    var snapshot = guard.getRegisteredIntegrations();
    assertTrue(guard.unregisterIntegration("deny"));
    assertFalse(guard.unregisterIntegration("missing"));
    assertTrue(guard.canUse(player, location));
    assertEquals(2, snapshot.size());
    assertThrows(UnsupportedOperationException.class, snapshot::clear);
    guard.registerIntegration("allow", deny);
    assertEquals(1, guard.getRegisteredIntegrations().size());
    assertFalse(guard.canUse(player, location));
  }

  @Test
  void invalidIdsAndReservedTypeMismatchesAreRejected() {
    Integration custom = integration(IntegrationType.CUSTOM, true);
    assertThrows(IllegalArgumentException.class, () -> guard.registerIntegration(" ", custom));
    assertThrows(NullPointerException.class, () -> guard.unregisterIntegration(null));
    assertThrows(IllegalArgumentException.class, () -> guard.registerIntegration("towny", custom));
  }

  @Test
  void placementForwardsMaterialAndEntityInteractionUsesItsOwnOverload() {
    Integration integration = integration(IntegrationType.CUSTOM, true);
    when(integration.canPlace(player, location, Material.DIAMOND_BLOCK)).thenReturn(true);
    when(integration.canInteract(player, target)).thenReturn(true);
    guard.registerIntegration("test", integration);
    assertTrue(guard.canPlace(player, location, Material.DIAMOND_BLOCK));
    assertTrue(guard.canInteract(player, target));
    assertEquals(ProtectionResult.Action.PLACE, guard.checkPlace(player, location, Material.DIAMOND_BLOCK).action());
    assertEquals(ProtectionResult.Action.INTERACT_ENTITY, guard.checkInteract(player, target).action());
    verify(integration, times(2)).canPlace(player, location, Material.DIAMOND_BLOCK);
    verify(integration, times(2)).canInteract(player, target);
    verify(integration, never()).canBuild(player, location);
    verify(integration, never()).canInteract(player, location);
  }

  @Test
  void placementRejectsNonBlocksAirAndNullMaterial() {
    assertThrows(IllegalArgumentException.class, () -> guard.canPlace(player, location, Material.STICK));
    assertThrows(IllegalArgumentException.class, () -> guard.checkPlace(player, location, Material.AIR));
    assertThrows(NullPointerException.class, () -> guard.canPlace(player, location, null));
  }

  @Test
  void detailedChecksExposeTheCorrectAction() {
    assertEquals(ProtectionResult.Action.BREAK, guard.checkBreak(player, location).action());
    assertEquals(ProtectionResult.Action.INTERACT_BLOCK, guard.checkInteract(player, location).action());
    assertEquals(ProtectionResult.Action.USE, guard.checkUse(player, location).action());
    assertTrue(guard.checkUse(player, location).issues().isEmpty());
  }

  @Test
  void selectionIsCopiedAndAppliesToLaterEnableEvents() throws Exception {
    try (var bukkit = mockStatic(Bukkit.class)) {
      PluginManager manager = mock(PluginManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
      JavaPlugin owner = mock(JavaPlugin.class);
      var selection = EnumSet.of(IntegrationType.TOWNY);
      guard.init(owner, selection);
      selection.clear();
      assertEquals(Set.of(IntegrationType.TOWNY), guard.getEnabledIntegrations());
      verify(manager, never()).getPlugin("Residence");
      ArgumentCaptor<Listener> captor = ArgumentCaptor.forClass(Listener.class);
      verify(manager).registerEvents(captor.capture(), same(owner));
      Listener listener = captor.getValue();
      var enable = listener.getClass().getMethod("onEnable", PluginEnableEvent.class);
      Plugin excluded = mock(Plugin.class);
      when(excluded.getName()).thenReturn("Residence");
      enable.invoke(listener, new PluginEnableEvent(excluded));
      verify(manager, never()).getPlugin("Residence");
      Plugin towny = mock(Plugin.class);
      when(towny.getName()).thenReturn("Towny");
      when(towny.isEnabled()).thenReturn(true);
      when(manager.getPlugin("Towny")).thenReturn(towny);
      enable.invoke(listener, new PluginEnableEvent(towny));
      assertTrue(guard.getRegisteredIntegrations().containsKey("towny"));
      guard.close();
    }
  }

  @Test
  void initializationErrorAppearsInDiagnosticsAndCanBeRemoved() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      PluginManager manager = mock(PluginManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
      JavaPlugin owner = mock(JavaPlugin.class);
      when(owner.getLogger()).thenReturn(Logger.getAnonymousLogger());
      Plugin incompatible = mock(Plugin.class);
      when(incompatible.isEnabled()).thenReturn(true);
      when(manager.getPlugin("Residence")).thenReturn(incompatible);
      guard.init(owner, Set.of(IntegrationType.RESIDENCE));
      var result = guard.checkBuild(player, location);
      assertFalse(result.allowed());
      var issue = result.issues().getFirst();
      assertEquals("residence", issue.integrationId());
      assertEquals(IntegrationType.RESIDENCE, issue.integrationType());
      assertEquals(ProtectionResult.Reason.INITIALIZATION_ERROR, issue.reason());
      assertNotNull(issue.error());
      guard.setFailOpen(true);
      assertTrue(guard.checkBuild(player, location).allowed());
      assertTrue(guard.unregisterIntegration("residence"));
      assertTrue(guard.getFailedIntegrations().isEmpty());
      guard.setFailOpen(false);
      assertTrue(guard.canBuild(player, location));
      guard.close();
    }
  }

  @Test
  void emptySelectionDisablesDetectionAndReinitializationClearsCustomRegistrations() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      PluginManager manager = mock(PluginManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
      JavaPlugin owner = mock(JavaPlugin.class);
      guard.init(owner, Set.of());
      verify(manager, never()).getPlugin(anyString());
      guard.registerIntegration("custom-one", integration(IntegrationType.CUSTOM, false));
      guard.init(owner, Set.of());
      assertTrue(guard.getRegisteredIntegrations().isEmpty());
      assertTrue(guard.canBuild(player, location));
      guard.close();
    }
  }

  @Test
  void invalidSelectionDoesNotDiscardExistingRegistrations() {
    guard.registerIntegration("keep", integration(IntegrationType.CUSTOM, true));
    assertThrows(IllegalArgumentException.class,
        () -> guard.init(mock(JavaPlugin.class), Set.of(IntegrationType.CUSTOM)));
    assertTrue(guard.getRegisteredIntegrations().containsKey("keep"));
  }
}
