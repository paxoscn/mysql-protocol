
[![LICENSE](https://img.shields.io/badge/license-Apache%202-blue)](https://github.com/paxoscn/mysql-protocol/blob/master/LICENSE)
[![Language](https://img.shields.io/badge/Language-Java-green)](https://www.oracle.com/java/technologies/)

README in English: [README_EN.md](https://github.com/paxoscn/mysql-protocol/blob/master/README_EN.md)

## MySQL协议Java实现

改造自https://github.com/mheath/netty-mysql-codec

这个项目适用于:

- 实现自有数据库时并允许任何MySQL兼容客户端进行连接.
- 代理任何数据源并将其封装为MySQL服务.
- 学习MySQL协议.

## 快速开始

### 运行单元测试

为了确保代码下载完整, 你可以运行[MySqlListenerTest.java](https://github.com/paxoscn/mysql-protocol/blob/master/src/test/java/cn/paxos/mysql/MySqlListenerTest.java)
来确认单元测试是否可以正确运行.

这个单元测试会启动一个MySQL服务，对它的任何查询都只会返回'Hello World !'. 随后它会启动一个JDBC访问并发送一句SQL到服务器, 检查查询结果并关闭服务.

### 创建自己的MySQL服务

首先你需要实现[SqlEngine](https://github.com/paxoscn/mysql-protocol/blob/master/src/main/java/cn/paxos/mysql/engine/SqlEngine.java)

```java
public class YourSqlEngine implements SqlEngine {
}
```

要对客户端发过来的SQL进行响应，你需要实现query().

```java
@Override
public void query(ResultSetWriter resultSetWriter, String database, String userName, byte[] scramble411, byte[] authSeed, String sql) throws IOException {
    // 在结果集中放入一列，名为'col1'
    // 该调用必须在任意一次writeRow()之前
    resultSetWriter.writeColumns(List.of(new QueryResultColumn("col1", "varchar(255)")));

    // 在结果集中放入一行，值为'Hello World !'
    resultSetWriter.writeRow(List.of("Hello World !"));

    // 最后需要调用finish()方法结束响应
    resultSetWriter.finish();
}
```

为了保护你的服务不被未授权人员访问, 你还需要实现authenticate().

```java
@Override
public void authenticate(String database, String userName, byte[] scramble411, byte[] authSeed) throws IOException {
    // 只允许用户名'github'
    String validUser = "github";
    if (!userName.equals(validUser)) {
        throw new IOException(new IllegalAccessException("Authentication failed: User " + userName + " is not allowed to connect"));
    }
    
    // 只允许密码'123456'
    String validPassword = "123456";
    
    // 对密码进行SHA1及编码
    String validPasswordSha1 = SHAUtils.SHA(validPassword, SHAUtils.SHA_1);
    String validScramble411WithSeed20 = Utils.scramble411(validPasswordSha1, authSeed);
    
    // 比较密码编码结果
    if (!Utils.compareDigest(validScramble411WithSeed20, Base64.getEncoder().encodeToString(scramble411))) {
        throw new IOException(new IllegalAccessException("Authentication failed: Validation failed"));
    }
}
```

实现完毕后, 你可以把实现类传入[MySqlListener](https://github.com/paxoscn/mysql-protocol/blob/master/src/main/java/cn/paxos/mysql/MySqlListener.java)，当它实例化后会自动监听指定TCP端口.

```java
// 启动并监听端口3307
new MySqlListener(3307, 100, new YourSqlEngine());

// 为了有足够时间来进行客户端连接，可以在这里sleep一段时间.
Thread.sleep(1000L * 60 * 10);
```

启动服务后, 你可以用任何MySQL客户端来进行连接，以下以MySQL命令行客户端为例.

```shell
mysql -h127.0.0.1 -P3307 -ugithub -p123456 dummy_db
> select * from dummy_table;
+---------------+
| col1          |
+---------------+
| Hello World ! |
+---------------+
1 row in set (0.006 sec)
```

## 开源协议

本项目使用Apache 2.0协议. 详情请见: [LICENSE](./LICENSE).

## 告知

- 感谢 [mheath](https://github.com/mheath) 提供的基础代码.
- 联系方式 - 微信: 95634620 邮件: [unrealwalker@126.com](mailto:unrealwalker@126.com)