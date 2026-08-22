package aparmar2000.xenforoposter.scheduler;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.security.CredentialEncryptionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobStorageService {
	private final Path storageDir;
	private final Path jobsFile;
	private final Path profilesFile;
	@Getter
	private final CredentialEncryptionService encryptionService;
	private final Gson gson;

	@Inject
	public JobStorageService(@Named("baseDataDir") @NotNull Path baseDir,
			@NotNull CredentialEncryptionService encryptionService,
			@NotNull Gson gson) {
		this.storageDir = baseDir;
		this.jobsFile = baseDir.resolve("scheduled_jobs.json");
		this.profilesFile = baseDir.resolve("forum_profiles.json");
		this.encryptionService = encryptionService;
		this.gson = gson;

		try {
			Files.createDirectories(storageDir);
		} catch (Exception e) {
			log.error("Failed to create storage directory {}", storageDir, e);
		}
	}

	public synchronized void saveJobs(@NotNull List<ScheduledJob> jobs) {
		try (Writer writer = Files.newBufferedWriter(jobsFile, StandardCharsets.UTF_8)) {
			gson.toJson(jobs, writer);
		} catch (Exception e) {
			log.error("Failed to save scheduled jobs to {}", jobsFile, e);
		}
	}

	public synchronized List<ScheduledJob> loadJobs() {
		if (!Files.exists(jobsFile)) {
			return new ArrayList<>();
		}
		try (Reader reader = Files.newBufferedReader(jobsFile, StandardCharsets.UTF_8)) {
			Type listType = new TypeToken<List<ScheduledJob>>() {}.getType();
			List<ScheduledJob> list = gson.fromJson(reader, listType);
			return list != null ? new ArrayList<>(list) : new ArrayList<>();
		} catch (Exception e) {
			log.error("Failed to load scheduled jobs from {}", jobsFile, e);
			return new ArrayList<>();
		}
	}

	public synchronized void saveProfiles(@NotNull List<ForumProfile> profiles) {
		try (Writer writer = Files.newBufferedWriter(profilesFile, StandardCharsets.UTF_8)) {
			gson.toJson(profiles, writer);
		} catch (Exception e) {
			log.error("Failed to save forum profiles to {}", profilesFile, e);
		}
	}

	public synchronized List<ForumProfile> loadProfiles() {
		if (!Files.exists(profilesFile)) {
			return new ArrayList<>();
		}
		try (Reader reader = Files.newBufferedReader(profilesFile, StandardCharsets.UTF_8)) {
			Type listType = new TypeToken<List<ForumProfile>>() {}.getType();
			List<ForumProfile> list = gson.fromJson(reader, listType);
			return list != null ? new ArrayList<>(list) : new ArrayList<>();
		} catch (Exception e) {
			log.error("Failed to load forum profiles from {}", profilesFile, e);
			return new ArrayList<>();
		}
	}
}
