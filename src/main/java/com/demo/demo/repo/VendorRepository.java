package com.demo.demo.repo;

import com.demo.demo.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    // default JpaRepository methods are enough (findById, findAll)
}