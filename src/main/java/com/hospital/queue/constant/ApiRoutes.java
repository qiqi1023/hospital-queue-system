package com.hospital.queue.constant;

public final class ApiRoutes {
	public static final String API_PREFIX = "/api";
	public static final String QUEUES = API_PREFIX + "/queues";
	public static final String QUEUE_NUMBER_PATH = "/{queueNumber}";
	public static final String DEPARTMENTS = API_PREFIX + "/departments";
	public static final String CURRENT_QUEUES = "/current-queues";

	private ApiRoutes() {
	}
}
