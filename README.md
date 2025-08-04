# UrosRML Akka Actor Code Generator

This project is a code generator that translates a custom Domain-Specific Language (DSL) called **UrosRML** into Java code for the **Akka Actor** framework. It is built using **ANTLR4** for parsing and **FreeMarker** for template-based code generation.

The generator's design emphasizes the separation of concerns:

  * The ANTLR parser and visitor handle the syntactic analysis of the DSL.
  * The visitor builds a clean data model from the parsed tree.
  * FreeMarker templates use this data model to generate the final, well-structured Java code.

## Features

  * **DSL Parsing:** Parses a `.urosrml` file that defines IoT device interfaces and behaviors.
  * **Akka Actor Generation:** Automatically creates Java Akka Actor classes that implement the behavior defined in the DSL.
  * **Template-Based:** The generated code's structure and style can be easily customized by modifying the FreeMarker template files.
  * **Modular Design:** The parsing logic and code templates are decoupled, making the system flexible and easy to maintain.
  * **Maven Integration:** Uses Maven for build automation and dependency management.

## Prerequisites

  * Java Development Kit (JDK) 11 or newer
  * Apache Maven

## Project Structure

```
.
├── pom.xml                                 # Maven build configuration
└── src
    ├── main
    │   ├── antlr
    │   │   └── UrosRML.g4                  # ANTLR grammar file for UrosRML
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           ├── Generator.java      # Main application to run the generation
    │   │           ├── UrosRMLCodeVisitor.java # ANTLR Visitor that builds the data model
    │   │           └── model               # Data model classes for FreeMarker
    │   │               ├── Thing.java
    │   │               ├── State.java
    │   │               └── ... (other model files)
    │   └── resources
    │       ├── templates                   # FreeMarker templates
    │       │   ├── actor.ftl               # Template for generating the Akka Actor class
    │       │   └── message.ftl             # Template for generating message classes
    │       └── your_urosrml_file.urosrml   # Your DSL input file
```

## Getting Started

### 1\. Build the Project

First, use Maven to build the project and generate the ANTLR parser source files.

```bash
mvn clean install
```

This command will:

  * Download all dependencies.
  * Run the `antlr4-maven-plugin` to generate `UrosRMLParser.java`, `UrosRMLLexer.java`, etc., in the `target/generated-sources/antlr4` directory.
  * Compile all Java source code.

### 2\. Prepare Your DSL File

Place your `.urosrml` file in the `src/main/resources` directory.

### 3\. Run the Code Generator

Execute the `Generator` main class to start the code generation process.

```bash
mvn exec:java -Dexec.mainClass="com.example.Generator"
```

The generated Java files (e.g., `LightBulbActor.java`, `ToggleCommand.java`) will be written to the `src/main/java/com/example/generated` directory.

## DSL Syntax Example

Here's an example of the UrosRML DSL for a simple `LightBulb` device.

```urosrml
interface Switchable {
    commands {
        toggle(newValue: boolean);
    }
}

thing LightBulb implements Switchable {
    properties {
        isOn: boolean = false;
    }
    
    behavior {
        statechart {
            state Off {
                on command toggle(newValue: boolean) if (newValue) {
                    assign isOn = true;
                    goto On;
                }
            }
            
            state On {
                on command toggle(newValue: boolean) if (!newValue) {
                    assign isOn = false;
                    goto Off;
                }
            }
        }
    }
}
```

## Generated Code Example

The generator would produce an `Akka` Actor class similar to this for the `LightBulb` example.

```java
// src/main/java/com/example/generated/LightBulbActor.java
package com.example.generated;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;

public class LightBulbActor extends AbstractBehavior<Object> {
    
    private boolean isOn = false;

    private LightBulbActor(ActorContext<Object> context) {
        super(context);
    }

    public static Behavior<Object> create() {
        return Behaviors.setup(LightBulbActor::new);
    }
    
    @Override
    public Receive<Object> createReceive() {
        return offState();
    }
    
    private Behavior<Object> offState() {
        return newReceiveBuilder()
            .onMessage(ToggleCommand.class, message -> {
                if (message.newValue) {
                    this.isOn = true;
                    return onState();
                }
                return Behaviors.same();
            })
            .build();
    }
    
    private Behavior<Object> onState() {
        return newReceiveBuilder()
            .onMessage(ToggleCommand.class, message -> {
                if (!message.newValue) {
                    this.isOn = false;
                    return offState();
                }
                return Behaviors.same();
            })
            .build();
    }
}
```

## Customization

The power of this project lies in its templating. You can easily modify the code generation output by editing the `.ftl` files located in `src/main/resources/templates/`.

  * **`actor.ftl`**: Edit this file to change the structure of the Akka Actor class. You can add new methods, change class names, or alter the logic.
  * **`message.ftl`**: Modify this file to change how command and event message classes are generated.