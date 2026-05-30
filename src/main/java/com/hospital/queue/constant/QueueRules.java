package com.hospital.queue.constant;

import com.hospital.queue.domain.QueueStatus;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public final class QueueRules {
	public static final ZoneId MALAYSIA_ZONE = ZoneId.of("Asia/Kuala_Lumpur");
	public static final LocalTime QUEUE_OPEN_TIME = LocalTime.of(6, 0);
	public static final LocalTime LUNCH_BREAK_START_TIME = LocalTime.of(12, 0);
	public static final LocalTime LUNCH_BREAK_END_TIME = LocalTime.of(14, 0);
	public static final int INITIAL_SEQUENCE = 0;
	public static final int FIRST_SEQUENCE = 1;
	public static final long INITIAL_TICKET_ID = 1L;
	public static final int IC_DIGIT_COUNT = 12;
	public static final String NON_DIGIT_REGEX = "[^0-9]";
	public static final String QUEUE_NUMBER_SEQUENCE_FORMAT = "%03d";
	public static final List<QueueStatus> ACTIVE_STATUSES = List.of(
		QueueStatus.WAITING,
		QueueStatus.CALLED,
		QueueStatus.SERVING
	);

	private QueueRules() {
	}
}
