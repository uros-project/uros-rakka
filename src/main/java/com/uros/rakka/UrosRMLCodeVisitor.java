package com.uros.rakka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.uros.rakka.model.GotoRule;
import com.uros.rakka.model.Message;
import com.uros.rakka.model.OnRule;
import com.uros.rakka.model.Property;
import com.uros.rakka.model.State;
import com.uros.rakka.model.Thing;
import com.uros.rml.UrosRMLBaseVisitor;
import com.uros.rml.UrosRMLParser;

public class UrosRMLCodeVisitor extends UrosRMLBaseVisitor<Object> {

    // 用于存储所有解析出来的 Thing 模型
    private final Map<String, Thing> things = new HashMap<>();

    // 用于存储所有接口中定义的命令和事件消息
    private final Map<String, Message> messages = new HashMap<>();

    @Override
    public Object visitFile(UrosRMLParser.FileContext ctx) {
        // 第一步: 遍历所有 interface，收集所有命令和事件
        // 这样在解析 Thing 的行为时，可以正确引用消息类型
        ctx.interface_().forEach(this::visit);

        // 第二步: 遍历所有 thing，生成 Thing 模型
        ctx.thing().forEach(this::visit);

        return things;
    }

    @Override
    public Object visitInterface(UrosRMLParser.InterfaceContext ctx) {
        // 如果存在，解析 implements 的接口，将它们的消息也添加进来
        // 目前简化处理，直接处理本接口的消息
        if (ctx.interfaceBody() != null) {
            visit(ctx.interfaceBody());
        }
        return null;
    }

    @Override
    public Object visitInterfaceBody(UrosRMLParser.InterfaceBodyContext ctx) {
        ctx.commandsSection().forEach(this::visitCommandsSection);
        ctx.eventsSection().forEach(this::visitEventsSection);
        return null;
    }

    @Override
    public Object visitCommandsSection(UrosRMLParser.CommandsSectionContext ctx) {
        ctx.command().forEach(commandCtx -> {
            Message msg = new Message();
            String name = commandCtx.ID().getText();
            msg.name = name;
            msg.className = toPascalCase(name) + "Command";
            if (commandCtx.paramList() != null) {
                msg.params = commandCtx.paramList().param().stream()
                        .map(this::createParamFromContext)
                        .collect(Collectors.toList());
            }
            messages.put(name, msg);
        });
        return null;
    }

    @Override
    public Object visitEventsSection(UrosRMLParser.EventsSectionContext ctx) {
        ctx.event().forEach(eventCtx -> {
            Message msg = new Message();
            String name = eventCtx.ID().getText();
            msg.name = name;
            msg.className = toPascalCase(name) + "Event";
            if (eventCtx.paramList() != null) {
                msg.params = eventCtx.paramList().param().stream()
                        .map(this::createParamFromContext)
                        .collect(Collectors.toList());
            }
            messages.put(name, msg);
        });
        return null;
    }

    @Override
    public Object visitThing(UrosRMLParser.ThingContext ctx) {
        Thing thing = new Thing();
        thing.name = ctx.ID().getText();

        if (ctx.implementsInterfaces() != null) {
            // 这里可以处理 implements 的接口，并将它们的消息添加到 Thing 中
            // 目前简化，不作处理
        }

        if (ctx.thingBody() != null) {
            // 遍历 Thing 的所有子节点
            ctx.thingBody().children.forEach(child -> {
                if (child instanceof UrosRMLParser.PropertiesSectionContext) {
                    thing.properties.addAll(visitPropertiesSection((UrosRMLParser.PropertiesSectionContext) child));
                } else if (child instanceof UrosRMLParser.BehaviorSectionContext) {
                    Thing tempThing = visitBehaviorSection((UrosRMLParser.BehaviorSectionContext) child);
                    thing.states.addAll(tempThing.states);
                    thing.initialStateName = tempThing.initialStateName;
                }
            });
        }

        // 从messages中找到thing所引用的所有消息
        // ...

        things.put(thing.name, thing);
        return thing;
    }

    @Override
    public List<Property> visitPropertiesSection(UrosRMLParser.PropertiesSectionContext ctx) {
        return ctx.property().stream()
                .map(this::createPropertyFromContext)
                .collect(Collectors.toList());
    }

    @Override
    public Thing visitBehaviorSection(UrosRMLParser.BehaviorSectionContext ctx) {
        Thing tempThing = new Thing();
        UrosRMLParser.StatechartContext statechartCtx = ctx.statechart();
        if (statechartCtx != null && !statechartCtx.state().isEmpty()) {
            tempThing.initialStateName = statechartCtx.state().get(0).ID().getText();
            tempThing.states = statechartCtx.state().stream()
                    .map(this::createStateFromContext)
                    .collect(Collectors.toList());
        }
        return tempThing;
    }

