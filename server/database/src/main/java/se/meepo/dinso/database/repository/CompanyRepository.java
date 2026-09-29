package se.meepo.dinso.database.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.meepo.dinso.database.entity.CompanyEntity;
import se.meepo.dinso.service.CustomerId;

public interface CompanyRepository extends JpaRepository<CompanyEntity, String> {
  Optional<CompanyEntity> findByCustomerIdAndExternalId(CustomerId customerId, String externalId);
}
