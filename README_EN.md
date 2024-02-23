
[![LICENSE](https://img.shields.io/badge/license-Apache%202-blue)](https://github.com/paxoscn/mysql-protocol/blob/master/LICENSE)
[![Language](https://img.shields.io/badge/Language-Java-green)](https://www.oracle.com/java/technologies/)

Chinese Readme: [README.md](https://github.com/paxoscn/mysql-protocol/blob/master/README.md)

## A MySQL Protocol Implementation in Java

Forked from https://github.com/mheath/netty-mysql-codec

This project is useful on:

- Implementing a database and making it ready to connect with MySQL compatible clients.
- Proxy an arbitrary data source and make it act as a MySQL server.
- Studying on MySQL Protocol.

## Quick start

### Run unit tests

To make sure the downloaded codes are complete, you can run [MySqlListenerTest.java](https://github.com/paxoscn/mysql-protocol/blob/master/src/test/java/cn/paxos/mysql/MySqlListenerTest.java) to see if it can be passed successfully.

The test first runs a MySQL server with any queries resulting 'Hello World !'. Then it raises a JDBC connection and send a SQL to the server, checks the result and finally closes the server.

### Create your own MySQL server

The first thing you need to do is implementing a [SqlEngine](https://github.com/paxoscn/mysql-protocol/blob/master/src/main/java/cn/paxos/mysql/engine/SqlEngine.java)

```java
public class YourSqlEngine implements SqlEngine {
}
```

To respond SQLs from the client, you need to implement query().

```java
@Override
public void query(ResultSetWriter resultSetWriter, String database, String userName, byte[] scramble411, byte[] authSeed, String sql) throws IOException {
    // Build a result returning just one string column named 'col1'
    // writeColumns() must be called before any invoking of writeRow()
    resultSetWriter.writeColumns(List.of(new QueryResultColumn("col1", "varchar(255)")));

    // Return just one row with content 'Hello World !'
    resultSetWriter.writeRow(List.of("Hello World !"));

    // Do not forget to finish the response
    resultSetWriter.finish();
}
```

To protect your server from disallowed visiting, you need to implement authenticate().

```java
@Override
public void authenticate(String database, String userName, byte[] scramble411, byte[] authSeed) throws IOException {
    // Block user if not named 'github'
    String validUser = "github";
    if (!userName.equals(validUser)) {
        throw new IOException(new IllegalAccessException("Authentication failed: User " + userName + " is not allowed to connect"));
    }
    
    // Check if the password is '123456'
    String validPassword = "123456";
    
    // SHA1 and encode the password
    String validPasswordSha1 = SHAUtils.SHA(validPassword, SHAUtils.SHA_1);
    String validScramble411WithSeed20 = Utils.scramble411(validPasswordSha1, authSeed);
    
    // Compare and throw an exception if needed
    if (!Utils.compareDigest(validScramble411WithSeed20, Base64.getEncoder().encodeToString(scramble411))) {
        throw new IOException(new IllegalAccessException("Authentication failed: Validation failed"));
    }
}
```

Once the engine definition is ready, you can pass it into [MySqlListener](https://github.com/paxoscn/mysql-protocol/blob/master/src/main/java/cn/paxos/mysql/MySqlListener.java) and then it will start listening a TCP port.

```java
// Start and listen port 3307
new MySqlListener(3307, 100, new YourSqlEngine());

// For test you can sleep for a while so you have enough time to connect to it.
Thread.sleep(1000L * 60 * 10);
```

After starting of the server, you can open any favorite MySQL client to connect to it.

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

## License

This project is under the Apache 2.0 license. See the [LICENSE](./LICENSE) file for details.

## Acknowledgments

- Thanks [mheath](https://github.com/mheath) for providing beautiful implementation codes.
- Contact - Wechat: 95634620 Mail: [unrealwalker@126.com](mailto:unrealwalker@126.com)