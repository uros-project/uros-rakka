package com.uros.rakka.model;

import java.util.ArrayList;
import java.util.List;

public class OnRule {
    public String type; // COMMAND or EVENT
    public Message message;
    public String condition; // if 表达式的 Java 代码
    public List<String> actions = new ArrayList<>(); // 动作的 Java 代码
}