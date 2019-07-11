package org.mybatis.generator.plugins.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mybatis.generator.plugins.interfaze.EnumInterface;

/**
 * @author Administrator
 * @date 2019/7/10 18:11
 */
@RequiredArgsConstructor
@Getter
public enum LombokEnum implements EnumInterface {

    DATA("@Data", "lombok.Data"),
    SETTER("@Setter", "lombok.Setter"),
    GETTER("@Getter", "lombok.Getter"),
    BUILDER("@Builder", "lombok.Builder"),
    ACCESSORS("@Accessors(chain = true)", "lombok.experimental.Accessors"),
    NO_ARGS_CONSTRUCTOR("@NoArgsConstructor", "lombok.NoArgsConstructor"),
    ALL_ARGS_CONSTRUCTOR("@AllArgsConstructor", "lombok.AllArgsConstructor"),
    REQUIRED_ARGS_CONSTRUCTOR("@RequiredArgsConstructor", "lombok.RequiredArgsConstructor");

    private final String annotation;
    private final String pkg;

}
