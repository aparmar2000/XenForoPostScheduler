package aparmar2000.xenforoposter.extension.hook;

public enum HookPriority {
	HIGHEST(100),
	HIGH(200),
	NORMAL(300),
	LOW(400),
	LOWEST(500);

	private final int weight;

	HookPriority(int weight) {
		this.weight = weight;
	}

	public int getWeight() {
		return weight;
	}
}
