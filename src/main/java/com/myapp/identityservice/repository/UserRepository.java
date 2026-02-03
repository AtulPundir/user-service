package com.myapp.identityservice.repository;

import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByIdentityKey(String identityKey);

    boolean existsByIdentityKey(String identityKey);

    List<User> findByIdIn(List<String> ids);

    Optional<User> findByAuthUserId(String authUserId);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByAuthUserId(String authUserId);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    Page<User> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.createdAt DESC")
    Page<User> findByStatusOrderByCreatedAtDesc(@Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isVerified = :verified ORDER BY u.createdAt DESC")
    Page<User> findByIsVerifiedOrderByCreatedAtDesc(@Param("verified") boolean verified, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.isVerified = :verified ORDER BY u.createdAt DESC")
    Page<User> findByStatusAndIsVerifiedOrderByCreatedAtDesc(
            @Param("status") UserStatus status,
            @Param("verified") boolean verified,
            Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.id <> :id")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") String id);

    // Invariant check queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = true AND u.authUserId IS NULL")
    long countVerifiedWithoutAuthUserId();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = true AND u.identityKey IS NULL")
    long countVerifiedWithoutIdentityKey();

    @Query(value = "SELECT COUNT(*) FROM (SELECT auth_user_id FROM users WHERE auth_user_id IS NOT NULL GROUP BY auth_user_id HAVING COUNT(*) > 1) sub", nativeQuery = true)
    long countDuplicateAuthUserIds();

    @Query("SELECT u FROM User u WHERE u.isVerified = true AND u.authUserId IS NULL")
    List<User> findVerifiedWithoutAuthUserId();
}
