package com.phd.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connect {
    /**
     * Connect to a sample database
     * @param dbLocation
     */
    public static Connection getConnection(String dbLocation) {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:C:/DB/SDP1.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static void closeConnection(Connection conn) {

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void main(String[] args) {
        getConnection("test");
    }

}
