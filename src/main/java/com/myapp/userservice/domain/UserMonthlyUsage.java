package com.myapp.userservice.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_monthly_usage",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_year_month", columnNames = {"user_id", "year", "month"})
    },
    indexes = {
        @Index(name = "idx_usage_user_id", columnList = "user_id"),
        @Index(name = "idx_usage_year_month", columnList = "year, month"),
        @Index(name = "idx_usage_user_year_month", columnList = "user_id, year, month")
    }
)
public class UserMonthlyUsage {

    @Id
    @Column(name = "id", length = 30)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usage_user"))
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private String userId;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "monthly_limit", nullable = false)
    private int monthlyLimit;

    @Column(name = "utilised", nullable = false)
    private int utilised = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserMonthlyUsage() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserId() {
        return userId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(int monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public int getUtilised() {
        return utilised;
    }

    public void setUtilised(int utilised) {
        this.utilised = utilised;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isUnlimited() {
        return monthlyLimit < 0;
    }

    public int getRemaining() {
        if (isUnlimited()) {
            return -1;
        }
        return Math.max(0, monthlyLimit - utilised);
    }
}
