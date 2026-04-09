package com.eaglepoint.workforce.repository;
import com.eaglepoint.workforce.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByFullNameContainingIgnoreCase(String keyword);
}
