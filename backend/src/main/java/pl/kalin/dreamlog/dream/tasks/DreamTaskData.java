package pl.kalin.dreamlog.dream.tasks;

import java.io.Serializable;
import java.util.UUID;

/**
 * Serializable task data for db-scheduler tasks.
 * Stored in scheduled_tasks.task_data column as serialized bytes.
 */
public record DreamTaskData(UUID dreamId) implements Serializable {
}
