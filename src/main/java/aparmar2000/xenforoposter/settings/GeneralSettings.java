package aparmar2000.xenforoposter.settings;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import aparmar2000.xenforoposter.extension.setting.BooleanSettingDefinition;
import aparmar2000.xenforoposter.extension.setting.ChoiceSettingDefinition;
import aparmar2000.xenforoposter.extension.setting.IntegerSettingDefinition;
import aparmar2000.xenforoposter.extension.setting.StringSettingDefinition;
import aparmar2000.xenforoposter.utils.GsonSupplier;
import lombok.Getter;

/**
 * Application-wide settings extending {@link SettingsHolder}.
 */
public class GeneralSettings extends SettingsHolder {

    // Setting Definitions
    @Getter private final IntegerSettingDefinition pollIntervalSetting;
    @Getter private final BooleanSettingDefinition autoStartEngineSetting;
    @Getter private final IntegerSettingDefinition minGlobalPostIntervalSetting;
    @Getter private final IntegerSettingDefinition minThreadPostIntervalSetting;
    @Getter private final IntegerSettingDefinition maxPostsPerHourSetting;
    @Getter private final IntegerSettingDefinition maxPostsPerDaySetting;
    @Getter private final StringSettingDefinition defaultUserAgentSetting;
    @Getter private final IntegerSettingDefinition requestTimeoutSetting;
    @Getter private final ChoiceSettingDefinition appThemeSetting;

    // Memoized Value Suppliers
    private final Supplier<Integer> pollIntervalSupplier;
    private final Supplier<Boolean> autoStartEngineSupplier;
    private final Supplier<Integer> minGlobalPostIntervalSupplier;
    private final Supplier<Integer> minThreadPostIntervalSupplier;
    private final Supplier<Integer> maxPostsPerHourSupplier;
    private final Supplier<Integer> maxPostsPerDaySupplier;
    private final Supplier<String> defaultUserAgentSupplier;
    private final Supplier<Integer> requestTimeoutSupplier;
    private final Supplier<String> appThemeSupplier;

    public GeneralSettings() {
        this(null, (Gson) null);
    }

    public GeneralSettings(@Nullable Path settingsFile) {
        this(settingsFile, (Gson) null);
    }

    public GeneralSettings(@NotNull GsonSupplier gsonSupplier) {
        this(null, gsonSupplier.get());
    }

    public GeneralSettings(@NotNull Gson gson) {
        this(null, gson);
    }

    public GeneralSettings(@Nullable Path settingsFile, @NotNull GsonSupplier gsonSupplier) {
        this(settingsFile, gsonSupplier.get());
    }

    @Inject
    public GeneralSettings(@Named("settingsFile") @Nullable Path settingsFile,
                           @Nullable Gson gson) {
        super(settingsFile, gson);

        // --- Scheduler Engine
        pollIntervalSetting = new IntegerSettingDefinition(
                "scheduler.poll_interval", "Polling Interval (seconds)", "How often the engine evaluates pending post conditions",
                3, 1, 60, "Scheduler Engine"
        );
        autoStartEngineSetting = new BooleanSettingDefinition(
                "scheduler.auto_start", "Auto-start Engine on Launch", "Whether the scheduler starts running immediately on application launch",
                true, "Scheduler Engine"
        );

        // --- Safety & Rate Limits
        minGlobalPostIntervalSetting = new IntegerSettingDefinition(
                "rate_limit.global_interval", "Global Post Cooldown (seconds)", "Minimum delay between any two outgoing posts across all forums",
                60, 10, 3600, "Safety & Rate Limits"
        );
        minThreadPostIntervalSetting = new IntegerSettingDefinition(
                "rate_limit.thread_interval", "Thread Post Cooldown (seconds)", "Minimum delay between consecutive posts to the same thread",
                300, 30, 3600, "Safety & Rate Limits"
        );
        maxPostsPerHourSetting = new IntegerSettingDefinition(
                "rate_limit.max_per_hour", "Max Posts Per Hour", "Maximum number of dispatched posts allowed within any 60 minute window",
                12, 1, 100, "Safety & Rate Limits"
        );
        maxPostsPerDaySetting = new IntegerSettingDefinition(
                "rate_limit.max_per_day", "Max Posts Per Day", "Maximum number of dispatched posts allowed within a 24-hour period",
                60, 1, 500, "Safety & Rate Limits"
        );

        // --- Web & Network
        defaultUserAgentSetting = new StringSettingDefinition(
                "network.user_agent", "Default User-Agent", "HTTP User-Agent string sent to XenForo instances",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Web & Network"
        );
        requestTimeoutSetting = new IntegerSettingDefinition(
                "network.timeout_seconds", "HTTP Request Timeout (seconds)", "Connection and read timeout for forum requests",
                15, 5, 120, "Web & Network"
        );

        // --- Appearance
        appThemeSetting = new ChoiceSettingDefinition(
                "ui.theme", "Theme Mode", "Visual theme for the application interface",
                "Dark", List.of("Dark", "Light"), "Appearance"
        );


        pollIntervalSupplier = register(pollIntervalSetting);
        autoStartEngineSupplier = register(autoStartEngineSetting);
        minGlobalPostIntervalSupplier = register(minGlobalPostIntervalSetting);
        minThreadPostIntervalSupplier = register(minThreadPostIntervalSetting);
        maxPostsPerHourSupplier = register(maxPostsPerHourSetting);
        maxPostsPerDaySupplier = register(maxPostsPerDaySetting);
        defaultUserAgentSupplier = register(defaultUserAgentSetting);
        requestTimeoutSupplier = register(requestTimeoutSetting);
        appThemeSupplier = register(appThemeSetting);

        if (settingsFile != null) {
            load();
        }
    }

    // --- Value Getters

    public int getPollIntervalSeconds() {
        return pollIntervalSupplier.get();
    }

    public boolean isAutoStartEngine() {
        return autoStartEngineSupplier.get();
    }

    public int getMinGlobalPostIntervalSeconds() {
        return minGlobalPostIntervalSupplier.get();
    }

    public int getMinThreadPostIntervalSeconds() {
        return minThreadPostIntervalSupplier.get();
    }

    public int getMaxPostsPerHour() {
        return maxPostsPerHourSupplier.get();
    }

    public int getMaxPostsPerDay() {
        return maxPostsPerDaySupplier.get();
    }

    public String getDefaultUserAgent() {
        return defaultUserAgentSupplier.get();
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSupplier.get();
    }

    public String getAppTheme() {
        return appThemeSupplier.get();
    }
}
