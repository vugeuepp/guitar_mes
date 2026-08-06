package com.example.guitarmes.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeFormatterUtil {
	
	private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
	private DateTimeFormatterUtil() {
		
	}
	public static String format(LocalDateTime dateTime) {
		if (dateTime == null) {
			return "-";
		}
		return dateTime.format(DISPLAY_FORMATTER);
	}
}
