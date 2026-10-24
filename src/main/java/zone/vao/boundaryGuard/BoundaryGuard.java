package zone.vao.boundaryGuard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.ProtectionResult.Action;
import zone.vao.boundaryGuard.ProtectionResult.Issue;
import zone.vao.boundaryGuard.ProtectionResult.Reason;
import zone.vao.boundaryGuard.enums.IntegrationType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

/**
 * Embed one instance in the owning plugin and initialize it during onEnable().
 * Call protection checks and registration methods on the server thread.
 */
public class BoundaryGuard implements AutoCloseable {
  private final Map<String, Registration> integrations = new LinkedHashMap<>();
  private final Map<IntegrationType, Throwable> failedIntegrations = new EnumMap<>(IntegrationType.class);
  private final Set<String> reportedFailures = new HashSet<>();
  private final Set<IntegrationType> enabledIntegrations = EnumSet.noneOf(IntegrationType.class);
  private JavaPlugin owner;
  private Listener listener;
  private boolean debug;
  private boolean failOpen;

  /** Detects all built-in integrations. Reinitialization clears existing registrations and listeners. */
  public void init(JavaPlugin plugin) {
    init(plugin, EnumSet.complementOf(EnumSet.of(IntegrationType.CUSTOM)));
  }

  /**
   * Detects only the selected built-in integrations, including later plugin enable events.
   * An empty set disables auto-detection. Register custom integrations after initialization.
   */
  public void init(JavaPlugin plugin, Set<IntegrationType> enabledTypes) {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(enabledTypes, "enabledTypes");
    Set<IntegrationType> selected = EnumSet.noneOf(IntegrationType.class);
    for (IntegrationType type : enabledTypes) selected.add(Objects.requireNonNull(type, "integration type"));
    if (selected.contains(IntegrationType.CUSTOM)) {
      throw new IllegalArgumentException("CUSTOM integrations must be registered explicitly");
    }
    close();
    owner = plugin;
    enabledIntegrations.addAll(selected);
    for (IntegrationType type : enabledIntegrations) loadIntegration(type);
    listener = new Listener() {
      @EventHandler
      public void onEnable(PluginEnableEvent event) {
        for (IntegrationType type : enabledIntegrations) {
          if (event.getPlugin().getName().equals(type.getPluginName())) loadIntegration(type);
        }
      }

      @EventHandler
      public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == owner) {
          close();
          return;
        }
        for (IntegrationType type : IntegrationType.values()) {
          if (event.getPlugin().getName().equals(type.getPluginName())) {
            unregisterIntegration(type.getId());
          }
        }
      }
    };
    Bukkit.getPluginManager().registerEvents(listener, plugin);
  }

  public boolean canBuild(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.BUILD, integration -> integration.canBuild(player, location), false).allowed();
  }

  /** Checks placement using the material to be placed, without changing the world. */
  public boolean canPlace(Player player, Location location, Material material) {
    validatePlacement(player, location, material);
    return evaluate(Action.PLACE, integration -> integration.canPlace(player, location, material), false).allowed();
  }

  public boolean canBreak(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.BREAK, integration -> integration.canBreak(player, location), false).allowed();
  }

  public boolean canInteract(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.INTERACT_BLOCK, integration -> integration.canInteract(player, location), false).allowed();
  }

  public boolean canInteract(Player player, Entity entity) {
    validateEntity(player, entity);
    return evaluate(Action.INTERACT_ENTITY, integration -> integration.canInteract(player, entity), false).allowed();
  }

  public boolean canUse(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.USE, integration -> integration.canUse(player, location), false).allowed();
  }

  public boolean canDamage(Player player, Entity entity) {
    validateEntity(player, entity);
    return evaluate(Action.DAMAGE, integration -> integration.canDamage(player, entity), false).allowed();
  }

  /** Detailed checks evaluate every integration and report all denials and API errors. */
  public ProtectionResult checkBuild(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.BUILD, integration -> integration.canBuild(player, location), true);
  }

  public ProtectionResult checkPlace(Player player, Location location, Material material) {
    validatePlacement(player, location, material);
    return evaluate(Action.PLACE, integration -> integration.canPlace(player, location, material), true);
  }

  public ProtectionResult checkBreak(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.BREAK, integration -> integration.canBreak(player, location), true);
  }

  public ProtectionResult checkInteract(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.INTERACT_BLOCK, integration -> integration.canInteract(player, location), true);
  }

  public ProtectionResult checkInteract(Player player, Entity entity) {
    validateEntity(player, entity);
    return evaluate(Action.INTERACT_ENTITY, integration -> integration.canInteract(player, entity), true);
  }

  public ProtectionResult checkUse(Player player, Location location) {
    validate(player, location);
    return evaluate(Action.USE, integration -> integration.canUse(player, location), true);
  }

  public ProtectionResult checkDamage(Player player, Entity entity) {
    validateEntity(player, entity);
    return evaluate(Action.DAMAGE, integration -> integration.canDamage(player, entity), true);
  }

  public void setDebug(boolean debugMode) { debug = debugMode; }

  /** Ignores integration errors when enabled. Disabled by default; other integrations can still deny. */
  public void setFailOpen(boolean failOpen) { this.failOpen = failOpen; }

  /** Registers under the type ID, replacing the previous registration with that ID. */
  public void registerIntegration(Integration integration) {
    Objects.requireNonNull(integration, "integration");
    IntegrationType type = Objects.requireNonNull(integration.getType(), "integration.type");
    registerIntegration(type.getId(), integration);
  }

  /**
   * Registers an independent integration by ID. Reusing an ID replaces only that registration.
   * Built-in IDs are reserved for integrations of the matching type.
   */
  public void registerIntegration(String id, Integration integration) {
    validateId(id);
    Objects.requireNonNull(integration, "integration");
    IntegrationType type = Objects.requireNonNull(integration.getType(), "integration.type");
    IntegrationType reserved = typeForId(id);
    if (reserved != null && reserved != type) {
      throw new IllegalArgumentException("Integration ID " + id + " is reserved for " + reserved);
    }
    integrations.put(id, new Registration(type, integration));
    if (reserved != null) failedIntegrations.remove(reserved);
    reportedFailures.remove(id);
  }

  /** Removes a registration or its initialization failure. Returns whether anything was removed. */
  public boolean unregisterIntegration(String id) {
    validateId(id);
    boolean removed = integrations.remove(id) != null;
    IntegrationType type = typeForId(id);
    if (type != null) removed |= failedIntegrations.remove(type) != null;
    reportedFailures.remove(id);
    return removed;
  }

  /**
   * A view by type for compatibility. If several registrations share a type, the last one is returned.
   * Use getRegisteredIntegrations() to inspect every named registration.
   */
  public Map<IntegrationType, Integration> getIntegrations() {
    Map<IntegrationType, Integration> byType = new EnumMap<>(IntegrationType.class);
    integrations.values().forEach(registration -> byType.put(registration.type(), registration.integration()));
    return Map.copyOf(byType);
  }

  public Map<String, Integration> getRegisteredIntegrations() {
    Map<String, Integration> byId = new LinkedHashMap<>();
    integrations.forEach((id, registration) -> byId.put(id, registration.integration()));
    return Map.copyOf(byId);
  }

  /** Built-in integrations that failed to initialize; check results also include runtime errors. */
  public Set<IntegrationType> getFailedIntegrations() { return Set.copyOf(failedIntegrations.keySet()); }

  public Set<IntegrationType> getEnabledIntegrations() { return Set.copyOf(enabledIntegrations); }

  /** Releases integrations and listeners. Also called automatically when the owner disables. */
  @Override
  public void close() {
    if (listener != null) HandlerList.unregisterAll(listener);
    listener = null;
    integrations.clear();
    failedIntegrations.clear();
    reportedFailures.clear();
    enabledIntegrations.clear();
    owner = null;
  }

  private void loadIntegration(IntegrationType type) {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(type.getPluginName());
    if (plugin == null || !plugin.isEnabled()) return;
    try {
      Class<?> adapter = Class.forName(type.getIntegrationClass(), true, getClass().getClassLoader());
      Integration integration = (Integration) adapter.getConstructor(JavaPlugin.class, Plugin.class)
          .newInstance(owner, plugin);
      registerIntegration(integration);
    } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
      Throwable cause = failure instanceof InvocationTargetException && failure.getCause() != null
          ? failure.getCause() : failure;
      integrations.remove(type.getId());
      failedIntegrations.put(type, cause);
      report(type.getId(), cause);
    }
  }

  private ProtectionResult evaluate(Action action, PermissionCheck check, boolean detailed) {
    var issues = new ArrayList<Issue>();
    boolean allowed = true;
    for (var failure : failedIntegrations.entrySet()) {
      issues.add(new Issue(failure.getKey().getId(), failure.getKey(), Reason.INITIALIZATION_ERROR, failure.getValue()));
      if (!failOpen) allowed = false;
    }
    if (!allowed && !detailed) return new ProtectionResult(action, false, issues);
    for (var entry : integrations.entrySet()) {
      Registration registration = entry.getValue();
      try {
        if (!check.test(registration.integration())) {
          issues.add(new Issue(entry.getKey(), registration.type(), Reason.DENIED, null));
          allowed = false;
        }
      } catch (RuntimeException | LinkageError failure) {
        issues.add(new Issue(entry.getKey(), registration.type(), Reason.CHECK_ERROR, failure));
        report(entry.getKey(), failure);
        if (!failOpen) allowed = false;
      }
      if (!allowed && !detailed) break;
    }
    return new ProtectionResult(action, allowed, issues);
  }

  private void report(String id, Throwable failure) {
    if (reportedFailures.add(id) || debug) {
      var logger = owner == null ? java.util.logging.Logger.getLogger("BoundaryGuard") : owner.getLogger();
      String message = "BoundaryGuard: " + id + " integration failed; "
          + (failOpen ? "ignoring this failure." : "denying this action.");
      if (debug) logger.log(Level.WARNING, message, failure);
      else logger.warning(message + " " + failure);
    }
  }

  private IntegrationType typeForId(String id) {
    for (IntegrationType type : IntegrationType.values()) {
      if (type.getId().equals(id)) return type;
    }
    return null;
  }

  private void validateId(String id) {
    Objects.requireNonNull(id, "id");
    if (id.isBlank()) throw new IllegalArgumentException("Integration ID must not be blank");
  }

  private void validate(Player player, Location location) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(location, "location");
    if (location.getWorld() == null) throw new IllegalArgumentException("Target world is not loaded");
  }

  private void validateEntity(Player player, Entity entity) {
    Objects.requireNonNull(entity, "entity");
    validate(player, entity.getLocation());
  }

  private void validatePlacement(Player player, Location location, Material material) {
    validate(player, location);
    Objects.requireNonNull(material, "material");
    if (!material.isBlock() || material.isAir()) {
      throw new IllegalArgumentException("Placement material must be a non-air block");
    }
  }

  private record Registration(IntegrationType type, Integration integration) {}

  @FunctionalInterface
  private interface PermissionCheck { boolean test(Integration integration); }
}
