package org.mybatis.generator.utils;

import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;

/**
 * @author Administrator
 * @date 2019/7/10 18:06
 */
public abstract class MethodGenerator {

    public static Method defaultGenerator(String methodName, JavaVisibility visibility, FullyQualifiedJavaType returnJavaType, Parameter... parameters) {
        Method method = new Method();
        method.setName(methodName);
        method.setVisibility(visibility);
        method.setReturnType(returnJavaType);
        for (Parameter parameter : parameters)
            method.addParameter(parameter);
        return method;
    }
}
