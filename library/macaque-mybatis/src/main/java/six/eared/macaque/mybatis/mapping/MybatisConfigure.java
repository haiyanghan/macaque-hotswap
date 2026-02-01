package six.eared.macaque.mybatis.mapping;

import io.github.hhy50.linker.annotations.Field;
import io.github.hhy50.linker.annotations.Method;
import io.github.hhy50.linker.annotations.Target;
import io.github.hhy50.linker.annotations.Typed;

import java.util.Map;

@Target.Bind("org.apache.ibatis.session.Configuration")
public interface MybatisConfigure {

    @Field.Getter("environment")
    Object getEnvironment();

    @Field.Getter("mappedStatements")
    Map<String, Object> mappedStatements();

    @Method.InvokeSuper
    @Method.Expr("mappedStatements.put(..)")
    void putStatement(String key, @Typed("java.lang.Object") MybatisStatement value);

    Object getSqlFragments();
}
