package org.mybatis.generator.custom.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mybatis.generator.custom.interfaze.EnumInterface;

import java.util.HashMap;
import java.util.Map;

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

    private final static Map<String, String> lombokEnumMap;

    static {
        LombokEnum[] enums = values();
        lombokEnumMap = new HashMap<>(enums.length);
        for (LombokEnum lombokEnum : enums)
            lombokEnumMap.put(lombokEnum.annotation, lombokEnum.pkg);
    }
    public static Map<String, String> getLombokEnumMap(){
        return lombokEnumMap;
    }
}
