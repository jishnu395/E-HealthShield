package com.ehealthshield.backend;

import com.ehealthshield.backend.repository.AccessPermissionRepository;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import com.ehealthshield.backend.repository.RecordSearchTagRepository;
import com.ehealthshield.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private EhrRecordRepository ehrRecordRepository;

    @MockitoBean
    private RecordSearchTagRepository recordSearchTagRepository;

    @MockitoBean
    private AccessPermissionRepository accessPermissionRepository;

    @Test
    void contextLoads() {
    }
}
