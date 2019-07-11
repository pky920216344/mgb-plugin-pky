package org.mybatis.generator.plugins.custom;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.config.JavaClientGeneratorConfiguration;
import org.mybatis.generator.plugins.enums.LombokEnum;
import org.mybatis.generator.plugins.interfaze.EnumInterface;

import java.util.List;
import java.util.Properties;


public class CustomPlugin extends PluginAdapter {

    private String customLombok;
    private String lombokPackage;
    private String needSwagger;
    private String needInsertBatch;
    private String needInsertMulti;


    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);

        customLombok = properties.getProperty("customLombok");
        lombokPackage = properties.getProperty("lombokPackage");
        needSwagger = properties.getProperty("enableSwagger");
        needInsertBatch = properties.getProperty("needInsertBatch");
        needInsertMulti = properties.getProperty("needInsertMulti");
    }


    public boolean validate(List<String> warnings) {
        return true;
    }


    /**
     *
     */
    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // add insertBatch
        addInsertBatch(interfaze, topLevelClass, introspectedTable);
        // add insertMulti
        addInsertMulti(interfaze, topLevelClass, introspectedTable);
        return super.clientGenerated(interfaze, topLevelClass, introspectedTable);
    }

    private void addInsertMulti(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {

    }

    private void addInsertBatch(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {

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
        StringBuilder remarks = new StringBuilder(" * 【").append(introspectedColumn.getRemarks().replaceAll("(\r\n|\n|\r|\")", " "));
        String columnName = introspectedColumn.getActualColumnName();
        List<IntrospectedColumn> primaryKey = introspectedTable.getPrimaryKeyColumns();
        for (IntrospectedColumn pk : primaryKey) {
            if (columnName.equals(pk.getActualColumnName())) {
                remarks.append(" (主健ID)");
                continue;
            }
            remarks.append(introspectedColumn.isNullable() ? "(可选项)" : "(必填项)");
        }
        String defaultValue = introspectedColumn.getDefaultValue();
        if (null == defaultValue) {
            remarks.append(" (无默认值)");
        } else {
            remarks.append("  (默认值为: ");
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
        addInsertMultiXMLMapper(document, introspectedTable);
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
     * Whether  Use  Lombok
     * 是否使用Lombok
     */
    private boolean enableLombok() {
        return !(customLombok == null || customLombok.length() == 0);
    }

    /**
     * Whether  Use  Swagger
     * 是否使用Swagger
     */
    private boolean enableSwagger() {
        return Boolean.TRUE.toString().equalsIgnoreCase(needSwagger);
    }

    /**
     * Whether  Use  insertBatch(notes: different with insertMulti)
     * 是否开启批量新增
     * like this:
     * for(T t: collection){
     * xxxMapper.insertSelective(t);
     * }
     */
    private boolean enableInsertBatch() {
        return Boolean.TRUE.toString().equalsIgnoreCase(needInsertBatch);
    }

    /**
     * Whether  Use  insertMulti
     * 是否开启多条新增
     * like this:
     * INSERT INTO table VALUES (t1),(t2),(t3),(t4)
     */
    private boolean enableInsertMulti() {
        return Boolean.TRUE.toString().equalsIgnoreCase(needInsertMulti);
    }

    /**
     * XML file add insertMulti
     * {{@link IntrospectedTable.TargetRuntime#MYBATIS3_DSQL}} do not add  这个没有XML文件不需要添加
     * {{@link IntrospectedTable.TargetRuntime#IBATIS2}} i don't know,  i haven't used it  这个没用过不知道
     * {{@link IntrospectedTable.TargetRuntime#MYBATIS3}} when
     */
    private void addInsertMultiXMLMapper(Document document,
                                         IntrospectedTable introspectedTable) {
        IntrospectedTable.TargetRuntime runtime = introspectedTable.getTargetRuntime();
        switch (runtime) {
            case MYBATIS3:
                JavaClientGeneratorConfiguration javaClientGeneratorConfiguration;
                if (null == (javaClientGeneratorConfiguration = context.getJavaClientGeneratorConfiguration()))
                    break;
                String type = javaClientGeneratorConfiguration.getConfigurationType();
                if ("XMLMAPPER".equalsIgnoreCase(type)) {

                }
                break;
            default:
                //do nothing
        }
    }


    /**
     * add Lombok support
     */
    private void addLombokToTopLevelClass(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (enableLombok()) {
            String[] customLombokAnnotations = customLombok.split(",");
            if (null == lombokPackage || lombokPackage.length() == 0) {
                for (String annotation : customLombokAnnotations) {
                    String pkg;
                    if (null == (pkg = LombokEnum.getLombokEnumMap().get(annotation))) {
                        topLevelClass.getAnnotations().add(annotation);
                        topLevelClass.addImportedType("lombok." + annotation.substring(1));
                    } else {
                        topLevelClass.getAnnotations().add(annotation);
                        topLevelClass.addImportedType(pkg);
                    }
                }
            } else {
                String[] lombokPackages = lombokPackage.split(",");
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
            topLevelClass.addImportedType("io.swagger.annotations.ApiModel");
            topLevelClass.getAnnotations().add("@ApiModel(description = \"" + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "\")");
        }
    }


    private void addAnnotation(TopLevelClass topLevelClass, EnumInterface... enumInterfaces) {
        for (EnumInterface enumInterface : enumInterfaces) {
            topLevelClass.getAnnotations().add(enumInterface.getAnnotation());
            topLevelClass.addImportedType(enumInterface.getPkg());
        }
    }

}
