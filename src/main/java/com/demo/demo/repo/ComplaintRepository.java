package com.demo.demo.repo;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    // ✅ Add these
    long countByUser_Id(Long userId);
    List<Complaint> findByUser_IdOrderByCreatedAtDesc(Long userId);
}