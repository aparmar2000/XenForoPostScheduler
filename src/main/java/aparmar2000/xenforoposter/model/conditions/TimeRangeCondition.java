package aparmar2000.xenforoposter.model.conditions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
public class TimeRangeCondition extends PostCondition {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @NotNull @Builder.Default String id = UUID.randomUUID().toString();
    @NotNull LocalTime startTime;
    @NotNull LocalTime endTime;
    @Nullable ZoneId zoneId;

    @Override
    public @NotNull String getDisplayName() {
        return "Time Range";
    }

    @Override
    public @NotNull String getDescription() {
        String startStr = TIME_FORMATTER.format(startTime);
        String endStr = TIME_FORMATTER.format(endTime);
        if (startTime.equals(endTime)) {
            return "All day (24 hours)";
        }
        if (startTime.isAfter(endTime)) {
            return "Between " + startStr + " and " + endStr + " (overnight)";
        }
        return "Between " + startStr + " and " + endStr;
    }

    @Override
    public @NotNull ConditionType getType() {
        return ConditionType.LOCAL;
    }

    @Override
    protected @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException {
        ZoneId zone = zoneId != null ? zoneId : ZoneId.systemDefault();
        Instant currentInstant = context.getEvaluationTime();
        ZonedDateTime zdt = currentInstant.atZone(zone);
        LocalTime currentTime = zdt.toLocalTime();
        LocalDate currentDate = zdt.toLocalDate();

        if (startTime.equals(endTime)) {
            return pass("24-hour time range allows execution at any time");
        }

        boolean isNormalRange = startTime.isBefore(endTime);

        if (isNormalRange) {
            if (!currentTime.isBefore(startTime) && !currentTime.isAfter(endTime)) {
                return pass(String.format("Current time (%s) is within window [%s - %s]",
                        TIME_FORMATTER.format(currentTime), TIME_FORMATTER.format(startTime), TIME_FORMATTER.format(endTime)));
            }

            Instant nextStart;
            if (currentTime.isBefore(startTime)) {
                nextStart = currentDate.atTime(startTime).atZone(zone).toInstant();
            } else {
                nextStart = currentDate.plusDays(1).atTime(startTime).atZone(zone).toInstant();
            }

            fail(String.format("Current time (%s) is outside window [%s - %s]",
                    TIME_FORMATTER.format(currentTime), TIME_FORMATTER.format(startTime), TIME_FORMATTER.format(endTime)), nextStart);
        } else {
            // Overnight range e.g. 22:00 to 06:00
            if (!currentTime.isBefore(startTime) || !currentTime.isAfter(endTime)) {
                return pass(String.format("Current time (%s) is within overnight window [%s - %s]",
                        TIME_FORMATTER.format(currentTime), TIME_FORMATTER.format(startTime), TIME_FORMATTER.format(endTime)));
            }

            Instant nextStart = currentDate.atTime(startTime).atZone(zone).toInstant();
            fail(String.format("Current time (%s) is outside overnight window [%s - %s]",
                    TIME_FORMATTER.format(currentTime), TIME_FORMATTER.format(startTime), TIME_FORMATTER.format(endTime)), nextStart);
        }
        return pass("OK"); // Unreachable due to fail()
    }
}
