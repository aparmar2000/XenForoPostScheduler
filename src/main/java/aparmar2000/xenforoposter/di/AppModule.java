package aparmar2000.xenforoposter.di;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.InternalExtensionContext;
import aparmar2000.xenforoposter.scheduler.JobStorageService;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import aparmar2000.xenforoposter.security.CredentialEncryptionService;
import aparmar2000.xenforoposter.security.SafetyRateLimiter;
import aparmar2000.xenforoposter.settings.GeneralSettings;
import aparmar2000.xenforoposter.settings.SettingsHolder;
import aparmar2000.xenforoposter.ui.MainFrame;
import aparmar2000.xenforoposter.utils.GsonSupplier;
import aparmar2000.xenforoposter.web.XenForoWebClient;

/**
 * Guice configuration module for injecting shared dependencies, services, and UI components.
 */
public class AppModule extends AbstractModule {
    private final Path baseDataDir;

    public AppModule() {
        this(Paths.get(System.getProperty("user.home"), ".xenforo-post-scheduler"));
    }

    @Deprecated
    @ApiStatus.Internal
    public AppModule(@NotNull Path baseDataDir) {
        this.baseDataDir = baseDataDir;
    }

    @Override
    protected void configure() {
        bind(Path.class)
        	.annotatedWith(Names.named("baseDataDir"))
        	.toInstance(baseDataDir);
        bind(Path.class)
        	.annotatedWith(Names.named("credentialsKeyFile"))
        	.toInstance(baseDataDir.resolve(".credentials.key"));
        bind(Path.class)
        	.annotatedWith(Names.named("settingsFile"))
        	.toInstance(baseDataDir.resolve("general_settings.json"));
        

		install(new FactoryModuleBuilder()
		     .implement(SettingsHolder.class, SettingsHolder.class)
		     .build(SettingsHolder.Factory.class));
		install(new FactoryModuleBuilder()
		     .implement(InternalExtensionContext.class, InternalExtensionContext.class)
		     .build(InternalExtensionContext.Factory.class));
    }

    @Provides
    @Singleton
    public CredentialEncryptionService provideCredentialEncryptionService(@Named("credentialsKeyFile") Path keyFile) {
        return new CredentialEncryptionService(keyFile);
    }

    @Provides
    @Singleton
    public GsonSupplier provideGsonSupplier(CredentialEncryptionService encryptionService) {
        return new GsonSupplier(encryptionService);
    }

    @Provides
    @Singleton
    public Gson provideGson(GsonSupplier gsonSupplier) {
        return gsonSupplier.createGson();
    }

    @Provides
    public GsonBuilder provideGsonBuilder(GsonSupplier gsonSupplier) {
        return gsonSupplier.createBuilder();
    }

    @Provides
    @Singleton
    public SafetyRateLimiter provideSafetyRateLimiter() {
        return new SafetyRateLimiter();
    }

    @Provides
    @Singleton
    public XenForoWebClient provideXenForoWebClient() {
        return new XenForoWebClient();
    }

    @Provides
    @Singleton
    public GeneralSettings provideGeneralSettings(@Named("settingsFile") Path settingsFile,
                                                  Gson gson) {
        return new GeneralSettings(settingsFile, gson);
    }

    @Provides
    @Singleton
    public JobStorageService provideJobStorageService(@Named("baseDataDir") Path baseDataDir,
                                                     CredentialEncryptionService encryptionService,
                                                     Gson gson) {
        return new JobStorageService(baseDataDir, encryptionService, gson);
    }

    @Provides
    @Singleton
    public ExtensionManager provideExtensionManager(@Named("baseDataDir") Path baseDataDir,
                                                    Gson gson,
                                                    InternalExtensionContext.Factory contextFactory) {
        return new ExtensionManager(baseDataDir, gson, contextFactory);
    }

    @Provides
    @Singleton
    public SchedulerEngine provideSchedulerEngine(SafetyRateLimiter rateLimiter,
                                                  JobStorageService storageService,
                                                  XenForoWebClient webClient) {
        return new SchedulerEngine(rateLimiter, storageService, webClient);
    }

    @Provides
    @Singleton
    public MainFrame provideMainFrame(SchedulerEngine schedulerEngine,
                                      ExtensionManager extensionManager,
                                      XenForoWebClient webClient,
                                      GeneralSettings generalSettings) {
        return new MainFrame(schedulerEngine, extensionManager, webClient, generalSettings);
    }
}
