package zone.vao.boundaryGuard.integrations;

import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Allay;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Villager;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zone.vao.boundaryGuard.Integration;
import zone.vao.boundaryGuard.enums.IntegrationType;

public class BentoBoxIntg implements Integration {
  @Override
  public IntegrationType getType() { return IntegrationType.BENTOBOX; }

  private final BentoBox plugin;

  public BentoBoxIntg(JavaPlugin owner, Plugin plugin) { this.plugin = (BentoBox) plugin; }

  @Override
  public boolean canBuild(Player player, Location location) { return canDo(player, location, Flags.PLACE_BLOCKS); }

  @Override
  public boolean canBreak(Player player, Location location) { return canDo(player, location, Flags.BREAK_BLOCKS); }

  @Override
  public boolean canInteract(Player player, Location location) {
    return !plugin.getIWM().inWorld(location) || plugin.getIslands().locationIsOnIsland(player, location);
  }

  @Override
  public boolean canInteract(Player player, Entity entity) {
    Flag flag;
    if (entity instanceof ItemFrame) flag = Flags.ITEM_FRAME;
    else if (entity instanceof ArmorStand) flag = Flags.ARMOR_STAND;
    else if (entity instanceof AbstractVillager) flag = Flags.TRADING;
    else if (entity instanceof Allay) flag = Flags.ALLAY;
    else if (entity instanceof StorageMinecart || entity instanceof ChestBoat && player.isSneaking()) flag = Flags.CHEST;
    else if (entity instanceof HopperMinecart) flag = Flags.HOPPER;
    else if (entity instanceof PoweredMinecart) flag = Flags.FURNACE;
    else if (entity instanceof Boat) flag = Flags.BOAT;
    else if (entity instanceof Minecart) flag = Flags.MINECART;
    else if (entity instanceof AbstractHorse) flag = Flags.RIDING;
    else return canInteract(player, entity.getLocation());
    return canDo(player, entity.getLocation(), flag);
  }

  @Override
  public boolean canUse(Player player, Location location) { return canInteract(player, location); }

  @Override
  public boolean canDamage(Player player, Entity entity) {
    Location location = entity.getLocation();
    if (entity instanceof Animals) return canDo(player, location, Flags.HURT_ANIMALS);
    if (entity instanceof Monster) return canDo(player, location, Flags.HURT_MONSTERS);
    if (entity instanceof Villager) return canDo(player, location, Flags.HURT_VILLAGERS);
    if (entity instanceof Player) {
      if (!plugin.getIWM().inWorld(location)) return true;
      World.Environment environment = entity.getWorld().getEnvironment();
      Flag flag = environment == World.Environment.NETHER ? Flags.PVP_NETHER
          : environment == World.Environment.THE_END ? Flags.PVP_END : Flags.PVP_OVERWORLD;
      return canDo(player, location, flag);
    }
    return true;
  }

  private boolean canDo(Player player, Location location, Flag flag) {
    if (!plugin.getIWM().inWorld(location)) return true;
    return plugin.getIslands().getIslandAt(location)
        .map(island -> island.isAllowed(User.getInstance(player), flag))
        .orElseGet(() -> !flag.isSetForWorld(location.getWorld()));
  }
}
