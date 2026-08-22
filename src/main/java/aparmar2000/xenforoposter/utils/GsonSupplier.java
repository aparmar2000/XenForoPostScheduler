package aparmar2000.xenforoposter.utils;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import aparmar2000.xenforoposter.model.conditions.PostCondition;
import aparmar2000.xenforoposter.security.CredentialEncryptionService;
import aparmar2000.xenforoposter.security.SecureString;
import aparmar2000.xenforoposter.security.SecureStringTypeAdapter;

@Singleton
public class GsonSupplier implements Provider<Gson>, Supplier<Gson> {
	private final CredentialEncryptionService encryptionService;

	@Inject
	public GsonSupplier(@NotNull CredentialEncryptionService encryptionService) {
		this.encryptionService = encryptionService;
	}

	public @NotNull GsonBuilder createBuilder() {
		return new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.registerTypeAdapter(Instant.class, new InstantAdapter())
				.registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
				.registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
				.registerTypeHierarchyAdapter(ZoneId.class, new ZoneIdAdapter())
				.registerTypeAdapter(PostCondition.class, new PostConditionAdapter())
				.registerTypeAdapter(SecureString.class, new SecureStringTypeAdapter(encryptionService));
	}
	public @NotNull Gson createGson() {
		return createBuilder().create();
	}

	@Override
	public Gson get() {
		return createGson();
	}

	private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
		@Override
		public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
			return src != null ? new JsonPrimitive(src.toString()) : JsonNull.INSTANCE;
		}

		@Override
		public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json == null || json.isJsonNull()) {
				return null;
			}
			return Instant.parse(json.getAsString());
		}
	}

	private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
		@Override
		public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
			return src != null ? new JsonPrimitive(src.toString()) : JsonNull.INSTANCE;
		}

		@Override
		public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json == null || json.isJsonNull()) {
				return null;
			}
			return LocalDate.parse(json.getAsString());
		}
	}

	private static class LocalTimeAdapter implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
		@Override
		public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
			return src != null ? new JsonPrimitive(src.toString()) : JsonNull.INSTANCE;
		}

		@Override
		public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json == null || json.isJsonNull()) {
				return null;
			}
			return LocalTime.parse(json.getAsString());
		}
	}

	private static class ZoneIdAdapter implements JsonSerializer<ZoneId>, JsonDeserializer<ZoneId> {
		@Override
		public JsonElement serialize(ZoneId src, Type typeOfSrc, JsonSerializationContext context) {
			return src != null ? new JsonPrimitive(src.getId()) : JsonNull.INSTANCE;
		}

		@Override
		public ZoneId deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json == null || json.isJsonNull()) {
				return null;
			}
			return ZoneId.of(json.getAsString());
		}
	}

	private static class PostConditionAdapter implements JsonSerializer<PostCondition>, JsonDeserializer<PostCondition> {
		private static final String TYPE_FIELD = "conditionClass";

		@Override
		public JsonElement serialize(PostCondition src, Type typeOfSrc, JsonSerializationContext context) {
			if (src == null) {
				return JsonNull.INSTANCE;
			}
			JsonObject obj = context.serialize(src, src.getClass()).getAsJsonObject();
			obj.addProperty(TYPE_FIELD, src.getClass().getName());
			return obj;
		}

		@Override
		public PostCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json == null || json.isJsonNull()) {
				return null;
			}
			JsonObject obj = json.getAsJsonObject();
			JsonElement typeEl = obj.get(TYPE_FIELD);
			if (typeEl == null) {
				throw new JsonParseException("Missing " + TYPE_FIELD + " in condition payload");
			}
			try {
				Class<?> clazz = Class.forName(typeEl.getAsString());
				return context.deserialize(obj, clazz);
			} catch (ClassNotFoundException e) {
				throw new JsonParseException("Unknown condition class: " + typeEl.getAsString(), e);
			}
		}
	}
}
