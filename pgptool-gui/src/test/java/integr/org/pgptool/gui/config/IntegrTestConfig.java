package integr.org.pgptool.gui.config;

import com.google.common.eventbus.EventBus;
import integr.org.pgptool.gui.TestTools;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.mockito.Mockito;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.config.impl.ConfigRepositoryImpl;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.configpairs.impl.ConfigPairsImpl;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.implpgp.EncryptionServicePgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyFilesOperationsPgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyGeneratorServicePgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyRingServicePgpImpl;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgptool.gui.tempfolderfordecrypted.impl.DecryptedTempFolderImpl;
import org.pgptool.gui.ui.root.RootPm;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.api.UsageLoggerNoOpImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.summerb.methodCapturers.PropertyNameResolverFactory;
import org.summerb.validation.ValidationContextConfig;
import org.summerb.validation.ValidationContextFactory;

@Configuration
@Import({ValidationContextConfig.class})
public class IntegrTestConfig {

  @Bean
  static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
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
  MessageSource messageSource() {
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
  Messages messages(ApplicationContext applicationContext) {
    return new Messages(applicationContext);
  }

  @Bean
  EventBus eventBus() {
    return new EventBus();
  }

  @Bean
  String tempDirPath() {
    return TestTools.buildNewTempDir();
  }

  @Bean
  ConfigPairsImpl appProps(ConfigRepository configRepository, EventBus eventBus) {
    return new ConfigPairsImpl(configRepository, eventBus, "app-props");
  }

  @Bean
  DecryptedTempFolder decryptedTempFolder(
      ConfigsBasePathResolver configsBasePathResolver, ConfigPairs appProps) {
    return new DecryptedTempFolderImpl(
        configsBasePathResolver, appProps, Mockito.mock(RootPm.class));
  }

  @Bean
  ConfigsBasePathResolver configsBasePathResolver(@Value("#{tempDirPath}") String tempDir) {
    // For tests use temp directory directly as configs base path
    return () -> {
      java.io.File f = new java.io.File(tempDir);
      if (!f.exists()) {
        f.mkdirs();
      }
      return tempDir;
    };
  }

  @Bean
  ConfigRepositoryImpl configRepository(
      ConfigsBasePathResolver configsBasePathResolver, EventBus eventBus) {
    return new ConfigRepositoryImpl(configsBasePathResolver, eventBus);
  }

  @Bean
  KeyFilesOperationsPgpImpl keyFilesOperations(
      PropertyNameResolverFactory propertyNameResolverFactory) {
    return new KeyFilesOperationsPgpImpl(propertyNameResolverFactory);
  }

  @Bean
  KeyRingServicePgpImpl keyRingService(
      ConfigRepository configRepository,
      EventBus eventBus,
      KeyGeneratorService keyGeneratorService,
      UsageLogger usageLogger) {
    return new KeyRingServicePgpImpl(configRepository, eventBus, keyGeneratorService, usageLogger);
  }

  @Bean
  EncryptionServicePgpImpl encryptionService() {
    return new EncryptionServicePgpImpl();
  }

  @Bean
  KeyGeneratorServicePgpImpl keyGeneratorService(
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
  UsageLoggerNoOpImpl usageLogger() {
    return new UsageLoggerNoOpImpl();
  }
}
