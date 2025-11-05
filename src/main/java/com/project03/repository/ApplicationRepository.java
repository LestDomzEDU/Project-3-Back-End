package com.project03.repository;

import com.project03.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * TEMPORARY SAFE VERSION:
     * Avoids JPQL parsing errors at startup by not declaring a @Query.
     * Returns all applications for now. We'll refine once we confirm entity field names.
     *
     * TODO: Replace with a proper @Query after you share the actual fields
     *       (e.g., deadline, student.id/user.id, school.name/status, etc.).
     */
    default List<Application> findUpcomingDeadlines(String q, Long studentId) {
        // Basic fallback – no filtering to avoid JPQL parser issues.
        // You can do in-memory filtering here if you want,
        // but leaving it simple prevents boot-time crashes.
        return findAll();
    }
}
