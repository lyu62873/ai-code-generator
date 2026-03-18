package com.leyu.aicodegenerator.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class MyBatisCodeGenerator {

    // Table to generate code
    private static final String[] TABLE_NAMES = {"chat_history_original"};

    public static void main(String[] args) {
        // Get data source
        Dict dict = YamlUtil.loadByPath("application-local.yaml");
        Map<String, Object> dataSourceConfig = dict.getByPath("spring.datasource");
        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));
        // Set data source
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // Create config
        GlobalConfig globalConfig = createGlobalConfig();

        // Use datasource and globalConfig create code generator
        Generator generator = new Generator(dataSource, globalConfig);

        // Generate code
        generator.generate();
    }

    // see: https://mybatis-flex.com/zh/others/codegen.html
    public static GlobalConfig createGlobalConfig() {
        // Create config
        GlobalConfig globalConfig = new GlobalConfig();

        // set root package, generate the codes to a temp package
        // and move them to the project.
        globalConfig.getPackageConfig()
                .setBasePackage("com.leyu.aicodegenerator.genresult");

        // set table prefix and the tables to generate codes
        // if there is no setGenerateTable, it will generate all tables
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                .setLogicDeleteColumn("isDelete");

        // generate entity and use Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        // generate mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        // generate service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        // generate controller
        globalConfig.enableController();

        // set java doc
        globalConfig.getJavadocConfig()
                .setAuthor("Lyu")
                .setSince("");
        return globalConfig;
    }
}
