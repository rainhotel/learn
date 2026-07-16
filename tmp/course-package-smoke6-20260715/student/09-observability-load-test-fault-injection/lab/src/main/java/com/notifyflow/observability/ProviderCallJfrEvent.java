package com.notifyflow.observability;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Timespan;

@Name(ProviderCallJfrEvent.EVENT_NAME)
@Label("NotifyFlow Provider Call")
@Category({"NotifyFlow", "Provider"})
@Description("A bounded-cardinality event for an accepted NotifyFlow provider task")
public final class ProviderCallJfrEvent extends Event {

    public static final String EVENT_NAME = "com.notifyflow.ProviderCall";

    @Label("Task Type")
    public String taskType;

    @Label("Accepted")
    public boolean accepted;

    @Label("Queue Depth At Start")
    public int queueDepth;

    @Label("Measured Task Duration")
    @Timespan(Timespan.NANOSECONDS)
    public long durationNanos;

    @Label("Outcome")
    public String outcome;
}
