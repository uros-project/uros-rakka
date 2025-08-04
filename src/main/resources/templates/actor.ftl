package com.example.generated;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;
import java.util.Optional;

// ... (导入生成的命令消息类)
<#list thing.messages as message>
import com.example.generated.${message.className};
</#list>

/**
 * Auto-generated Akka Actor for UrosRML Thing: ${thing.name}
 */
public class ${thing.name}Actor extends AbstractBehavior<Object> {
    
    // Properties
    <#list thing.properties as prop>
    private ${prop.type} ${prop.name}<#if prop.value??> = ${prop.value}</#if>;
    </#list>

    // Constructor
    private ${thing.name}Actor(ActorContext<Object> context) {
        super(context);
    }

    // Factory method
    public static Behavior<Object> create() {
        return Behaviors.setup(${thing.name}Actor::new);
    }

    // Initial behavior
    @Override
    public Receive<Object> createReceive() {
        return ${thing.initialStateName}State();
    }
    
    // States
    <#list thing.states as state>
    private Behavior<Object> ${state.name}State() {
        return newReceiveBuilder()
        <#list state.onRules as rule>
                .onMessage(${rule.message.className}.class, message -> {
                    // Condition: ${rule.condition}
                    if (${rule.condition}) {
                    <#list rule.actions as action>
                        ${action}
                    </#list>
                    }
                    return Behaviors.same();
                })
        </#list>
                .build();
    }
    </#list>
    
    // Helper method to convert UrosRML types to Java types
    private String getJavaType(String urosRMLType) {
        switch (urosRMLType) {
            case "boolean": return "boolean";
            case "UInt8": return "byte";
            case "Int32": return "int";
            case "String": return "String";
            default: return "Object";
        }
    }
}