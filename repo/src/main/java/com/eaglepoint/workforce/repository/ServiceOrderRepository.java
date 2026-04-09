package com.eaglepoint.workforce.repository;
import com.eaglepoint.workforce.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    List<ServiceOrder> findByOrderCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String code, String desc);
}
