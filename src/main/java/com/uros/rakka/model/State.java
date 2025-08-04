package com.uros.rakka.model;

import java.util.ArrayList;
import java.util.List;

public class State {
    public String name;
    public List<OnRule> onRules = new ArrayList<>();
    public GotoRule gotoRule;
}