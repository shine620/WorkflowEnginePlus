package com.hy.workflow.enums;

public final class FlowElementType {

    public static final String START_EVENT = "startEvent";

    public static final String END_EVENT = "endEvent";

    public static final String USER_TASK = "userTask";

    public static final String PARALLEL_TASK = "parallelTask";

    public static final String SEQUENTIAL_TASK = "sequentialTask";

    public static final String SERVICE_TASK = "serviceTask";

    public static final String PARALLEL_GATEWAY = "parallelGateway";

    public static final String EXCLUSIVE_GATEWAY = "exclusiveGateway";

    public static final String INCLUSIVE_GATEWAY = "inclusiveGateway";

    public static final String SUB_PROCESS = "subProcess";

    public static final String CALL_ACTIVITY = "callActivity";


}
