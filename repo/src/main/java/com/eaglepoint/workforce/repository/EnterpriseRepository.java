package com.eaglepoint.workforce.repository;
import com.eaglepoint.workforce.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {
    List<Enterprise> findByNameContainingIgnoreCase(String keyword);
}
