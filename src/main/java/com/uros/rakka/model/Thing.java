package com.uros.rakka.model;

import java.util.ArrayList;
import java.util.List;

public class Thing {
    public String name;
    public List<Property> properties = new ArrayList<>();
    public List<Message> messages = new ArrayList<>();
    public List<State> states = new ArrayList<>();
    public String initialStateName;
}