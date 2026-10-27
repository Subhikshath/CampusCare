package com.CampusCare.ai;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CampusLocationValidator {

	private static final Set<String> GENERIC_LOCATIONS = Set.of(
			"campus", "college", "college campus", "block", "building", "hostel",
			"ground", "field", "parking", "parking lot", "room", "floor", "library",
			"lab", "laboratory", "classroom", "hall", "canteen", "gate",
			"block name", "building name", "hostel name", "ground name", "field name",
			"parking lot name", "parking area", "room number", "room no", "floor number");

	private static final Pattern LOCATION_MARKER = Pattern.compile(
			"\\b(?:block|building|hostel|ground|field|parking(?:\\s+lot)?|"
					+ "library|laboratory|lab|canteen|classroom|lecture\\s+hall|"
					+ "auditorium|gate|bus\\s+stop)\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern WORDS = Pattern.compile("[\\p{L}\\p{N}]+");
	private static final Pattern GENERIC_SITE_WITH_ID = Pattern.compile(
			"\\b(?:block|building|hostel|ground|field|parking(?:\\s+lot)?|gate)\\s+(?:[a-z]|\\d+)\\b",
			Pattern.CASE_INSENSITIVE);

	public boolean isSpecificLocation(String location, String studentText) {
		if (location == null || studentText == null) {
			return false;
		}

		String normalizedLocation = normalize(location);
		if (normalizedLocation.isBlank()
				|| GENERIC_LOCATIONS.contains(normalizedLocation)
				|| !LOCATION_MARKER.matcher(normalizedLocation).find()) {
			return false;
		}

		long locationWordCount = WORDS.matcher(normalizedLocation).results().count();
		String normalizedStudentText = normalize(studentText);
		boolean hasNamedPlace = WORDS.matcher(normalizedLocation).results()
				.map(result -> result.group())
				.anyMatch(word -> !word.matches(
						"campus|college|block|building|hostel|ground|field|parking|lot|"
								+ "room|floor|library|lab|laboratory|classroom|hall|canteen|"
								+ "gate|bus|stop|the|name|number|no|area"));
		return locationWordCount >= 2
				&& (hasNamedPlace || GENERIC_SITE_WITH_ID.matcher(normalizedLocation).find())
				&& normalizedStudentText.contains(normalizedLocation);
	}

	public static String findMockLocation(String studentText) {
		if (studentText == null) {
			return null;
		}

		Pattern namedLocation = Pattern.compile(
				"\\b(?:block|building|hostel|ground|field|parking(?:\\s+lot)?|room|floor|"
						+ "library|laboratory|lab|canteen|classroom|lecture\\s+hall|"
						+ "auditorium|gate|bus\\s+stop)\\s+[\\p{L}\\p{N}][\\p{L}\\p{N}-]*\\b",
				Pattern.CASE_INSENSITIVE);
		var match = namedLocation.matcher(studentText);
		if (!match.find()) {
			return null;
		}

		String location = match.group().trim();
		return new CampusLocationValidator().isSpecificLocation(location, studentText)
				? location : null;
	}

	public String studentLocationFollowUp() {
		return "Where exactly on campus is the issue? Please give the named building or block and, where relevant, its floor or room number. For a hostel, name the hostel; for an outdoor issue, name the ground or parking area.";
	}

	private String normalize(String value) {
		return WORDS.matcher(value.toLowerCase())
				.results()
				.map(result -> result.group())
				.collect(Collectors.joining(" "));
	}
}
