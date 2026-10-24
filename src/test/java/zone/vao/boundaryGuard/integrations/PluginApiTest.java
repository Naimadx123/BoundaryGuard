package zone.vao.boundaryGuard.integrations;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PluginApiTest {
  public static class ExampleApi {
    public String pick(Entity entity) { return "entity"; }
    public String pick(Player player) { return "player"; }
    public boolean flag(String world, boolean enabled) { return enabled; }
    public void broken() { throw new IllegalStateException("original cause"); }
  }

  @Test
  void resolvesSpecificOverloadAndCachesIt() {
    PluginApi api = new PluginApi(mock(Plugin.class));
    ExampleApi target = new ExampleApi();
    Player player = mock(Player.class);
    assertEquals("player", api.call(target, "pick", player));
    assertEquals("player", api.call(target, "pick", player));
    assertEquals("entity", api.call(target, "pick", mock(Entity.class)));
    assertEquals(true, api.call(target, "flag", "world", true));
  }

  @Test
  void preservesFailureAndRejectsIncompatibleApi() {
    PluginApi api = new PluginApi(mock(Plugin.class));
    ExampleApi target = new ExampleApi();
    var failure = assertThrows(IllegalStateException.class, () -> api.call(target, "broken"));
    assertEquals("original cause", failure.getMessage());
    assertThrows(IllegalStateException.class, () -> api.call(target, "missing"));
    assertThrows(IllegalStateException.class, () -> api.type("missing.protection.Plugin"));
  }
}
