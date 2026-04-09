package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}
