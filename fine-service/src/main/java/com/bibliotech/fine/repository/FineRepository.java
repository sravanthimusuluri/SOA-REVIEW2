package com.bibliotech.fine.repository;

import com.bibliotech.fine.entity.Fine;
import com.bibliotech.fine.entity.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    List<Fine> findByStudentId(String studentId);
    List<Fine> findByStudentUsername(String studentUsername);
    Optional<Fine> findByRentalId(Long rentalId);
    List<Fine> findByStatus(FineStatus status);
}
