package six.eared.macaque.agent.test.mybatis;

import io.github.hhy50.linker.LinkerFactory;
import io.github.hhy50.linker.annotations.Method;
import io.github.hhy50.linker.annotations.Target;
import io.github.hhy50.linker.exceptions.LinkerException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Test;
import six.eared.macaque.agent.hotswap.handler.ClassHotSwapHandler;
import six.eared.macaque.common.util.FileUtil;
import six.eared.macaque.mbean.rmi.HotSwapRmiData;
import six.eared.macaque.mbean.rmi.RmiResult;
import six.eared.macaque.mybatis.mapping.MybatisStatement;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class TestUpdateSqlStatement extends six.eared.macaque.agent.test.Env  {

    private ClassHotSwapHandler classHotSwapHandler = new ClassHotSwapHandler();

    private SqlSessionFactory sqlSessionFactory;

    public TestUpdateSqlStatement() throws IOException, LinkerException {
        try (InputStream is = TestUpdateSqlStatement.class.getClassLoader()
                .getResourceAsStream("mybatis/mybatis-config.xml");) {
            sqlSessionFactory = LinkerFactory.createLinker(SqlSessionFactory.class, new SqlSessionFactoryBuilder().build(is));
        }
    }

    interface SqlSessionFactory {
        @Method.Expr("configuration.mappedStatements.get(..)")
        MybatisStatement2 getStatement(String key);
    }

    @Target.Bind("org.apache.ibatis.mapping.MappedStatement")
    interface MybatisStatement2 extends MybatisStatement {
        @Method.Expr("sqlSource.getBoundSql(..)")
        BoundSql getBoundSql(Object parameter);
    }


    @Test
    public void test1() {
        String nameSpace = "six.eared.macaque.agent.test.mybatis.UserMapper.getById";
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(sqlSessionFactory.getStatement(nameSpace).getBoundSql(null).getSql(),
                    "select 1");
            RmiResult result = classHotSwapHandler.handlerRequest(new HotSwapRmiData("UserMapper.xml", "xml",
                    FileUtil.is2bytes(TestUpdateSqlStatement.class.getClassLoader().getResourceAsStream("mybatis/UserMapper.xml.1")),
                    Collections.emptyMap()));
            if (!result.isSuccess()) {
                throw new RuntimeException(result.getMessage());
            }
            Assert.assertEquals(sqlSessionFactory.getStatement(nameSpace).getBoundSql(null).getSql(),
                    "select 2");
            result = classHotSwapHandler.handlerRequest(new HotSwapRmiData("UserMapper.xml", "xml",
                    FileUtil.is2bytes(TestUpdateSqlStatement.class.getClassLoader()
                            .getResourceAsStream("mybatis/mapper/UserMapper.xml")),
                    Collections.emptyMap()));
            if (!result.isSuccess()) {
                throw new RuntimeException(result.getMessage());
            }
        }
    }
}
