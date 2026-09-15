package com.ehealthshield.backend.config;

import com.ehealthshield.backend.crypto.MlKemCryptoService;
import com.ehealthshield.backend.crypto.MlKemKeyPair;
import com.ehealthshield.backend.entity.UserEntity;
import com.ehealthshield.backend.entity.UserRole;
import com.ehealthshield.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("!test")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    public static final String DEV_PATIENT_WALLET = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";

    private final UserRepository userRepository;
    private final MlKemCryptoService mlKemCryptoService;

    public DevDataInitializer(UserRepository userRepository, MlKemCryptoService mlKemCryptoService) {
        this.userRepository = userRepository;
        this.mlKemCryptoService = mlKemCryptoService;
    }

    @Override
    public void run(String... args) {
        try {
            if (!userRepository.existsByWalletAddress(DEV_PATIENT_WALLET)) {
                log.info("Initializing development test PATIENT with genuine ML-KEM-768 keypair...");
                MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();

                UserEntity patient = UserEntity.builder()
                        .walletAddress(DEV_PATIENT_WALLET)
                        .role(UserRole.PATIENT)
                        .kyberPublicKey(keyPair.publicKey())
                        .createdAt(Instant.now())
                        .build();

                userRepository.save(patient);
                log.info("Development test PATIENT inserted successfully into PostgreSQL. Wallet: {}", DEV_PATIENT_WALLET);
            } else {
                log.info("Development test PATIENT already present in PostgreSQL. Wallet: {}", DEV_PATIENT_WALLET);
            }
        } catch (Exception e) {
            log.warn("DevDataInitializer could not complete (database may be unreachable during certain startup scenarios): {}", e.getMessage());
        }
    }
}
