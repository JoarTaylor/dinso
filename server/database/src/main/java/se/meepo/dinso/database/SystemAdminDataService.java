package se.meepo.dinso.database;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.meepo.dinso.database.entity.CompanyAuthorizationEntity;
import se.meepo.dinso.database.entity.CompanyEntity;
import se.meepo.dinso.database.repository.CompanyAuthorizationRepository;
import se.meepo.dinso.database.repository.CompanyRepository;
import se.meepo.dinso.database.repository.DemoProfileRepository;
import se.meepo.dinso.service.CustomerId;
import se.meepo.dinso.service.DemoPermission;
import se.meepo.dinso.service.PortalType;

@Service
@Transactional(readOnly = true)
public class SystemAdminDataService {

  private final DemoProfileRepository profiles;
  private final CompanyAuthorizationRepository authorizations;
  private final CompanyRepository companies;

  public SystemAdminDataService(
      DemoProfileRepository profiles,
      CompanyAuthorizationRepository authorizations,
      CompanyRepository companies) {
    this.profiles = profiles;
    this.authorizations = authorizations;
    this.companies = companies;
  }

  public List<CompanyProfile> listCompanyProfiles(CustomerId customer) {
    return profiles.findByCustomerIdAndPortal(customer, PortalType.COMPANY).stream()
        .map(
            profile -> {
              var auths = authorizations.findByProfile(profile);
              var companyAccess =
                  auths.stream()
                      .map(
                          a ->
                              new CompanyAccess(
                                  a.getCompany().getExternalId(),
                                  a.getCompany().getName(),
                                  a.getPermissions()))
                      .toList();
              return new CompanyProfile(
                  profile.getExternalId(), profile.getName(), companyAccess);
            })
        .toList();
  }

  @Transactional
  public void updatePermissions(
      CustomerId customer, String profileId, String companyId, Set<DemoPermission> permissions) {
    var profile =
        profiles
            .findByCustomerIdAndExternalId(customer, profileId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown profile"));
    var company =
        companies
            .findByCustomerIdAndExternalId(customer, companyId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown company"));
    var auth =
        authorizations
            .findByProfileAndCompany(profile, company)
            .orElseGet(
                () -> {
                  var newAuth = new CompanyAuthorizationEntity(profile, company, permissions);
                  authorizations.save(newAuth);
                  return newAuth;
                });
    auth.setPermissions(permissions);
  }

  public record CompanyAccess(String companyId, String companyName, Set<DemoPermission> permissions) {}

  public record CompanyProfile(String profileId, String name, List<CompanyAccess> companies) {}
}
