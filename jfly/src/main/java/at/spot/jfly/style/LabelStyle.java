package at.spot.jfly.style;

public enum LabelStyle {
	Default("label-default"),
	Primary("label-primary"),
	Success("label-success"),
	Info("label-info"),
	Warning("label-warning"),
	Danger("label-danger"),;

	private String styleClass;

	private LabelStyle(final String styleClass) {
		this.styleClass = styleClass;
	}

	@Override
	public String toString() {
		return this.styleClass;
	}
}