# BoundaryGuard

A small Java library that gives Bukkit plugins one API for block protection,
entity interactions and damage checks. Requires Java 21 and Bukkit/Spigot/Paper
1.20.4 or a compatible newer server.

Supports WorldGuard, Towny, PlotSquared, FactionsUUID, Lands, CrashClaim,
GriefPrevention, HuskClaims, HuskTowns, BentoBox, Residence and NoBuildPlus.
Protection plugin APIs are compile-only dependencies and are not bundled.

## Server setup

BoundaryGuard is bundled inside plugins that use it. Install those plugins and
your chosen protection plugins in versions compatible with your server.
Integration settings are controlled by the plugin using BoundaryGuard.

## Add to your plugin

Add the repository and dependency:

```kotlin
repositories {
    maven("https://repo.vao.zone/releases")
}

dependencies {
    implementation("zone.vao:BoundaryGuard:1.0.0")
}
```

Include BoundaryGuard in your plugin JAR using Maven Shade or Gradle Shadow.
Keep all integration classes when using minimization: adapters are loaded through
reflection. Declare your server API
separately as a compile-only dependency.

Add supported protection plugins to your plugin's `plugin.yml`:

```yaml
softdepend: [WorldGuard, Towny, PlotSquared, Factions, Lands, CrashClaim, GriefPrevention, HuskClaims, HuskTowns, BentoBox, Residence, NoBuildPlus]
```

## Usage

```java
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.BoundaryGuard;

public final class MyPlugin extends JavaPlugin {
    private final BoundaryGuard protection = new BoundaryGuard();

    @Override
    public void onEnable() {
        protection.init(this);
    }

    @Override
    public void onDisable() {
        protection.close();
    }
}
```

Call checks on the server thread. All active integrations must allow an action.
If no integrations are active, checks allow it. Arguments must be non-null and
the target world must be loaded.

```java
boolean place = protection.canPlace(player, location, Material.STONE);
boolean destroy = protection.canBreak(player, location);
boolean blockInteraction = protection.canInteract(player, location);
boolean entityInteraction = protection.canInteract(player, entity);
boolean use = protection.canUse(player, location);
boolean damage = protection.canDamage(player, entity);
```

`canPlace` accepts the material being placed and rejects air and non-block items.
`canBuild(player, location)` remains available for general build checks.
Entity checks use the target entity's location.

To detect only selected built-in integrations:

```java
protection.init(this, EnumSet.of(IntegrationType.WORLDGUARD, IntegrationType.TOWNY));
```

Use `Set.of()` to disable automatic detection. The selection also applies when
plugins are enabled later. Reinitializing clears previous registrations.

## Detailed results

Every `can*` method has a matching `check*` method that evaluates all integrations:

```java
ProtectionResult result = protection.checkPlace(player, location, Material.STONE);
if (!result.allowed()) {
    for (ProtectionResult.Issue issue : result.issues()) {
        getLogger().warning(issue.integrationId() + ": " + issue.reason());
    }
}
```

Reasons are `DENIED`, `INITIALIZATION_ERROR` and `CHECK_ERROR`.
`issue.error()` contains the original exception for errors and is null for denials.

Integration errors block checks by default. `setFailOpen(true)` ignores errors
while still respecting explicit denials. Detailed results retain ignored errors.
Use `setDebug(true)` to log stack traces.

## Custom integrations

Implement `zone.vao.boundaryGuard.Integration` and register it after initialization:

```java
protection.registerIntegration("my-protection", myIntegration);
protection.registerIntegration("another-protection", anotherIntegration);
protection.unregisterIntegration("my-protection");
```

Each ID is independent; registering the same ID replaces its previous adapter.
Built-in IDs such as `worldguard` are reserved for the matching integration type.
`getRegisteredIntegrations()` returns an immutable snapshot of all registrations.
Custom adapters can override `canPlace` and entity `canInteract`; their defaults
delegate to the existing build and location interaction methods.
