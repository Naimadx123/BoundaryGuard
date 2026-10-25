package zone.vao.boundaryGuard.enums;

import zone.vao.boundaryGuard.Integration;

import java.util.Locale;

/** Built-in integrations and their Bukkit plugin names. Factions means FactionsUUID. */
public enum IntegrationType {
  TOWNY("Towny", "TownyIntg"),
  WORLDGUARD("WorldGuard", "WorldGuardIntg"),
  PLOTSQUARED("PlotSquared", "PlotSquaredIntg"),
  FACTIONS("Factions", "FactionsUuidIntg"),
  LANDS("Lands", "LandsIntg"),
  CRASHCLAIM("CrashClaim", "CrashClaimIntg"),
  GRIEFPREVENTION("GriefPrevention", "GriefPreventionIntg"),
  HUSKCLAIMS("HuskClaims", "HuskClaimsIntg"),
  BENTOBOX("BentoBox", "BentoBoxIntg"),
  HUSKTOWNS("HuskTowns", "HuskTownsIntg"),
  RESIDENCE("Residence", "ResidenceIntg"),
  NOBUILDPLUS("NoBuildPlus", "NoBuildPlusIntg"),
  PROTECT("Protect", "ProtectIntg"),
  CUSTOM(null, null);

  private final String pluginName;
  private final String integrationClass;

  IntegrationType(String pluginName, String integrationClass) {
    this.pluginName = pluginName;
    this.integrationClass = integrationClass;
  }

  public String getId() { return name().toLowerCase(Locale.ROOT); }

  public String getPluginName() { return pluginName; }

  public String getIntegrationClass() {
    return integrationClass == null ? null : Integration.class.getPackageName() + ".integrations." + integrationClass;
  }
}
