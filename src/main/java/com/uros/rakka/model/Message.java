package com.uros.rakka.model;

import java.util.ArrayList;
import java.util.List;

public class Message {
    public String name;
    public String className; // 生成的 Java 类名，如 ToggleMessage
    public List<Param> params = new ArrayList<>();
}