package com.hy.workflow.base;

import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.condition.ConditionUtil;

public class EvaluateExpressionCmd  implements Command<Boolean> {


    private final SequenceFlow sequenceFlow;
    private final ExecutionEntity execution;

    public EvaluateExpressionCmd(SequenceFlow sequenceFlow, ExecutionEntity execution) {
        this.sequenceFlow = sequenceFlow;
        this.execution = execution;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        String expr =  sequenceFlow.getConditionExpression();
        if (expr!=null && ConditionUtil.hasTrueCondition(sequenceFlow, execution)) {
            return true;
        }
        return false;
    }

}
