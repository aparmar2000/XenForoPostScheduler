package aparmar2000.xenforoposter.model.conditions;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
public class DayOfWeekCondition extends PostCondition {
	private static final Set<DayOfWeek> WEEKDAYS = EnumSet.of(
			DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
	private static final Set<DayOfWeek> WEEKENDS = EnumSet.of(
			DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

	@NotNull @Builder.Default String id = UUID.randomUUID().toString();
	@NotNull @Singular Set<DayOfWeek> allowedDays;
	@Nullable ZoneId zoneId;

	@Override
	public @NotNull String getDisplayName() {
		return "Day of the Week";
	}

	@Override
	public @NotNull String getDescription() {
		if (allowedDays == null || allowedDays.isEmpty()) {
			return "No allowed days selected (never runs)";
		}
		if (allowedDays.size() == 7) {
			return "Any day of the week";
		}
		if (allowedDays.equals(WEEKDAYS)) {
			return "Weekdays only (Mon - Fri)";
		}
		if (allowedDays.equals(WEEKENDS)) {
			return "Weekends only (Sat - Sun)";
		}
		String daysStr = allowedDays.stream()
				.sorted(Comparator.comparingInt(DayOfWeek::getValue))
				.map(d -> d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
				.collect(Collectors.joining(", "));
		return "Only on " + daysStr;
	}

	@Override
	public @NotNull ConditionType getType() {
		return ConditionType.LOCAL;
	}

	@Override
	protected @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException {
		if (allowedDays == null || allowedDays.isEmpty()) {
			fail("No allowed days configured for Day of Week condition.");
		}

		ZoneId zone = zoneId != null ? zoneId : ZoneId.systemDefault();
		Instant currentInstant = context.getEvaluationTime();
		ZonedDateTime zdt = currentInstant.atZone(zone);
		DayOfWeek currentDay = zdt.getDayOfWeek();

		if (allowedDays.contains(currentDay)) {
			return pass(String.format("Current day (%s) is an allowed day of the week",
					currentDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH)));
		}

		// Calculate next allowed day start timestamp for expiry
		Instant nextEligibleInstant = null;
		for (int i = 1; i <= 7; i++) {
			LocalDate candidateDate = zdt.toLocalDate().plusDays(i);
			if (allowedDays.contains(candidateDate.getDayOfWeek())) {
				nextEligibleInstant = candidateDate.atStartOfDay(zone).toInstant();
				break;
			}
		}

		String message = String.format("Current day (%s) is not in allowed days: %s",
				currentDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
				getDescription());

		if (nextEligibleInstant != null) {
			fail(message, nextEligibleInstant);
		} else {
			fail(message);
		}
		return pass(message); // Unreachable due to fail()
	}
}
