package org.pgptool.gui.app;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.google.common.eventbus.EventBus;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.pgptool.gui.autoupdate.api.NewVersionChecker;
import org.pgptool.gui.autoupdate.impl.NewVersionCheckerGitHubImpl;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.config.impl.ConfigRepositoryImpl;
import org.pgptool.gui.config.impl.ConfigsBasePathResolverUserHomeImpl;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.configpairs.impl.ConfigPairsImpl;
import org.pgptool.gui.decryptedlist.api.MonitoringDecryptedFilesService;
import org.pgptool.gui.decryptedlist.impl.MonitoringDecryptedFilesServiceImpl;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.implpgp.EncryptionServicePgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyFilesOperationsPgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyGeneratorServicePgpImpl;
import org.pgptool.gui.encryption.implpgp.KeyRingServicePgpImpl;
import org.pgptool.gui.encryptionparams.api.EncryptionParamsStorage;
import org.pgptool.gui.encryptionparams.impl.EncryptionParamsStorageImpl;
import org.pgptool.gui.filecomparison.MessageDigestFactory;
import org.pgptool.gui.filecomparison.MessageDigestFactoryImpl;
import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.hintsforusage.hints.BuyMeCoffeeHint;
import org.pgptool.gui.hintsforusage.hints.CreateOrImportPrivateKeyHint;
import org.pgptool.gui.hintsforusage.hints.MightNeedPublicKeysHint;
import org.pgptool.gui.hintsforusage.hints.PrivateKeyBackupHint;
import org.pgptool.gui.hintsforusage.impl.HintsCoordinatorImpl;
import org.pgptool.gui.hintsforusage.ui.HintPm;
import org.pgptool.gui.hintsforusage.ui.HintView;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgptool.gui.tempfolderfordecrypted.impl.DecryptedTempFolderImpl;
import org.pgptool.gui.ui.about.AboutPm;
import org.pgptool.gui.ui.about.AboutView;
import org.pgptool.gui.ui.changekeypassword.ChangeKeyPasswordPm;
import org.pgptool.gui.ui.changekeypassword.ChangeKeyPasswordView;
import org.pgptool.gui.ui.checkForUpdates.CheckForUpdatesPm;
import org.pgptool.gui.ui.checkForUpdates.CheckForUpdatesView;
import org.pgptool.gui.ui.checkForUpdates.UpdatesPolicy;
import org.pgptool.gui.ui.createkey.CreateKeyPm;
import org.pgptool.gui.ui.createkey.CreateKeyView;
import org.pgptool.gui.ui.decryptone.DecryptOnePm;
import org.pgptool.gui.ui.decryptone.DecryptOneView;
import org.pgptool.gui.ui.decryptonedialog.DecryptOneDialogPm;
import org.pgptool.gui.ui.decryptonedialog.DecryptOneDialogView;
import org.pgptool.gui.ui.decrypttext.DecryptTextPm;
import org.pgptool.gui.ui.decrypttext.DecryptTextView;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptBackMultiplePm;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptBackMultipleView;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptMultipleResultsDialogPm;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptMultipleResultsDialogView;
import org.pgptool.gui.ui.encryptone.EncryptOnePm;
import org.pgptool.gui.ui.encryptone.EncryptOneView;
import org.pgptool.gui.ui.encrypttext.EncryptTextPm;
import org.pgptool.gui.ui.encrypttext.EncryptTextView;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordManyKeysView;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordOneKeyView;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPm;
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogPm;
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogView;
import org.pgptool.gui.ui.historyquicksearch.HistoryQuickSearchPm;
import org.pgptool.gui.ui.historyquicksearch.HistoryQuickSearchView;
import org.pgptool.gui.ui.importkey.KeyImporterPm;
import org.pgptool.gui.ui.importkey.KeyImporterView;
import org.pgptool.gui.ui.keyslist.KeysExporterUi;
import org.pgptool.gui.ui.keyslist.KeysExporterUiImpl;
import org.pgptool.gui.ui.keyslist.KeysListPm;
import org.pgptool.gui.ui.keyslist.KeysListView;
import org.pgptool.gui.ui.keyslist.KeysTableView;
import org.pgptool.gui.ui.mainframe.MainFramePm;
import org.pgptool.gui.ui.mainframe.MainFrameView;
import org.pgptool.gui.ui.root.GlobalAppActions;
import org.pgptool.gui.ui.root.RootPm;
import org.pgptool.gui.ui.tempfolderfordecrypted.TempFolderChooserPm;
import org.pgptool.gui.ui.tools.geometrymemory.ConfigPairsMonitorsDependentImpl;
import org.pgptool.gui.usage.api.Usage;
import org.pgptool.gui.usage.api.UsageLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.summerb.methodCapturers.PropertyNameResolverFactory;
import org.summerb.validation.ValidationContextConfig;
import org.summerb.validation.ValidationContextFactory;

