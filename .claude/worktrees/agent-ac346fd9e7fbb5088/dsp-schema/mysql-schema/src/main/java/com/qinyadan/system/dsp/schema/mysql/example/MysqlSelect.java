package com.qinyadan.system.dsp.schema.mysql.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static com.qinyadan.system.dsp.schema.common.constants.MetaConstants.META_MODEL;


public class MysqlSelect {
    public static void main(String[] args) {
        try {
            final Properties info = new Properties();
            info.setProperty(META_MODEL,
                    "/Users/liuzm/workspace/Storage/dsp/dsp-schema/mysql-schema/src/main/resources/mysql.json");

            Connection connection =
                    DriverManager.getConnection("jdbc:calcite:", info);

            Statement statement = connection.createStatement();

            //CodeGen模式下太耗时了....
            //需要引进volcano模型的MPP模式
            for (int i = 0; i < 1; i++) {
                //FIXME 支持中文条件查询
                ResultSet r = statement.executeQuery("select * from test.t1 where varchar_type like '%cn%' limit 10");

                while (r.next()) {
                    System.out.println(r.getString(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
