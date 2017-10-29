package org.pgptool.gui.feedback.api;

public class UserFeedback {
	public static final String FN_EMAIL = "email";

	private String email;
	private String feedback;
	private int rating;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}
}
