package com.myapp.identityservice.repository;

import com.myapp.identityservice.domain.UserMonthlyUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMonthlyUsageRepository extends JpaRepository<UserMonthlyUsage, String> {

    @Query("SELECT u FROM UserMonthlyUsage u WHERE u.user.id = :userId AND u.year = :year AND u.month = :month")
    Optional<UserMonthlyUsage> findByUserIdAndYearAndMonth(
            @Param("userId") String userId,
            @Param("year") int year,
            @Param("month") int month
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserMonthlyUsage u WHERE u.user.id = :userId AND u.year = :year AND u.month = :month")
    Optional<UserMonthlyUsage> findByUserIdAndYearAndMonthWithLock(
            @Param("userId") String userId,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("SELECT u FROM UserMonthlyUsage u WHERE u.user.id = :userId AND u.year = :year ORDER BY u.month ASC")
    List<UserMonthlyUsage> findByUserIdAndYear(@Param("userId") String userId, @Param("year") int year);

    @Query("SELECT u FROM UserMonthlyUsage u WHERE u.user.id = :userId ORDER BY u.year DESC, u.month DESC")
    List<UserMonthlyUsage> findByUserIdOrderByYearDescMonthDesc(@Param("userId") String userId);

    @Query(value = "SELECT u FROM UserMonthlyUsage u WHERE u.user.id = :userId ORDER BY u.year DESC, u.month DESC LIMIT 12")
    List<UserMonthlyUsage> findLast12MonthsByUserId(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE UserMonthlyUsage u SET u.utilised = u.utilised + :amount WHERE u.id = :id")
    int incrementUtilised(@Param("id") String id, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE UserMonthlyUsage u SET u.monthlyLimit = :monthlyLimit WHERE u.user.id = :userId AND u.year = :year AND u.month = :month")
    int updateMonthlyLimit(
            @Param("userId") String userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("monthlyLimit") int monthlyLimit
    );
}
