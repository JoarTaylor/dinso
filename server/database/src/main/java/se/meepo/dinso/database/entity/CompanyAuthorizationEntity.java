package se.meepo.dinso.database.entity;

import jakarta.persistence.*;
import java.util.EnumSet;
import java.util.Set;
import se.meepo.dinso.service.DemoPermission;

@Entity
@Table(name = "company_authorization")
public class CompanyAuthorizationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(optional = false)
  private DemoProfileEntity profile;

  @ManyToOne(optional = false)
  private CompanyEntity company;

  @ElementCollection(fetch = FetchType.EAGER)
  @Enumerated(EnumType.STRING)
  @CollectionTable(
      name = "company_authorization_permissions",
      joinColumns = @JoinColumn(name = "authorization_id"))
  @Column(name = "permission")
  private Set<DemoPermission> permissions;

  protected CompanyAuthorizationEntity() {}

  public CompanyAuthorizationEntity(
      DemoProfileEntity profile, CompanyEntity company, Set<DemoPermission> permissions) {
    this.profile = profile;
    this.company = company;
    this.permissions = EnumSet.copyOf(permissions.isEmpty() ? EnumSet.noneOf(DemoPermission.class) : permissions);
  }

  public DemoProfileEntity getProfile() {
    return profile;
  }

  public CompanyEntity getCompany() {
    return company;
  }

  public Set<DemoPermission> getPermissions() {
    return permissions;
  }

  public void setPermissions(Set<DemoPermission> permissions) {
    this.permissions =
        permissions.isEmpty()
            ? java.util.EnumSet.noneOf(DemoPermission.class)
            : java.util.EnumSet.copyOf(permissions);
  }
}
