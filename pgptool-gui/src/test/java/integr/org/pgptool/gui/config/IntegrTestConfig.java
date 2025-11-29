package integr.org.pgptool.gui.config;

import com.google.common.eventbus.EventBus;
import integr.org.pgptool.gui.TestTools;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.config.impl.ConfigRepositoryImpl;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.implpgp.EncryptionServicePgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyFilesOperationsPgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyGeneratorServicePgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyRingServicePgpImpl;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.api.UsageLoggerNoOpImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.summerb.validation.ValidationContextFactory;
import org.summerb.validation.ValidationContextFactoryImpl;

// @TestConfiguration
public class IntegrTestConfig {

  @Bean
  public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
    PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
    cfg.setIgnoreResourceNotFound(true);
    cfg.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    cfg.setNullValue("null");
    cfg.setLocations(
        new org.springframework.core.io.Resource[] {
          new ClassPathResource("default.properties"),
          new FileSystemResource("pgptool-gui-devmode.properties")
        });
    return cfg;
  }

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:pgptool-gui-messages", "classpath:summerb-messages");
    ms.setCacheSeconds(60);
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(true);
    return ms;
  }

  @Bean(destroyMethod = "shutdownNow")
  public ExecutorService executorService() {
    return Executors.newCachedThreadPool();
  }

  @Bean
  public Messages messages(ApplicationContext applicationContext) {
    return new Messages(applicationContext);
  }

  @Bean
  public EventBus eventBus() {
    return new EventBus();
  }

  @Bean
  public String tempDirPath() {
    return TestTools.buildNewTempDir();
  }

  @Bean
  public ConfigsBasePathResolver configsBasePathResolver(@Value("#{tempDirPath}") String tempDir) {
    // For tests use temp directory directly as configs base path
    return new ConfigsBasePathResolver() {
      @Override
      public String getConfigsBasePath() {
        java.io.File f = new java.io.File(tempDir);
        if (!f.exists()) {
          f.mkdirs();
        }
        return tempDir;
      }
    };
  }

  @Bean
  public ConfigRepositoryImpl configRepository(
      ConfigsBasePathResolver configsBasePathResolver, EventBus eventBus) {
    return new ConfigRepositoryImpl(configsBasePathResolver, eventBus);
  }

  @Bean
  public KeyFilesOperationsPgpImpl keyFilesOperations() {
    return new KeyFilesOperationsPgpImpl();
  }

  @Bean
  public KeyRingServicePgpImpl keyRingService(
      ConfigRepository configRepository,
      EventBus eventBus,
      KeyGeneratorService keyGeneratorService,
      UsageLogger usageLogger) {
    return new KeyRingServicePgpImpl(configRepository, eventBus, keyGeneratorService, usageLogger);
  }

  @Bean
  public EncryptionServicePgpImpl encryptionService() {
    return new EncryptionServicePgpImpl();
  }

  @Bean
  public ValidationContextFactory validationContextFactory() {
    return new ValidationContextFactoryImpl();
  }

  @Bean
  public KeyGeneratorServicePgpImpl keyGeneratorService(
      ExecutorService executorService,
      ValidationContextFactory validationContextFactory,
      @Value("${keygen.masterKey.algorithm}") String masterKeyAlgorithm,
      @Value("${keygen.masterKey.purpose}") String masterKeyPurpose,
      @Value("${keygen.masterKey.size}") int masterKeySize,
      @Value("${keygen.masterKey.signer.signerAlgorithm}") String masterKeySignerAlgorithm,
      @Value("${keygen.masterKey.signer.hashingAlgorithm}") String masterKeySignerHashingAlgorithm,
      @Value("${keygen.secretKey.hashingAlgorithm}") String secretKeyHashingAlgorithm,
      @Value("${keygen.secretKey.symmetricEncryptionAlgorithm}")
          String secretKeyEncryptionAlgorithm,
      @Value("${keygen.encryptionSubKey.algorithm}") String encryptionKeyAlgorithm,
      @Value("${keygen.encryptionSubKey.purpose}") String encryptionKeyPurpose,
      @Value("${keygen.encryptionSubKey.dhparams.primeModulus}") String primeModulusHex,
      @Value("${keygen.encryptionSubKey.dhparams.baseGenerator}") String baseGeneratorStr) {

    BigInteger dhPrime = new BigInteger(primeModulusHex, 16);
    BigInteger dhBase = new BigInteger(baseGeneratorStr, 16);

    return new KeyGeneratorServicePgpImpl(
        executorService,
        validationContextFactory,
        masterKeyAlgorithm,
        masterKeyPurpose,
        masterKeySize,
        masterKeySignerAlgorithm,
        masterKeySignerHashingAlgorithm,
        secretKeyHashingAlgorithm,
        secretKeyEncryptionAlgorithm,
        encryptionKeyAlgorithm,
        encryptionKeyPurpose,
        dhPrime,
        dhBase);
  }

  @Bean
  public UsageLoggerNoOpImpl usageLogger() {
    return new UsageLoggerNoOpImpl();
  }
}