    private State createStateFromContext(UrosRMLParser.StateContext ctx) {
        State state = new State();
        state.name = ctx.ID().getText();

        ctx.onRule().forEach(onRuleCtx -> {
            OnRule rule = new OnRule();
            String messageName = onRuleCtx.ID().getText();
            rule.message = messages.get(messageName);

            // 暂时简化，不处理条件
            // if (onRuleCtx.condition() != null) {
            // rule.condition = visitCondition(onRuleCtx.condition());
            // }

            // 提取动作并生成 Java 代码字符串
            if (onRuleCtx.actionBlock() != null) {
                rule.actions = onRuleCtx.actionBlock().action().stream()
                        .map(this::visitAction)
                        .collect(Collectors.toList());
            }
            state.onRules.add(rule);
        });

        if (ctx.gotoRule() != null) {
            GotoRule gotoRule = new GotoRule();
            gotoRule.targetStateName = ctx.gotoRule().ID().getText();
            state.gotoRule = gotoRule;
        }

        return state;
    }

    // 辅助方法：将 UrosRML 动作转换为 Java 代码字符串
    @Override
    public String visitAction(UrosRMLParser.ActionContext ctx) {
        if (ctx.sendAction() != null) {
            return visitSendAction(ctx.sendAction());
        } else if (ctx.assignAction() != null) {
            return visitAssignAction(ctx.assignAction());
        } else if (ctx.ifAction() != null) {
            return visitIfAction(ctx.ifAction());
        }
        return "";
    }

    @Override
    public String visitSendAction(UrosRMLParser.SendActionContext ctx) {
        String msgName = ctx.ID().getText();
        String params = ctx.expressionList().expression().stream()
                .map(expressionCtx -> visitExpression(expressionCtx))
                .collect(Collectors.joining(", "));
        return String.format("context.self().tell(new %s(%s));", toPascalCase(msgName) + "Event", params);
    }

    @Override
    public String visitAssignAction(UrosRMLParser.AssignActionContext ctx) {
        String propName = ctx.ID(0).getText();
        String value = visitExpression(ctx.expression());
        return String.format("this.%s = %s;", propName, value);
    }

    @Override
    public String visitIfAction(UrosRMLParser.IfActionContext ctx) {
        String condition = visitExpression(ctx.expression());
        String ifBlock = ctx.actionBlock(0).action().stream()
                .map(this::visitAction)
                .collect(Collectors.joining("\n\t\t\t"));
        String elseBlock = "";
        if (ctx.ELSE() != null) {
            elseBlock = ctx.actionBlock(1).action().stream()
                    .map(this::visitAction)
                    .collect(Collectors.joining("\n\t\t\t"));
        }
        return String.format("if (%s) {\n\t\t\t%s\n\t\t} else {\n\t\t\t%s\n\t\t}", condition, ifBlock, elseBlock);
    }

    @Override
    public String visitExpression(UrosRMLParser.ExpressionContext ctx) {
        if (ctx.value() != null) {
            return visitValue(ctx.value());
        } else if (ctx.op() != null) {
            String left = visitExpression(ctx.expression(0));
            String right = visitExpression(ctx.expression(1));
            String op = ctx.op().getText();
            return String.format("(%s %s %s)", left, op, right);
        } else if (ctx.ID() != null) {
            return ctx.ID().getText();
        }
        return ctx.getText();
    }

    @Override
    public String visitValue(UrosRMLParser.ValueContext ctx) {
        if (ctx.BOOLEAN_LITERAL() != null)
            return ctx.BOOLEAN_LITERAL().getText();
        if (ctx.UINT8_LITERAL() != null)
            return ctx.UINT8_LITERAL().getText();
        if (ctx.INT32_LITERAL() != null)
            return ctx.INT32_LITERAL().getText();
        if (ctx.STRING_LITERAL() != null)
            return ctx.STRING_LITERAL().getText();
        return ctx.getText();
    }

    // --- 辅助方法 ---

    private Property createPropertyFromContext(UrosRMLParser.PropertyContext ctx) {
        Property prop = new Property();
        prop.name = ctx.ID().getText();
        prop.type = mapUrosRMLTypeToJava(ctx.dataType().getText());
        if (ctx.value() != null) {
            prop.value = visitValue(ctx.value());
        }
        return prop;
    }

    private Param createParamFromContext(UrosRMLParser.ParamContext ctx) {
        Param param = new Param();
        param.name = ctx.ID().getText();
        param.type = mapUrosRMLTypeToJava(ctx.dataType().getText());
        return param;
    }

    private String mapUrosRMLTypeToJava(String urosRMLType) {
        switch (urosRMLType) {
            case "boolean":
                return "boolean";
            case "UInt8":
                return "byte";
            case "Int32":
                return "int";
            case "String":
                return "String";
            default:
                return "Object"; // Fallback for unknown types
        }
    }

    private String toPascalCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}