@Configuration
@Import({ValidationContextConfig.class})
@PropertySource("classpath:default.properties")
@PropertySource(value = "file:pgptool-gui-devmode.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:custom.properties", ignoreResourceNotFound = true)
@ComponentScan(basePackages = "org.pgptool.gui")
public class AppConfig {

  @Bean
  MessageSource messageSource() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames(
        "classpath:pgptool-gui-messages",
        "classpath:summerb-validation-messages",
        "classpath:summerb-messages",
        "classpath:service-users-messages",
        "classpath:security-messages");
    ms.setCacheSeconds(60);
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(true);
    return ms;
  }

  @Bean(destroyMethod = "shutdownNow")
  ExecutorService executorService() {
    return Executors.newScheduledThreadPool(4);
  }

  @Bean(destroyMethod = "shutdownNow")
  ScheduledExecutorService scheduledExecutorService() {
    return Executors.newScheduledThreadPool(4);
  }

  @Bean
  Messages messages(ApplicationContext applicationContext) {
    return new Messages(applicationContext);
  }

  @Bean
  EntryPoint entryPoint() {
    return new EntryPoint();
  }

  @Bean
  EventBus eventBus() {
    return new EventBus();
  }

  @Bean
  ConfigsBasePathResolver configsBasePathResolver(
      @Value("${configs.folder-name}") String configFolderName,
      @Value("${configs.custom-base-path}") String customConfigBasePath) {
    return new ConfigsBasePathResolverUserHomeImpl(configFolderName, customConfigBasePath);
  }

  @Bean
  ConfigRepository configRepository(
      ConfigsBasePathResolver configsBasePathResolver, EventBus eventBus) {
    return new ConfigRepositoryImpl(configsBasePathResolver, eventBus);
  }

  @Bean
  ConfigPairs appProps(ConfigRepository configRepository, EventBus eventBus) {
    return new ConfigPairsImpl(configRepository, eventBus, "app-props");
  }

  @Bean
  ConfigPairs encryptionParams(ConfigRepository configRepository, EventBus eventBus) {
    return new ConfigPairsImpl(configRepository, eventBus, "encr-params");
  }

  @Bean
  ConfigPairs decryptionParams(ConfigRepository configRepository, EventBus eventBus) {
    return new ConfigPairsImpl(configRepository, eventBus, "decr-params");
  }

  @Bean
  ConfigPairs monitoredDecrypted(ConfigRepository configRepository, EventBus eventBus) {
    return new ConfigPairsImpl(configRepository, eventBus, "mon-decr");
  }

  @Bean
  ConfigPairs hintsProps(ConfigRepository configRepository, EventBus eventBus) {
    return new ConfigPairsImpl(configRepository, eventBus, "hints");
  }

  @Bean
  ConfigPairs uiGeom(ConfigRepository configRepository, EventBus eventBus) {
    return new ConfigPairsMonitorsDependentImpl(
        new ConfigPairsImpl(configRepository, eventBus, "uipos"));
  }

  // Misc services
  @Bean
  MessageDigestFactory messageDigestFactory() {
    return new MessageDigestFactoryImpl();
  }

  @Bean
  DecryptedTempFolder decryptedTempFolder(
      ConfigsBasePathResolver configsBasePathResolver, ConfigPairs appProps, RootPm rootPm) {
    return new DecryptedTempFolderImpl(configsBasePathResolver, appProps, rootPm);
  }

  @Bean
  EncryptionParamsStorage encryptionParamsStorage(ConfigPairs encryptionParams) {
    return new EncryptionParamsStorageImpl(encryptionParams);
  }

  // PGP services
  @Bean
  KeyFilesOperations keyFilesOperations(PropertyNameResolverFactory propertyNameResolverFactory) {
    return new KeyFilesOperationsPgpImpl(propertyNameResolverFactory);
  }

  @Bean
  KeyRingService keyRingService(
      ConfigRepository configRepository, EventBus eventBus, UsageLogger usageLogger) {
    return new KeyRingServicePgpImpl(configRepository, eventBus, usageLogger);
  }

  @Bean
  EncryptionService encryptionService() {
    return new EncryptionServicePgpImpl();
  }

  @Bean
  KeyGeneratorService keyGeneratorService(ValidationContextFactory validationContextFactory) {
    return new KeyGeneratorServicePgpImpl(validationContextFactory);
  }

  @Bean
  MonitoringDecryptedFilesService monitoringDecryptedFilesService(ConfigPairs monitoredDecrypted) {
    return new MonitoringDecryptedFilesServiceImpl(monitoredDecrypted);
  }

  // Views and PMs
  @Bean
  RootPm rootPm(
      EntryPoint entryPoint,
      ApplicationContext applicationContext,
      EventBus eventBus,
      UpdatesPolicy updatesPolicy,
      HintsCoordinator hintsCoordinator,
      KeysExporterUi keysExporterUi,
      KeyFilesOperations keyFilesOperations,
      UsageLogger usageLogger,
      BuyMeCoffeeHint buyMeCoffeeHint) {
    return new RootPm(
        entryPoint,
        applicationContext,
        eventBus,
        updatesPolicy,
        hintsCoordinator,
        keysExporterUi,
        keyFilesOperations,
        usageLogger,
        buyMeCoffeeHint);
  }

  @Bean
  @Scope(value = SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.NO)
  MainFramePm mainFramePm(
      EventBus eventBus,
      MonitoringDecryptedFilesService monitoringDecryptedFilesService,
      DecryptedTempFolder decryptedTempFolder,
      HistoryQuickSearchPm historyQuickSearchPm,
      ApplicationContext applicationContext) {
    return new MainFramePm(
        eventBus,
        monitoringDecryptedFilesService,
        decryptedTempFolder,
        historyQuickSearchPm,
        applicationContext);
  }

  @Bean
  @Scope(value = SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.NO)
  MainFrameView mainFrameView(
      HintView hintView, ScheduledExecutorService scheduledExecutorService, ConfigPairs uiGeom) {
    return new MainFrameView(hintView, scheduledExecutorService, uiGeom);
  }

  @Bean
  HintsCoordinator hintsCoordinator(EventBus eventBus) {
    return new HintsCoordinatorImpl(eventBus);
  }

  @Bean
  CreateOrImportPrivateKeyHint createOrImportPrivateKeyHint(
      ConfigPairs hintsProps,
      HintsCoordinator hintsCoordinator,
      EventBus eventBus,
      KeyRingService keyRingService,
      GlobalAppActions globalAppActions,
      ExecutorService executorService) {
    return new CreateOrImportPrivateKeyHint(
        hintsProps, hintsCoordinator, eventBus, keyRingService, globalAppActions, executorService);
  }

  @Bean
  MightNeedPublicKeysHint mightNeedPublicKeysHint(
      ConfigPairs hintsProps,
      HintsCoordinator hintsCoordinator,
      EventBus eventBus,
      KeyRingService keyRingService,
      GlobalAppActions globalAppActions,
      ExecutorService executorService) {
    return new MightNeedPublicKeysHint(
        hintsProps, hintsCoordinator, eventBus, keyRingService, globalAppActions, executorService);
  }

  @Bean
  PrivateKeyBackupHint privateKeyBackupHint(
      ConfigPairs hintsProps,
      HintsCoordinator hintsCoordinator,
      EventBus eventBus,
      KeyRingService keyRingService,
      GlobalAppActions globalAppActions,
      ExecutorService executorService) {
    return new PrivateKeyBackupHint(
        hintsProps, hintsCoordinator, eventBus, keyRingService, globalAppActions, executorService);
  }

  @Bean
  BuyMeCoffeeHint buyMeCoffeeHint(
      ConfigPairs hintsProps,
      ConfigPairs encryptionParams,
      ConfigPairs decryptionParams,
      HintsCoordinator hintsCoordinator,
      KeyRingService keyRingService,
      ExecutorService executorService) {
    return new BuyMeCoffeeHint(
        hintsProps,
        encryptionParams,
        decryptionParams,
        hintsCoordinator,
        keyRingService,
        executorService);
  }

  @Bean
  HintPm hintPm() {
    return new HintPm();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  HintView hintView() {
    return new HintView();
  }

  @Bean
  AboutView aboutView() {
    return new AboutView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  AboutPm aboutPm(NewVersionChecker newVersionChecker) {
    return new AboutPm(newVersionChecker);
  }

  @Bean
  KeyImporterView keyImporterView(KeysTableView keysTableView) {
    return new KeyImporterView(keysTableView);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  KeyImporterPm keyImporterPm(
      ConfigPairs appProps, KeyFilesOperations keyFilesOperations, KeyRingService keyRingService) {
    return new KeyImporterPm(appProps, keyFilesOperations, keyRingService);
  }

  @Bean
  KeysListView keysListView(KeysTableView keysTableView) {
    return new KeysListView(keysTableView);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  KeysListPm keysListPm(
      EventBus eventBus,
      KeyRingService keyRingService,
      KeysExporterUi keysExporterUi,
      KeyFilesOperations keyFilesOperations) {
    return new KeysListPm(eventBus, keyRingService, keysExporterUi, keyFilesOperations);
  }

  @Bean
  KeysExporterUi keysExporterUi(
      KeyFilesOperations keyFilesOperations,
      EventBus eventBus,
      ConfigPairs appProps,
      UsageLogger usageLogger) {
    return new KeysExporterUiImpl(keyFilesOperations, eventBus, appProps, usageLogger);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  EncryptOnePm encryptOnePm(
      ConfigPairs appProps,
      EncryptionParamsStorage encryptionParamsStorage,
      MessageDigestFactory messageDigestFactory,
      MonitoringDecryptedFilesService monitoringDecryptedFilesService,
      KeyRingService keyRingService,
      EncryptionService encryptionService) {
    return new EncryptOnePm(
        appProps,
        encryptionParamsStorage,
        messageDigestFactory,
        monitoringDecryptedFilesService,
        keyRingService,
        encryptionService);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  EncryptOneView encryptOneView() {
    return new EncryptOneView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  DecryptOnePm decryptOnePm(
      ConfigPairs appProps,
      ConfigPairs decryptionParams,
      ExecutorService executorService,
      EncryptionParamsStorage encryptionParamsStorage,
      DecryptedTempFolder decryptedTempFolder,
      KeyRingService keyRingService,
      EncryptionService encryptionService,
      MonitoringDecryptedFilesService monitoringDecryptedFilesService,
      MessageDigestFactory messageDigestFactory) {
    return new DecryptOnePm(
        appProps,
        decryptionParams,
        executorService,
        encryptionParamsStorage,
        decryptedTempFolder,
        keyRingService,
        encryptionService,
        monitoringDecryptedFilesService,
        messageDigestFactory);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  DecryptOneView decryptOneView() {
    return new DecryptOneView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  DecryptOneDialogPm decryptOneDialogPm(
      DecryptOnePm decryptOnePm, ApplicationContext applicationContext) {
    return new DecryptOneDialogPm(decryptOnePm, applicationContext);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  DecryptOneDialogView decryptOneDialogView(ApplicationContext applicationContext) {
    return new DecryptOneDialogView(applicationContext);
  }

  @Bean
  TempFolderChooserPm tempFolderChooserPm(DecryptedTempFolder decryptedTempFolder) {
    return new TempFolderChooserPm(decryptedTempFolder);
  }

  @Bean
  CreateKeyView createKeyView() {
    return new CreateKeyView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  CreateKeyPm createKeyPm(
      KeyRingService keyRingService,
      KeyGeneratorService keyGeneratorService,
      ExecutorService executorService,
      EventBus eventBus,
      UsageLogger usageLogger,
      PropertyNameResolverFactory propertyNameResolverFactory) {
    return new CreateKeyPm(
        keyRingService,
        keyGeneratorService,
        executorService,
        eventBus,
        usageLogger,
        propertyNameResolverFactory);
  }

  @Bean
  ChangeKeyPasswordView changeKeyPasswordView() {
    return new ChangeKeyPasswordView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  ChangeKeyPasswordPm changeKeyPasswordPm(
      KeyRingService keyRingService,
      KeyGeneratorService keyGeneratorService,
      KeyFilesOperations keyFilesOperations,
      PropertyNameResolverFactory propertyNameResolverFactory) {
    return new ChangeKeyPasswordPm(
        keyRingService, keyGeneratorService, keyFilesOperations, propertyNameResolverFactory);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  KeysTableView keysTableView(
      ScheduledExecutorService scheduledExecutorService, ConfigPairs uiGeom) {
    return new KeysTableView(scheduledExecutorService, uiGeom);
  }

  @Bean
  EncryptBackMultipleView encryptBackMultipleView() {
    return new EncryptBackMultipleView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  EncryptBackMultiplePm encryptBackMultiplePm(
      EncryptionParamsStorage encryptionParamsStorage,
      ConfigPairs appProps,
      KeyRingService keyRingService,
      EncryptionService encryptionService,
      MonitoringDecryptedFilesService monitoringDecryptedFilesService,
      MessageDigestFactory messageDigestFactory,
      UsageLogger usageLogger,
      ApplicationContext applicationContext) {
    return new EncryptBackMultiplePm(
        encryptionParamsStorage,
        appProps,
        keyRingService,
        encryptionService,
        monitoringDecryptedFilesService,
        messageDigestFactory,
        usageLogger,
        applicationContext);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  EncryptMultipleResultsDialogPm encryptMultipleResultsDialogPm() {
    return new EncryptMultipleResultsDialogPm();
  }

  @Bean
  EncryptMultipleResultsDialogView encryptMultipleResultsDialogView() {
    return new EncryptMultipleResultsDialogView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  GetKeyPasswordPm getKeyPasswordPm(
      KeyRingService keyRingService,
      KeyFilesOperations keyFilesOperations,
      EventBus eventBus,
      UsageLogger usageLogger,
      PropertyNameResolverFactory propertyNameResolverFactory) {
    return new GetKeyPasswordPm(
        keyRingService, keyFilesOperations, eventBus, usageLogger, propertyNameResolverFactory);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  GetKeyPasswordManyKeysView getKeyPasswordManyKeysView() {
    return new GetKeyPasswordManyKeysView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  GetKeyPasswordOneKeyView getKeyPasswordOneKeyView() {
    return new GetKeyPasswordOneKeyView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  EncryptTextPm encryptTextPm(
      KeyRingService keyRingService, EncryptionService encryptionService, UsageLogger usageLogger) {
    return new EncryptTextPm(keyRingService, encryptionService, usageLogger);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  EncryptTextView encryptTextView() {
    return new EncryptTextView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  DecryptTextPm decryptTextPm(
      KeyRingService keyRingService, EncryptionService encryptionService, UsageLogger usageLogger) {
    return new DecryptTextPm(keyRingService, encryptionService, usageLogger);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  DecryptTextView decryptTextView() {
    return new DecryptTextView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  GetKeyPasswordDialogPm getKeyPasswordDialogPm(ApplicationContext applicationContext) {
    return new GetKeyPasswordDialogPm(applicationContext);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  GetKeyPasswordDialogView getKeyPasswordDialogView(ApplicationContext applicationContext) {
    return new GetKeyPasswordDialogView(applicationContext);
  }

  @Bean
  CheckForUpdatesPm checkForUpdatesPm(NewVersionChecker newVersionChecker) {
    return new CheckForUpdatesPm(newVersionChecker);
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  CheckForUpdatesView checkForUpdatesView() {
    return new CheckForUpdatesView();
  }

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  HistoryQuickSearchPm historyQuickSearchPm(
      EventBus eventBus, ExecutorService executorService, ConfigPairs decryptionParams) {
    return new HistoryQuickSearchPm(eventBus, executorService, decryptionParams);
  }

  @Bean
  HistoryQuickSearchView historyQuickSearchView(
      ScheduledExecutorService scheduledExecutorService, ConfigPairs uiGeom) {
    return new HistoryQuickSearchView(scheduledExecutorService, uiGeom);
  }

  @Bean
  NewVersionChecker newVersionCheckerGitHub(@Value("${app-version}") String appVersion) {
    NewVersionCheckerGitHubImpl b = new NewVersionCheckerGitHubImpl();
    b.setAppVersion(appVersion);
    return b;
  }

  @Bean
  UpdatesPolicy updatesPolicy(ConfigPairs appProps, ApplicationContext applicationContext) {
    return new UpdatesPolicy(appProps, applicationContext);
  }

  @Bean
  Usage usage() {
    return new Usage();
  }
}
