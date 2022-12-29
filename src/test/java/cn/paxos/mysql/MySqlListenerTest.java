/*
 * Copyright 2022 paxos.cn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.paxos.mysql;

import cn.paxos.mysql.engine.QueryResultColumn;
import cn.paxos.mysql.engine.SqlEngine;
import cn.paxos.mysql.util.SHAUtils;
import cn.paxos.mysql.util.Utils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MySqlListenerTest {

    /**
     * Start a server and connect to it using JDBC
     *
     * @throws Exception
     */
    @Test
    void selfConnect() throws Exception {
        // Listen port 3307
        int port = 3307;
        // Preset database name
        String database = "dummy_db";
        // Preset user name
        String user = "github";
        // Preset password
        String password = "123456";

        // Start the server
        new MySqlListener(port, 100, new SqlEngine() {

            // Implement the authentication
            @Override
            public void authenticate(String database, String userName, byte[] scramble411, byte[] authSeed) throws IOException {
                // Print useful information
                System.out.println("Database: " + database + ", User: " + userName);

                // Check if the password is valid
                authenticateSimply(database, userName, scramble411, authSeed);
            }

            @Override
            public void query(ResultSetWriter resultSetWriter, String database, String userName, byte[] scramble411, byte[] authSeed, String sql) throws IOException {
                // Print useful information
                System.out.println("Database: " + database + ", User: " + userName + ", SQL: " + sql);

                // Check if the password is valid
                authenticateSimply(database, userName, scramble411, authSeed);

                // Build and respond columns
                List<QueryResultColumn> columns = List.of(
                        new QueryResultColumn("col1", "varchar(255)"));
                resultSetWriter.writeColumns(columns);

                // Build and respond rows
                List<String> row = List.of("Hello World !");
                resultSetWriter.writeRow(row);

                // Finish the response
                resultSetWriter.finish();
            }

            // Just check if the password equal to the preset
            private void authenticateSimply(String database, String userName, byte[] scramble411, byte[] authSeed) throws IOException {
                // SHA1 and encode the password
                String validPasswordSha1 = SHAUtils.SHA(password, SHAUtils.SHA_1);
                String validScramble411WithSeed20 = Utils.scramble411(validPasswordSha1, authSeed);

                // Use utils to compare the password
                if (!Utils.compareDigest(validScramble411WithSeed20, Base64.getEncoder().encodeToString(scramble411))) {
                    // Throw an exception if the checking failed
                    throw new IOException(new IllegalAccessException("Authentication failed: Digest validation failed"));
                }
            }
        });

        // Raise a connection to the server
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:" + port + "/" + database, user,  password);
             // Query an arbitrary SQL
             PreparedStatement ps = conn.prepareStatement("select * from dummy_table");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Check the result
                assertEquals("Hello World !", rs.getString(1));
            }
        }
    }
}