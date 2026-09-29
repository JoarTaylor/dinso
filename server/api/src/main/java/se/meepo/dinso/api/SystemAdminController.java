package se.meepo.dinso.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.meepo.dinso.database.DemoSessionService;
import se.meepo.dinso.database.SystemAdminDataService;
import se.meepo.dinso.service.DemoPermission;
import se.meepo.dinso.service.DemoRole;
import se.meepo.dinso.service.PortalType;

@RestController
@RequestMapping("/api/system")
public class SystemAdminController {
  private final DemoSessionService sessions;
  private final SystemAdminDataService data;

  public SystemAdminController(DemoSessionService sessions, SystemAdminDataService data) {
    this.sessions = sessions;
    this.data = data;
  }

  @GetMapping("/profiles")
  List<SystemAdminDataService.CompanyProfile> profiles(HttpServletRequest request) {
    var profile = current(request);
    return data.listCompanyProfiles(profile.customerId());
  }

  @PutMapping("/profiles/{profileId}/companies/{companyId}/permissions")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void updatePermissions(
      HttpServletRequest request,
      @PathVariable("profileId") String profileId,
      @PathVariable("companyId") String companyId,
      @RequestBody PermissionsRequest input) {
    var profile = current(request);
    data.updatePermissions(profile.customerId(), profileId, companyId, input.permissions());
  }

  private se.meepo.dinso.service.DemoProfile current(HttpServletRequest request) {
    var value = request.getHeader("Authorization");
    if (value == null || !value.startsWith("Bearer ")) throw new Forbidden();
    var profile = sessions.requireActive(value.substring(7));
    if (profile.role() != DemoRole.SYSTEM_ADMIN) throw new Forbidden();
    return profile;
  }

  public record PermissionsRequest(Set<DemoPermission> permissions) {}

  @ResponseStatus(HttpStatus.FORBIDDEN)
  static class Forbidden extends RuntimeException {}
}
