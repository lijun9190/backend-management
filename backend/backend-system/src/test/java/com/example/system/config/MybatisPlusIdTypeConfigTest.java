// package com.example.system.config;
//
// import org.junit.jupiter.api.Test;
//
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.nio.file.Path;
//
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// class MybatisPlusIdTypeConfigTest {
//
//     @Test
//     void systemAndAuthShouldForceAutoIncrementIdType() throws Exception {
//         assertContainsAutoIdType(Path.of("src", "main", "resources", "application.yml"));
//         assertContainsAutoIdType(Path.of("..", "backend-auth", "src", "main", "resources", "application.yml"));
//     }
//
//     private void assertContainsAutoIdType(Path path) throws Exception {
//         String source = Files.readString(path.normalize(), StandardCharsets.UTF_8);
//         assertTrue(
//                 source.contains("id-type: auto") || source.contains("id-type: AUTO"),
//                 () -> path + " should explicitly configure mybatis-plus global id-type as auto"
//         );
//     }
// }
