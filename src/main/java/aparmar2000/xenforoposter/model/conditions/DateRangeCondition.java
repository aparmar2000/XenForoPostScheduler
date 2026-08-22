package aparmar2000.xenforoposter.model.conditions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
public class DateRangeCondition extends PostCondition {
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@NotNull @Builder.Default String id = UUID.randomUUID().toString();
	@Nullable LocalDate startDate;
	@Nullable LocalDate endDate;
	@Nullable ZoneId zoneId;

	@Override
	public @NotNull String getDisplayName() {
		return "Date Range";
	}

	@Override
	public @NotNull String getDescription() {
		if (startDate != null && endDate != null) {
			if (startDate.equals(endDate)) {
				return "Only on " + DATE_FORMATTER.format(startDate);
			}
			return "Between " + DATE_FORMATTER.format(startDate) + " and " + DATE_FORMATTER.format(endDate);
		}
		if (startDate != null) {
			return "From " + DATE_FORMATTER.format(startDate) + " onwards";
		} else if (endDate != null) {
			return "Until " + DATE_FORMATTER.format(endDate);
		}
		return "Any date";
	}

	@Override
	public @NotNull ConditionType getType() {
		return ConditionType.LOCAL;
	}

	@Override
	protected @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException {
		ZoneId zone = zoneId != null ? zoneId : ZoneId.systemDefault();
		Instant currentInstant = context.getEvaluationTime();
		LocalDate currentDate = currentInstant.atZone(zone).toLocalDate();

		if (startDate != null && currentDate.isBefore(startDate)) {
			Instant startInstant = startDate.atStartOfDay(zone).toInstant();
			fail(String.format("Waiting for start date: current date (%s) is before %s",
					DATE_FORMATTER.format(currentDate), DATE_FORMATTER.format(startDate)), startInstant);
		}

		if (endDate != null && currentDate.isAfter(endDate)) {
			fail(String.format("Date range expired: current date (%s) is after %s",
					DATE_FORMATTER.format(currentDate), DATE_FORMATTER.format(endDate)));
		}

		return pass(String.format("Current date (%s) is within scheduled date range",
				DATE_FORMATTER.format(currentDate)));
	}
}
