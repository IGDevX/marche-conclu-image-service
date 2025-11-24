# Integration Tests

Integration tests should be placed in this directory.

## Usage

Mark your integration test classes with `@IntegrationTest`:

```java
@IntegrationTest
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ImageServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");
    
    @Test
    void testWithRealDependencies() {
        // Test with real database and MinIO
    }
}
```

## Running Integration Tests

```bash
# Run only integration tests
mvn test -Dgroups=integration

# Run all tests except integration
mvn test -DexcludedGroups=integration
```

