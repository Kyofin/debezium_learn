package com.data.debeziun;

import io.debezium.config.Configuration;
import io.debezium.connector.mysql.MySqlConnector;
import io.debezium.data.Envelope;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.List;
import java.util.Map;

import static io.debezium.data.Envelope.FieldName.*;
import static java.util.stream.Collectors.toMap;


/**
 * @program: debezium_learn
 * @author: huzekang
 * @create: 2022-03-08 16:28
 **/
public class Play {
    public static void main(String[] args) {

        final Play play = new Play();
        final Configuration configuration = play.buildConfig();
        final DebeziumEngine debeziumEngine = play.debeziumServerBootstrap(configuration);
        debeziumEngine.run();

    }

   private Configuration  buildConfig(){
        final Configuration configuration = Configuration.create()
//            连接器的Java类名称
                .with("connector.class", MySqlConnector.class.getName())
//            偏移量持久化，用来容错 默认值
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
//                偏移量持久化文件路径 默认/tmp/offsets.dat  如果路径配置不正确可能导致无法存储偏移量 可能会导致重复消费变更
//                如果连接器重新启动，它将使用最后记录的偏移量来知道它应该恢复读取源信息中的哪个位置。
                .with("offset.storage.file.filename", "/Volumes/Samsung_T5/opensource/debezium_learn/offset/offsets.dat")
//                捕获偏移量的周期
                .with("offset.flush.interval.ms", "6000")
//               连接器的唯一名称
                .with("name", "mysql-connector")
//                数据库的hostname
                .with("database.hostname", "localhost")
//                端口
                .with("database.port", "3307")
//                用户名
                .with("database.user", "root")
//                密码
                .with("database.password", "debezium")
//                 包含的数据库列表
                .with("database.include.list", "inventory")
//                是否包含数据库表结构层面的变更，建议使用默认值true
                .with("include.schema.changes", "false")
//                mysql.cnf 配置的 server-id
                .with("database.server.id", "123454")
//                 MySQL 服务器或集群的逻辑名称
                .with("database.server.name", "customer-mysql-db-server")
//                历史变更记录
                .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
//                历史变更记录存储位置
                .with("database.history.file.filename", "/Volumes/Samsung_T5/opensource/debezium_learn/history/dbhistory.dat")
                .build();

       return configuration;
    }


    private DebeziumEngine debeziumServerBootstrap(io.debezium.config.Configuration configuration) {

        return DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(configuration.asProperties())
                .notifying(this::handlePayload)
                .build();
    }


    private void handlePayload(List<RecordChangeEvent<SourceRecord>> recordChangeEvents, DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> recordCommitter) {
        recordChangeEvents.forEach(r -> {
            SourceRecord sourceRecord = r.record();
            Struct sourceRecordChangeValue = (Struct) sourceRecord.value();

            if (sourceRecordChangeValue != null) {
                // 判断操作的类型 过滤掉读 只处理增删改   这个其实可以在配置中设置
                Envelope.Operation operation = Envelope.Operation.forCode((String) sourceRecordChangeValue.get(OPERATION));

                if (operation != Envelope.Operation.READ) {
                    String record = operation == Envelope.Operation.DELETE ? BEFORE : AFTER;
                    // 获取增删改对应的结构体数据
                    Struct struct = (Struct) sourceRecordChangeValue.get(record);
                    // 将变更的行封装为Map
                    Map<String, Object> payload = struct.schema().fields().stream()
                            .map(Field::name)
                            .filter(fieldName -> struct.get(fieldName) != null)
                            .map(fieldName -> Pair.of(fieldName, struct.get(fieldName)))
                            .collect(toMap(Pair::getKey, Pair::getValue));
                    // 这里简单打印一下
                    System.out.println("payload = " + payload);
                }
            }
        });
    }

}


