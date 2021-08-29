package org.mybatis.generator.plugins.custom;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.plugins.enums.LombokEnum;
import org.mybatis.generator.plugins.interfaze.EnumInterface;

import java.util.*;


public class CustomPlugin extends PluginAdapter {

    private static final String CUSTOM_LOMBOK_PROPERTY = "customLombok";
    private static final String LOMBOK_PACKAGE_PROPERTY = "lombokPackage";
    private static final String ENABLE_SWAGGER_PROPERTY = "enableSwagger";
    private static final String CUSTOM_SUPER_MAPPER_PROPERTY = "customSuperMapper";

    private String customLombok;
    private String lombokPackage;
    private String needSwagger;
    private String customSuperMapper;
    private final Map<String, String> lombokEnumMap;


    public CustomPlugin() {
        LombokEnum[] enums = LombokEnum.values();
        lombokEnumMap = new HashMap<>(enums.length);
        for (LombokEnum lombokEnum : enums)
            lombokEnumMap.put(lombokEnum.getAnnotation(), lombokEnum.getPkg());
    }

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);

        customLombok = properties.getProperty(CUSTOM_LOMBOK_PROPERTY);
        lombokPackage = properties.getProperty(LOMBOK_PACKAGE_PROPERTY);
        needSwagger = properties.getProperty(ENABLE_SWAGGER_PROPERTY);
        customSuperMapper = properties.getProperty(CUSTOM_SUPER_MAPPER_PROPERTY);
    }


    public boolean validate(List<String> warnings) {
        return true;
    }


    /**
     *
     */
    @Override
    public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
        addSuperMapper(interfaze, introspectedTable);
        return super.clientGenerated(interfaze, introspectedTable);
    }


    @Override
    public boolean modelFieldGenerated(Field field,
                                       TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
                                       IntrospectedTable introspectedTable,
                                       Plugin.ModelClassType modelClassType) {
        addFieldComment(field, topLevelClass, introspectedColumn, introspectedTable);
        return true;
    }

    private void addFieldComment(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable) {

        field.addJavaDocLine("/**"); //$NON-NLS-1$
        var columnRemarks = introspectedColumn.getRemarks();
        var remarks = new StringBuilder(" * 【").append(null == columnRemarks ? "no mark" : columnRemarks.replaceAll("(\r\n|\n|\r|\")", " "));
        var columnName = introspectedColumn.getActualColumnName();
        var primaryKey = introspectedTable.getPrimaryKeyColumns();
        for (IntrospectedColumn pk : primaryKey) {
            if (columnName.equals(pk.getActualColumnName())) {
                remarks.append(" [Primary key] ");
                continue;
            }
            remarks.append(introspectedColumn.isNullable() ? "(can be null)" : "(not be null)");
        }
        var defaultValue = introspectedColumn.getDefaultValue();
        if (null == defaultValue) {
            remarks.append(" (no default value)");
        } else {
            remarks.append("  (default value: ");
            remarks.append(defaultValue);
            remarks.append(")");
        }
        remarks.append(" 】");
        field.addJavaDocLine(remarks.toString());
        field.addJavaDocLine(" */");
        if (enableSwagger()) {
            topLevelClass.addImportedType("io.swagger.annotations.ApiModelProperty");
            field.addAnnotation("@ApiModelProperty(name = \"" + field.getName() + "\", value = \"" + remarks + "\")");
        }

    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document,
                                           IntrospectedTable introspectedTable) {

        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //add Lombok
        addLombokToTopLevelClass(topLevelClass, introspectedTable);
        //add Swagger
        addSwaggerToTopLevelClass(topLevelClass, introspectedTable);
        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
    }


    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //add Lombok
        addLombokToTopLevelClass(topLevelClass, introspectedTable);
        //add Swagger
        addSwaggerToTopLevelClass(topLevelClass, introspectedTable);
        return super.modelPrimaryKeyClassGenerated(topLevelClass, introspectedTable);
    }

    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //add Lombok
        addLombokToTopLevelClass(topLevelClass, introspectedTable);
        //add Swagger
        addSwaggerToTopLevelClass(topLevelClass, introspectedTable);
        return super.modelRecordWithBLOBsClassGenerated(topLevelClass, introspectedTable);
    }

    /**
     * with @Data or @Getter doesn't need getter method
     * 有 @Data或者 @Getter时不生成Get方法
     */
    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return enableLombok() && !(customLombok.contains("@Data") || customLombok.contains("@Getter"));
    }

    /**
     * with @Data or @Setter doesn't need setter method
     * 有 @Data或者 @Setter 时不生成Set方法
     */
    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return enableLombok() && !(customLombok.contains("@Data") || customLombok.contains("@Setter"));
    }

    /**
     * Whether you  use  Lombok
     * 是否使用Lombok
     */
    private boolean enableLombok() {
        return !(customLombok == null || customLombok.length() == 0);
    }

    /**
     * Whether you  use  Swagger
     * 是否使用Swagger
     */
    private boolean enableSwagger() {
        return Boolean.TRUE.toString().equalsIgnoreCase(needSwagger);
    }

    /**
     * Whether you  use  super-mapper
     * 是否使用Swagger
     */
    private boolean enableSuperMapper() {
        return !(customSuperMapper == null || customSuperMapper.length() == 0);
    }


    /**
     * add Lombok support
     */
    private void addLombokToTopLevelClass(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (enableLombok()) {
            var customLombokAnnotations = customLombok.split(",");
            if (null == lombokPackage || lombokPackage.length() == 0) {
                for (var annotation : customLombokAnnotations) {
                    String pkg;
                    if (null == (pkg = lombokEnumMap.get(annotation))) {
                        topLevelClass.getAnnotations().add(annotation);
                        topLevelClass.addImportedType("lombok." + annotation.substring(1));
                    } else {
                        topLevelClass.getAnnotations().add(annotation);
                        topLevelClass.addImportedType(pkg);
                    }
                }
            } else {
                var lombokPackages = lombokPackage.split(",");
                for (int i = 0; i < customLombokAnnotations.length; i++) {
                    topLevelClass.getAnnotations().add(customLombokAnnotations[i]);
                    topLevelClass.addImportedType(lombokPackages[i]);
                }
            }
        } else {
            this.addAnnotation(topLevelClass,
                    LombokEnum.DATA, LombokEnum.NO_ARGS_CONSTRUCTOR,
                    LombokEnum.ALL_ARGS_CONSTRUCTOR, LombokEnum.BUILDER);
        }
    }

    /**
     * add Swagger support
     */
    private void addSwaggerToTopLevelClass(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //Swagger
        if (enableSwagger()) {
            var set = new HashSet<FullyQualifiedJavaType>();
            set.add(new FullyQualifiedJavaType("io.swagger.annotations.ApiModel"));
            topLevelClass.addImportedTypes(set);
            topLevelClass.getAnnotations().add("@ApiModel(description = \"" + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "\")");
        }
    }


    private void addAnnotation(TopLevelClass topLevelClass, EnumInterface... enumInterfaces) {
        for (EnumInterface enumInterface : enumInterfaces) {
            topLevelClass.getAnnotations().add(enumInterface.getAnnotation());
            topLevelClass.addImportedType(enumInterface.getPkg());
        }
    }

    private void addSuperMapper(Interface interfaze, IntrospectedTable introspectedTable) {
        if (enableSuperMapper()) {

            var baseType = new FullyQualifiedJavaType(customSuperMapper);

            var superType = new FullyQualifiedJavaType(baseType.getShortName());
            superType.addTypeArgument(introspectedTable.getPrimaryKeyColumns().get(0).getFullyQualifiedJavaType());
            superType.addTypeArgument(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));
            interfaze.addSuperInterface(superType);

            interfaze.addImportedType(baseType);
        }
    }

}
