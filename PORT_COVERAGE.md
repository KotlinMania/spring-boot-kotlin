# Compiled implementation and platform coverage

The root build explicitly compiles these translated modules on JVM:

| Source directory | Compiled Kotlin implementation | Runtime tests |
| --- | --- | --- |
| `module/spring-boot-sendgrid/src/main/kotlin` | SendGrid auto-configuration and configuration properties | API-key binding, missing-property backoff, existing-bean backoff, proxy configuration |
| `module/spring-boot-jpa-test/src/main/kotlin` | Reified `TestEntityManager` extensions | Find, persist-and-get-ID, get-ID |
| `module/spring-boot-resttestclient/src/main/kotlin` | Reified REST test-client extensions | String responses and generic JSON responses through a mock HTTP transport |

These JVM APIs use the released Spring Boot 4.1.1 runtime and SendGrid 4.10.3. The runtime types exposed by the extensions are API dependencies. Kotlin remains pinned to 2.4.20 and the JVM toolchain to 21.

Run `./gradlew jvmMainClasses jvmTestClasses jvmTest jvmJar` to compile the implementation, execute its tests, and package the classes and SendGrid auto-configuration metadata. The CI aggregate includes the JVM workflow, and `build` includes JVM tests through the existing check aggregate.

The configured JavaScript, Wasm, Android and native targets and the Swift export harness remain separate KMP build checks. They currently have no translated Spring runtime in their source sets. Their success does not establish cross-platform Spring runtime support. JVM-specific Spring, JPA and HTTP types are not exported to Swift or JavaScript.

Other nested files include upstream Java modules, build logic, documentation examples, and application smoke tests. They are not added wholesale to KMP common source sets. Restoring the complete upstream multi-project build or translating its JVM runtime requires separate implementation work.
