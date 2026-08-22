package aparmar2000.xenforoposter.security;

import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Gson adapter for serializing and deserializing {@link SecureString} instances
 * as encrypted ciphertext tokens via {@link CredentialEncryptionService}.
 */
public class SecureStringTypeAdapter implements JsonSerializer<SecureString>, JsonDeserializer<SecureString> {
	private final CredentialEncryptionService encryptionService;

	public SecureStringTypeAdapter(@NotNull CredentialEncryptionService encryptionService) {
		this.encryptionService = encryptionService;
	}

	@Override
	public JsonElement serialize(SecureString src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null || src.getClearText() == null) {
			return JsonNull.INSTANCE;
		}
		String encrypted = encryptionService.encrypt(src.getClearText());
		return new JsonPrimitive(encrypted);
	}

	@Override
	public SecureString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if (json == null || json.isJsonNull()) {
			return null;
		}

		try {
			String cipherText = json.getAsString();
			String decrypted = encryptionService.decrypt(cipherText);
			return SecureString.of(decrypted);
		} catch (Exception e) {
			throw new JsonParseException("Failed to decrypt SecureString payload", e);
		}
	}
}
