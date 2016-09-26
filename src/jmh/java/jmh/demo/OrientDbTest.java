package jmh.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Anindya Chatterjee.
 */
public class OrientDbTest {

    @State(Scope.Benchmark)
    public static class TestState {
        private OObjectDatabaseTx db;
        private Person[] personList;

        @Setup(Level.Trial)
        public void setUp() throws IOException {
            System.out.println("started setup code");
            try {
                personList = loadData();
                deleteDir("/tmp/orientdb/");
                db = new OObjectDatabaseTx("plocal:/tmp/orientdb/person");
                if (db.exists()) {
                    db.open("admin", "admin");
                    db.drop();
                }
                db.create();
                db.getEntityManager().registerEntityClass(Person.class);
                db.getEntityManager().registerEntityClass(Address.class);
                db.getEntityManager().registerEntityClass(PrivateData.class);
            } catch (Throwable e) {
                System.out.println("error in creating db ");
                e.printStackTrace();
            }
        }

        @TearDown(Level.Trial)
        public void cleanUp() {
            System.out.println("started cleanup code");
            if (db != null) {
                ODatabaseRecordThreadLocal.INSTANCE.set(db.getUnderlying());
                db.commit();
                db.close();
            }
        }

        private void deleteDir(String dirName) {
            File file = new File(dirName);
            if (file.exists()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        if (child.isDirectory()) {
                            deleteDir(child.getAbsolutePath());
                        } else {
                            child.delete();
                        }
                    }
                    file.delete();
                } else {
                    file.delete();
                }
            }
        }

        private Person[] loadData() throws IOException {
            InputStream inputStream = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream("data.json");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(inputStream, Person[].class);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Fork(0)
    public void benchmarkInsert(TestState state, Blackhole blackhole) {
        OObjectDatabaseTx db = state.db;
        Person[] personList = state.personList;

        if (db == null) {
            System.out.println("db null.. exiting");
            System.exit(0);
        }

        ODatabaseRecordThreadLocal.INSTANCE.set(db.getUnderlying());
        for (Person person : personList) {
            blackhole.consume(db.save(person));
        }
    }
}
