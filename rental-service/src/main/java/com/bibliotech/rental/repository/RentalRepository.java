package com.bibliotech.rental.repository;

import com.bibliotech.rental.entity.Rental;
import com.bibliotech.rental.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByStudentIdOrderByCreatedAtDesc(String studentId);

    List<Rental> findByStudentUsernameOrderByCreatedAtDesc(String studentUsername);

    List<Rental> findByStatus(RentalStatus status);

    @Query("SELECT r FROM Rental r WHERE r.studentUsername = :username AND r.bookId = :bookId AND r.status IN :statuses")
    Optional<Rental> findActiveRentalByUsernameAndBookId(
            @Param("username") String username,
            @Param("bookId") Long bookId,
            @Param("statuses") Collection<RentalStatus> statuses);

    @Query("SELECT r FROM Rental r WHERE r.studentId = :studentId AND r.bookId = :bookId AND r.status IN :statuses")
    Optional<Rental> findActiveRentalByStudentIdAndBookId(
            @Param("studentId") String studentId,
            @Param("bookId") Long bookId,
            @Param("statuses") Collection<RentalStatus> statuses);

    @Query("SELECT r FROM Rental r WHERE r.status = 'ISSUED' AND r.dueDate < :today")
    List<Rental> findOverdueRentals(@Param("today") LocalDate today);
}
