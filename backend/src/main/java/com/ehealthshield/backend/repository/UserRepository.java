package com.ehealthshield.backend.repository;

import com.ehealthshield.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByWalletAddress(String walletAddress);
    Optional<UserEntity> findByWalletAddressIgnoreCase(String walletAddress);
    boolean existsByWalletAddress(String walletAddress);
    boolean existsByWalletAddressIgnoreCase(String walletAddress);
}
