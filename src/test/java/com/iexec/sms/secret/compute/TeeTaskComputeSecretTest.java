package com.iexec.sms.secret.compute;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@DataJpaTest
public class TeeTaskComputeSecretTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;

    private TeeTaskComputeSecret getAppDeveloperSecret() {
        return TeeTaskComputeSecret.builder()
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .onChainObjectAddress("0x1")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .fixedSecretOwner("")
                .key("0")
                .value("secretValue")
                .build();
    }

    private TeeTaskComputeSecret getRequesterSecret() {
        return TeeTaskComputeSecret.builder()
                .secretOwnerRole(SecretOwnerRole.REQUESTER)
                .onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .fixedSecretOwner("0x1")
                .key("secret-key")
                .value("secretValue")
                .build();
    }

    @BeforeEach
    void preflight() {
        teeTaskComputeSecretRepository.deleteAll();
    }

    @Test
    void shouldNotSaveSecretFromDefaultBuilder() {
        log.info("shouldNotSaveEntity");
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder().build();
        assertThat(secret).hasAllNullFieldsOrProperties();
        teeTaskComputeSecretRepository.save(secret);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldNotSaveSecretWhenOnChainObjectAddressIsNull() {
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder()
                //.onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .fixedSecretOwner("")
                .key("")
                .value("")
                .build();
        teeTaskComputeSecretRepository.save(secret);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldNotSaveSecretWhenOnChainObjectTypeIsNull() {
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder()
                .onChainObjectAddress("")
                //.onChainObjectType(OnChainObjectType.APPLICATION)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .fixedSecretOwner("")
                .key("")
                .value("")
                .build();
        teeTaskComputeSecretRepository.save(secret);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldNotSaveSecretWhenSecretOwnerRoleIsNull() {
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder()
                .onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                //.secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .fixedSecretOwner("")
                .key("")
                .value("")
                .build();
        teeTaskComputeSecretRepository.save(secret);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldNotSaveSecretWhenFixedSecretOwnerIsNull() {
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder()
                .onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                //.fixedSecretOwner("")
                .key("")
                .value("")
                .build();
        teeTaskComputeSecretRepository.save(secret);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldNotSaveSecretWhenKeyIsNull() {
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder()
                .onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .fixedSecretOwner("")
                //.key("")
                .value("")
                .build();
        teeTaskComputeSecretRepository.save(secret);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldNotSaveSecretWhenValueIsNull() {
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder()
                .onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .fixedSecretOwner("")
                .key("")
                //.value("")
                .build();
        teeTaskComputeSecretRepository.save(secret);
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldFailToSaveSameAppDeveloperSecretTwice() {
        log.info("AppDeveloperSecret");
        teeTaskComputeSecretRepository.save(getAppDeveloperSecret());
        teeTaskComputeSecretRepository.save(getAppDeveloperSecret());
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void shouldFailToSaveSameRequesterSecretTwice() {
        log.info("RequesterSecret");
        teeTaskComputeSecretRepository.save(getRequesterSecret());
        teeTaskComputeSecretRepository.save(getRequesterSecret());
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void shouldSaveAppDeveloperAndRequesterSecrets() {
        TeeTaskComputeSecret appDeveloperSecret = getAppDeveloperSecret();
        teeTaskComputeSecretRepository.save(appDeveloperSecret);
        String appDeveloperSecretId = appDeveloperSecret.getId();
        TeeTaskComputeSecret requesterSecret = getRequesterSecret();
        teeTaskComputeSecretRepository.save(requesterSecret);
        String requesterSecretId = requesterSecret.getId();
        assertThat(teeTaskComputeSecretRepository.count()).isEqualTo(2);
        assertThat(teeTaskComputeSecretRepository.getById(appDeveloperSecretId))
                .isEqualTo(appDeveloperSecret);
        assertThat(teeTaskComputeSecretRepository.getById(requesterSecretId))
                .isEqualTo(requesterSecret);
    }

}
