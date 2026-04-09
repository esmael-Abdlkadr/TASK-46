package com.eaglepoint.workforce.repository;
import com.eaglepoint.workforce.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByNameContainingIgnoreCase(String keyword);
}
