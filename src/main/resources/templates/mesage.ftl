package com.example.generated;

/**
 * Auto-generated message class for UrosRML command: ${message.name}
 */
public class ${message.className} {
    <#list message.params as param>
    public final ${param.type} ${param.name};
    </#list>

    public ${message.className}(<#list message.params as param>${param.type} ${param.name}<#sep>, </#list>) {
    <#list message.params as param>
        this.${param.name} = ${param.name};
    </#list>
    }
}