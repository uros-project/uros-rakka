package com.uros.rakka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.uros.rakka.model.Message;
import com.uros.rakka.model.Thing;
import com.uros.rml.UrosRMLLexer;
import com.uros.rml.UrosRMLParser;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class Generator {

    public static void main(String[] args) throws IOException, TemplateException {
        // 1. ANTLR 解析
        CharStream input = CharStreams.fromFileName("your_urosrml_file.urosrml");
        UrosRMLLexer lexer = new UrosRMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        UrosRMLParser parser = new UrosRMLParser(tokens);
        ParseTree tree = parser.file();
        
        // 2. Visitor 遍历并构建数据模型
        UrosRMLCodeVisitor visitor = new UrosRMLCodeVisitor();
        Map<String, Thing> things = (Map<String, Thing>) visitor.visit(tree);
        
        // 3. FreeMarker 配置
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
        cfg.setDefaultEncoding("UTF-8");
        
        // 4. 渲染模板
        for (Thing thing : things.values()) {
            Template actorTemplate = cfg.getTemplate("actor.ftl");
            
            // 为每个 Thing 生成 Actor 代码
            File outputFile = new File("src/main/java/com/example/generated/" + thing.name + "Actor.java");
            try (Writer fileWriter = new FileWriter(outputFile)) {
                actorTemplate.process(Map.of("thing", thing), fileWriter);
            }
            
            // 为每个命令/事件生成消息类
            for (Message message : thing.messages) {
                // ... (获取 message.ftl 模板并渲染)
            }
        }
        
        System.out.println("Code generation completed successfully!");
    }
}