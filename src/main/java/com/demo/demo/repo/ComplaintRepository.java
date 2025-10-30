package com.demo.demo.repo;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findAllByOrderByCreatedAtDesc();
    // Count all complaints with a specific status
    long countByStatus(ComplaintStatus status);

    // ✅ Add these
    long countByUser_Id(Long userId);
    List<Complaint> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // inside public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    // return latest N complaints (Spring Data supports TopN)
    List<Complaint> findTop50ByOrderByCreatedAtDesc();




    // useful index hint method shown above already: findAllByOrderByCreatedAtDesc()
}
