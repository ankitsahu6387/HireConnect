package com.hireconnect.jobservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hireconnect.jobservice.entity.Job;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByEmployerId(Long employerId);

    @Query("""
            select j from Job j
            where (:keyword is null or lower(j.title) like lower(concat('%', :keyword, '%'))
                or lower(j.description) like lower(concat('%', :keyword, '%'))
                or lower(j.skills) like lower(concat('%', :keyword, '%')))
            and (:location is null or lower(j.location) like lower(concat('%', :location, '%')))
            and (:category is null or lower(j.category) = lower(:category))
            and (:type is null or lower(j.type) = lower(:type))
            and (:status is null or lower(j.status) = lower(:status))
            """)
    List<Job> searchJobs(@Param("keyword") String keyword,
                         @Param("location") String location,
                         @Param("category") String category,
                         @Param("type") String type,
                         @Param("status") String status);
}
