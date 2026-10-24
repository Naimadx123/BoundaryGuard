package zone.vao.boundaryGuard;

import zone.vao.boundaryGuard.enums.IntegrationType;

import java.util.List;
import java.util.Objects;

/**
 * A snapshot of all refusals and integration errors for one check.
 * Errors remain in issues even when fail-open permits the action.
 */
public record ProtectionResult(Action action, boolean allowed, List<Issue> issues) {
  public ProtectionResult {
    Objects.requireNonNull(action, "action");
    issues = List.copyOf(issues);
  }

  public enum Action {
    BUILD, PLACE, BREAK, INTERACT_BLOCK, INTERACT_ENTITY, USE, DAMAGE
  }

  public enum Reason {
    DENIED, INITIALIZATION_ERROR, CHECK_ERROR
  }

  /** Error is null for a normal denial and contains the original cause for an API error. */
  public record Issue(String integrationId, IntegrationType integrationType, Reason reason, Throwable error) {
    public Issue {
      Objects.requireNonNull(integrationId, "integrationId");
      Objects.requireNonNull(integrationType, "integrationType");
      Objects.requireNonNull(reason, "reason");
      if (reason == Reason.DENIED && error != null) {
        throw new IllegalArgumentException("A denial must not contain an error");
      }
      if (reason != Reason.DENIED) Objects.requireNonNull(error, "error");
    }
  }
}
