package zone.vao.boundaryGuard.integrations;

import net.thenextlvl.protect.service.ProtectionService;
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
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import zone.vao.boundaryGuard.BoundaryGuard;
import zone.vao.boundaryGuard.ProtectionResult;
import zone.vao.boundaryGuard.enums.IntegrationType;

import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProtectIntgTest {
  private final World world = mock(World.class);
  private final Player player = mock(Player.class);
  private final Entity target = mock(Entity.class);
  private final Location location = new Location(world, 10, 64, 20);
  private ProtectionService service;

  @BeforeEach
  void setup() {
    service = mock(ProtectionService.class);
    when(target.getLocation()).thenReturn(location);
  }

  private ProtectIntg integration() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      ServicesManager services = mock(ServicesManager.class);
      bukkit.when(Bukkit::getServicesManager).thenReturn(services);
      when(services.load(ProtectionService.class)).thenReturn(service);
      return new ProtectIntg(null, mock(Plugin.class));
    }
  }

  @Test
  void usesRegisteredServiceAndReportsItsType() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      ServicesManager services = mock(ServicesManager.class);
      bukkit.when(Bukkit::getServicesManager).thenReturn(services);
      when(services.load(ProtectionService.class)).thenReturn(service);
      assertEquals(IntegrationType.PROTECT, new ProtectIntg(null, mock(Plugin.class)).getType());
      verify(services).load(ProtectionService.class);
    }
  }

  @Test
  void placementAndBreakingRespectTheirOwnServiceDecisions() {
    ProtectIntg integration = integration();
    when(service.canPlace(player, location)).thenReturn(true);
    assertTrue(integration.canBuild(player, location));
    assertTrue(integration.canPlace(player, location, Material.STONE));
    assertFalse(integration.canBreak(player, location));
    when(service.canPlace(player, location)).thenReturn(false);
    when(service.canDestroy(player, location)).thenReturn(true);
    assertFalse(integration.canBuild(player, location));
    assertTrue(integration.canBreak(player, location));
  }

  @Test
  void blockInteractionAndItemUseDelegateToBlockInteraction() {
    ProtectIntg integration = integration();
    assertFalse(integration.canInteract(player, location));
    assertFalse(integration.canUse(player, location));
    when(service.canInteract(player, location)).thenReturn(true);
    assertTrue(integration.canInteract(player, location));
    assertTrue(integration.canUse(player, location));
    verify(service, times(4)).canInteract(player, location);
  }

  @Test
  void entityInteractionUsesTheEntityOverload() {
    ProtectIntg integration = integration();
    assertFalse(integration.canInteract(player, target));
    when(service.canInteract(player, target)).thenReturn(true);
    assertTrue(integration.canInteract(player, target));
    verify(service, never()).canInteract(any(), any(Location.class));
  }

  @Test
  void damagePassesBothEntitiesToAttackService() {
    ProtectIntg integration = integration();
    when(player.isOp()).thenReturn(true);
    assertFalse(integration.canDamage(player, target));
    when(service.canAttack(player, target)).thenReturn(true);
    assertTrue(integration.canDamage(player, target));
    verify(service, times(2)).canAttack(player, target);
    verify(player, never()).getLocation();
    verify(player, never()).isOp();
  }

  @Test
  void missingServiceIsAnInitializationFailure() {
    try (var bukkit = mockStatic(Bukkit.class)) {
      ServicesManager services = mock(ServicesManager.class);
      bukkit.when(Bukkit::getServicesManager).thenReturn(services);
      IllegalStateException failure = assertThrows(IllegalStateException.class,
          () -> new ProtectIntg(null, mock(Plugin.class)));
      assertEquals("Protect ProtectionService is not registered", failure.getMessage());
    }
  }

  @Test
  void serviceExceptionsReachBoundaryGuardDiagnostics() {
    ProtectIntg integration = integration();
    var cause = new IllegalStateException("Protect unavailable");
    when(service.canAttack(player, target)).thenThrow(cause);
    try (BoundaryGuard guard = new BoundaryGuard()) {
      guard.registerIntegration(integration);
      var result = guard.checkDamage(player, target);
      assertFalse(result.allowed());
      var issue = result.issues().getFirst();
      assertEquals("protect", issue.integrationId());
      assertEquals(ProtectionResult.Reason.CHECK_ERROR, issue.reason());
      assertSame(cause, issue.error());
      guard.setFailOpen(true);
      assertTrue(guard.checkDamage(player, target).allowed());
    }
  }

  @Test
  void automaticDetectionAndPluginLifecycleUseProtectRegistration() throws Exception {
    try (var bukkit = mockStatic(Bukkit.class);
         BoundaryGuard guard = new BoundaryGuard()) {
      PluginManager plugins = mock(PluginManager.class);
      ServicesManager services = mock(ServicesManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
      bukkit.when(Bukkit::getServicesManager).thenReturn(services);
      Plugin plugin = mock(Plugin.class);
      when(plugin.getName()).thenReturn("Protect");
      when(plugin.isEnabled()).thenReturn(true);
      when(plugins.getPlugin("Protect")).thenReturn(plugin);
      when(services.load(ProtectionService.class)).thenReturn(service);
      guard.init(mock(JavaPlugin.class), Set.of(IntegrationType.PROTECT));
      assertInstanceOf(ProtectIntg.class, guard.getRegisteredIntegrations().get("protect"));
      assertFalse(guard.canDamage(player, target));
      when(service.canAttack(player, target)).thenReturn(true);
      assertTrue(guard.canDamage(player, target));
      ArgumentCaptor<Listener> captor = ArgumentCaptor.forClass(Listener.class);
      verify(plugins).registerEvents(captor.capture(), any(JavaPlugin.class));
      Listener listener = captor.getValue();
      var disable = listener.getClass().getMethod("onDisable", PluginDisableEvent.class);
      disable.setAccessible(true);
      disable.invoke(listener, new PluginDisableEvent(plugin));
      assertFalse(guard.getRegisteredIntegrations().containsKey("protect"));
      var enable = listener.getClass().getMethod("onEnable", PluginEnableEvent.class);
      enable.setAccessible(true);
      enable.invoke(listener, new PluginEnableEvent(plugin));
      assertInstanceOf(ProtectIntg.class, guard.getRegisteredIntegrations().get("protect"));
    }
  }

  @Test
  void enabledPluginWithoutServiceFailsClosedAndCanBeRetried() {
    try (var bukkit = mockStatic(Bukkit.class);
         BoundaryGuard guard = new BoundaryGuard()) {
      PluginManager plugins = mock(PluginManager.class);
      ServicesManager services = mock(ServicesManager.class);
      bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
      bukkit.when(Bukkit::getServicesManager).thenReturn(services);
      Plugin plugin = mock(Plugin.class);
      when(plugin.isEnabled()).thenReturn(true);
      when(plugins.getPlugin("Protect")).thenReturn(plugin);
      JavaPlugin owner = mock(JavaPlugin.class);
      when(owner.getLogger()).thenReturn(Logger.getAnonymousLogger());
      guard.init(owner, Set.of(IntegrationType.PROTECT));
      assertEquals(Set.of(IntegrationType.PROTECT), guard.getFailedIntegrations());
      var result = guard.checkBuild(player, location);
      assertFalse(result.allowed());
      assertEquals(ProtectionResult.Reason.INITIALIZATION_ERROR, result.issues().getFirst().reason());
      when(services.load(ProtectionService.class)).thenReturn(service);
      when(service.canPlace(player, location)).thenReturn(true);
      guard.init(owner, Set.of(IntegrationType.PROTECT));
      assertTrue(guard.getFailedIntegrations().isEmpty());
      assertTrue(guard.canBuild(player, location));
    }
  }
}
