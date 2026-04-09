package com.eaglepoint.workforce.repository;
import com.eaglepoint.workforce.entity.RedemptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RedemptionRecordRepository extends JpaRepository<RedemptionRecord, Long> {
    List<RedemptionRecord> findByRedemptionCodeContainingIgnoreCaseOrItemDescriptionContainingIgnoreCase(String code, String desc);
}
