package org.mybatis.generator.plugins.custom;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.logging.Log;
import org.mybatis.generator.logging.LogFactory;
import org.mybatis.generator.plugins.enums.LombokEnum;
import org.mybatis.generator.plugins.interfaze.EnumInterface;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CustomPlugin extends PluginAdapter {

    private static final Log LOG = LogFactory.getLog(CustomPlugin.class);
    private static final String CUSTOM_LOMBOK_PROPERTY = "customLombok";
    private static final String LOMBOK_PACKAGE_PROPERTY = "lombokPackage";
    private static final String ENABLE_SWAGGER_PROPERTY = "enableSwagger";
    private static final String CUSTOM_SUPER_MAPPER_PROPERTY = "customSuperMapper";
    private static final String CUSTOM_SUPER_ENTITY_PROPERTY = "customSuperEntity";

    private String customLombok;
    private String lombokPackage;
    private String needSwagger;
    private String customSuperMapper;
    private String customSuperEntity;
    private final Map<String, String> lombokEnumMap = Stream.of(LombokEnum.values()).collect(Collectors.toMap(LombokEnum::getAnnotation, LombokEnum::getPkg));


    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        customLombok = properties.getProperty(CUSTOM_LOMBOK_PROPERTY);
        lombokPackage = properties.getProperty(LOMBOK_PACKAGE_PROPERTY);
        needSwagger = properties.getProperty(ENABLE_SWAGGER_PROPERTY);
        customSuperMapper = properties.getProperty(CUSTOM_SUPER_MAPPER_PROPERTY);
        customSuperEntity = properties.getProperty(CUSTOM_SUPER_ENTITY_PROPERTY);
    }


    public boolean validate(List<String> warnings) {
        return true;
    }


    @Override
    public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
        // add super mapper
        addSuperMapper(interfaze, introspectedTable);
        // add more method in mapper
        addGetColumnsMethod(interfaze, introspectedTable);

        removeGeneratedAnnotation(interfaze);
        return super.clientGenerated(interfaze, introspectedTable);
    }

    private void addGetColumnsMethod(Interface interfaze, IntrospectedTable introspectedTable) {
        var returnType = new FullyQualifiedJavaType("UpdateDSL");
        returnType.addTypeArgument(new FullyQualifiedJavaType("UpdateModel"));
        var parameter = new Parameter(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()), "record");
        var tableName = getTableField(introspectedTable.getFullyQualifiedTable().getDomainObjectName());

        var updateAllColumnsMethod = new Method("updateAllColumns");
        updateAllColumnsMethod.setDefault(true);
        updateAllColumnsMethod.setReturnType(returnType);
        updateAllColumnsMethod.addParameter(parameter);
        updateAllColumnsMethod.addBodyLine("return updateAllColumns(record, UpdateDSL.update(" + tableName + "));");
        interfaze.addMethod(updateAllColumnsMethod);

        var updateSelectiveColumnsMethod = new Method("updateSelectiveColumns");
        updateSelectiveColumnsMethod.setDefault(true);
        updateSelectiveColumnsMethod.setReturnType(returnType);
        updateSelectiveColumnsMethod.addParameter(parameter);
        updateSelectiveColumnsMethod.addBodyLine("return updateSelectiveColumns(record, UpdateDSL.update(" + tableName + "));");
        interfaze.addMethod(updateSelectiveColumnsMethod);

        //org.mybatis.dynamic.sql.SqlTable
        var getSqlTableMethod = new Method("getSqlTable");
        getSqlTableMethod.setDefault(true);
        var sqlTableType = new FullyQualifiedJavaType("SqlTable");
        getSqlTableMethod.setReturnType(sqlTableType);
        getSqlTableMethod.addBodyLine("return " + tableName + ";");
        interfaze.addMethod(getSqlTableMethod);
        interfaze.addImportedType(new FullyQualifiedJavaType("org.mybatis.dynamic.sql.SqlTable"));

    }

    private void removeGeneratedAnnotation(AbstractJavaType javaType) {
        if (javaType instanceof CompilationUnit) {
            var importedTypes = ((CompilationUnit) javaType).getImportedTypes();
            importedTypes.removeIf(importType -> "Generated".equals(importType.getShortName()));
        }
        removeGeneratedAnnotationFromJavaElement(javaType.getMethods());
        removeGeneratedAnnotationFromJavaElement(javaType.getFields());
        removeGeneratedAnnotationFromJavaElement(javaType.getInnerClasses());
        removeGeneratedAnnotationFromJavaElement(javaType.getInnerEnums());
    }

    private void removeGeneratedAnnotationFromJavaElement(Collection<? extends JavaElement> collection) {
        if (null == collection || collection.isEmpty()) {
            return;
        }
        for (var element : collection) {
            var annotations = element.getAnnotations();
            for (var annotation : annotations) {
                if (annotation.contains("Generated")) {
                    annotations.remove(annotation);
                    break;
                }
            }
        }
    }

    private String getTableField(String tableName) {
        var chars = tableName.toCharArray();
        var first = chars[0];
        if ('A' <= first && 'Z' >= first) {
            first ^= 'a' - 'A';
            chars[0] = first;
            return String.valueOf(chars);
        }
        return tableName;
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
        var remarks = new StringBuilder(" * 【 ")
                .append(null == columnRemarks ? "no mark" : columnRemarks.replaceAll("(\r\n|\n|\r|\")", " "))
                .append(" 】");
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
        //add super entity
        addSuperEntity(topLevelClass, introspectedTable);

        removeGeneratedAnnotation(topLevelClass);
        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
    }

    private void addSuperEntity(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (enableSuperEntity()) {
            var baseClass = new FullyQualifiedJavaType(customSuperEntity);
            topLevelClass.addImportedType(baseClass);

            var superType = new FullyQualifiedJavaType(baseClass.getShortName());
            superType.addTypeArgument(introspectedTable.getPrimaryKeyColumns().get(0).getFullyQualifiedJavaType());
            superType.addTypeArgument(new FullyQualifiedJavaType(introspectedTable.getBaseRecordType()));
            topLevelClass.addSuperInterface(superType);
        }
    }

    @Override
    public boolean dynamicSqlSupportGenerated(TopLevelClass supportClass, IntrospectedTable introspectedTable) {
        removeGeneratedAnnotation(supportClass);
        return super.dynamicSqlSupportGenerated(supportClass, introspectedTable);
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
     * 是否使用SuperMapper
     */
    private boolean enableSuperMapper() {
        return !(customSuperMapper == null || customSuperMapper.length() == 0);
    }

    /**
     * Whether you  use  super-entity
     * 是否使用SuperEntity
     */
    private boolean enableSuperEntity() {
        return !(customSuperEntity == null || customSuperEntity.length() == 0);
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
