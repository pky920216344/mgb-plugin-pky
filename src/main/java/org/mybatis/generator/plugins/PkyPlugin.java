package org.mybatis.generator.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.enums.LombokEnum;

import java.util.List;
import java.util.Properties;


public class PkyPlugin extends PluginAdapter {

    private String customLombok;
    private String lombokPackage;
    private String needSwagger;
    private String needInsertBatch;


    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);

        customLombok = properties.getProperty("customLombok");
        lombokPackage = properties.getProperty("lombokPackage");
        needSwagger = properties.getProperty("enableSwagger");
        needInsertBatch = properties.getProperty("needInsertBatch");
    }


    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //add Lombok
        addLombokToTopLevelClass(topLevelClass, introspectedTable);
        //add Swagger
        addSwaggerToTopLevelClass(topLevelClass, introspectedTable);
        return true;
    }


    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //add Lombok
        addLombokToTopLevelClass(topLevelClass, introspectedTable);
        //add Swagger
        addSwaggerToTopLevelClass(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //add Lombok
        addLombokToTopLevelClass(topLevelClass, introspectedTable);
        //add Swagger
        addSwaggerToTopLevelClass(topLevelClass, introspectedTable);
        return true;
    }

    /**
     * with @Data or @Getter doesn't need getter method
     */
    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return enableLombok() && !(customLombok.contains("@Data") || customLombok.contains("@Getter"));
    }

    /**
     * with @Data or @Setter doesn't need getter method
     */
    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return enableLombok() && !(customLombok.contains("@Data") || customLombok.contains("@Setter"));
    }


    public boolean enableLombok() {
        return !(customLombok == null || customLombok.length() == 0);
    }

    public boolean enableSwagger() {
        return Boolean.TRUE.toString().equalsIgnoreCase(needSwagger);
    }

    public boolean enableInsertBatch() {
        return Boolean.TRUE.toString().equalsIgnoreCase(needInsertBatch);
    }

    private void addLombokToTopLevelClass(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (enableLombok()) {
            String[] customLombokAnnotations = customLombok.split(",");
            if (null == lombokPackage || lombokPackage.length() == 0) {
                for (String annotation : customLombokAnnotations) {
                    topLevelClass.getAnnotations().add(annotation);
                    topLevelClass.addImportedType("lombok." + annotation.substring(1));
                }
            } else {
                String[] lombokPackages = lombokPackage.split(",");
                for (int i = 0; i < customLombokAnnotations.length; i++) {
                    topLevelClass.getAnnotations().add(customLombokAnnotations[i]);
                    topLevelClass.addImportedType(lombokPackages[i]);
                }
            }
        } else {
            topLevelClass.getAnnotations().add(LombokEnum.DATA.getAnnotation());
            topLevelClass.addImportedType(LombokEnum.DATA.getPkg());

            topLevelClass.getAnnotations().add("@NoArgsConstructor");
            topLevelClass.getAnnotations().add("@AllArgsConstructor");
            topLevelClass.getAnnotations().add("@Builder");
            topLevelClass.addImportedType("lombok.NoArgsConstructor");
            topLevelClass.addImportedType("lombok.AllArgsConstructor");
            topLevelClass.addImportedType("lombok.Builder");
        }

    }

    private void addSwaggerToTopLevelClass(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //Swagger
        if (enableSwagger()) {
            topLevelClass.getAnnotations().add("@ApiModel(description = \"" + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "\")");
            topLevelClass.addImportedType("io.swagger.annotations.ApiModel");
        }
    }

    private void addAnnotation() {

    }

}
