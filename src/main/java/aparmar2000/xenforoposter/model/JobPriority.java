package aparmar2000.xenforoposter.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobPriority {
    LOW("Low", 1),
    NORMAL("Normal", 5),
    HIGH("High", 10),
    URGENT("Urgent", 20);

    private final String displayName;
    private final int weight;

    @Override
    public String toString() {
        return displayName;
    }
}
