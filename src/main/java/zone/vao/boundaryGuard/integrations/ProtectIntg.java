package zone.vao.boundaryGuard.integrations;

import net.thenextlvl.protect.service.ProtectionService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class ProtectIntg implements Integration {
  private final ProtectionService service;

  public ProtectIntg(JavaPlugin owner, Plugin plugin) {
    service = Bukkit.getServicesManager().load(ProtectionService.class);
    if (service == null) throw new IllegalStateException("Protect ProtectionService is not registered");
  }

  @Override
  public IntegrationType getType() { return IntegrationType.PROTECT; }

  @Override
  public boolean canBuild(Player player, Location location) {
    return service.canPlace(player, location);
  }

  @Override
  public boolean canBreak(Player player, Location location) {
    return service.canDestroy(player, location);
  }

  @Override
  public boolean canInteract(Player player, Location location) {
    return service.canInteract(player, location);
  }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    return service.canInteract(player, entity);
  }

  @Override
  public boolean canUse(Player player, Location location) {
    return service.canInteract(player, location);
  }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    return service.canAttack(player, entity);
  }
}
