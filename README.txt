================================================================================
                    OPENTELE STACKTRACE TRACKER
                        Redis Persistence Setup
================================================================================

1. OVERVIEW
===========
This application tracks stack traces using OpenTelemetry and persists the data
using Redis. The data is accessible through Swagger UI for easy testing.

2. REDIS SETUP
==============

2.1 Start Redis Server
   - Install Redis from: https://redis.io/download
   - Or use Docker:
     docker run --name redis -d -p 6379:6379 redis:latest
   
   - Verify Redis is running:
     redis-cli ping
     (Should return: PONG)

2.2 Redis Configuration
   - Default host: localhost
   - Default port: 6379
   - Configuration file: src/main/resources/application.properties
   
   Properties to customize if needed:
   - spring.data.redis.host=localhost
   - spring.data.redis.port=6379
   - spring.data.redis.timeout=60000ms
   - spring.data.redis.jedis.pool.max-active=8

3. BUILDING THE APPLICATION
============================

3.1 Build with Maven:
   mvn clean install
   
3.2 Build with Maven (skip tests):
   mvn clean install -DskipTests

3.3 Run the Application:
   mvn spring-boot:run
   
   Or build JAR and run:
   mvn clean package
   java -jar target/opentele-stacktrace-1.0.0.jar

4. ACCESSING SWAGGER UI
=======================

4.1 Once the application is running, access Swagger UI:
   - URL: http://localhost:5080/swagger-ui.html
   
4.2 Swagger API Documentation:
   - URL: http://localhost:5080/v3/api-docs

4.3 Application Health Check:
   - URL: http://localhost:5080/api/health

5. API ENDPOINTS
================

5.1 Health Check
   Endpoint: GET /api/health
   Response: Service status
   
   curl http://localhost:5080/api/health

5.2 Track Error (Persists to Redis)
   Endpoint: POST /api/track-error
   Parameter: message (required)
   
   Example:
   curl -X POST "http://localhost:5080/api/track-error?message=TestError"
   
   Response: Tracked error with timestamp and stacktrace details

5.3 Get All Tracked Stack Traces (From Redis)
   Endpoint: GET /api/tracked-stacktraces
   
   Example:
   curl http://localhost:5080/api/tracked-stacktraces
   
   Response: Count and list of all stack traces from Redis

5.4 Get Stack Traces by IP (From Redis)
   Endpoint: GET /api/tracked-stacktraces/ip/{ip}
   
   Example:
   curl http://localhost:5080/api/tracked-stacktraces/ip/192.168.1.1
   
   Response: All stack traces from specific IP stored in Redis

5.5 Clear All Stack Traces (From Redis)
   Endpoint: DELETE /api/clear-stacktraces
   
   Example:
   curl -X DELETE http://localhost:5080/api/clear-stacktraces
   
   Response: Confirmation that all data is cleared from Redis

5.6 Documentation Page
   Endpoint: GET /api/documentation
   Response: HTML documentation page

6. USING SWAGGER UI TO TEST
============================

6.1 Open in Browser:
   http://localhost:5080/swagger-ui.html

6.2 Test POST /api/track-error:
   - Click on the endpoint
   - Click "Try it out"
   - Enter message parameter: "Test Error"
   - Click "Execute"
   - View the response (data is now stored in Redis)

6.3 Test GET /api/tracked-stacktraces:
   - Click on the endpoint
   - Click "Try it out"
   - Click "Execute"
   - View all tracked errors from Redis

6.4 Test GET /api/tracked-stacktraces/ip/{ip}:
   - Click on the endpoint
   - Click "Try it out"
   - Enter IP: "127.0.0.1"
   - Click "Execute"
   - View errors from specific IP in Redis

6.5 Test DELETE /api/clear-stacktraces:
   - Click on the endpoint
   - Click "Try it out"
   - Click "Execute"
   - All Redis data is cleared

7. REDIS DATA PERSISTENCE
==========================

7.1 Data Structure:
   - Keys: stacktrace:{uuid}
   - Index Set: stacktraces:index
   - IP Index: stacktraces:ip:{ip_address}

7.2 Verify Data in Redis:
   Connect to Redis CLI:
   redis-cli
   
   Commands:
   - KEYS stacktrace:*        (View all stack trace keys)
   - SMEMBERS stacktraces:index    (View all trace IDs)
   - SMEMBERS stacktraces:ip:127.0.0.1  (View traces by IP)
   - GET stacktrace:{id}      (View specific trace data)

7.3 Data Persistence:
   - Data persists across application restarts
   - Use "flushall" in Redis CLI to clear all data
   - Redis saves data to disk (depending on Redis configuration)

8. RUNNING TESTS
================

8.1 Run all tests:
   mvn test

8.2 Run specific test:
   mvn test -Dtest=StackTraceTrackerServiceTest

8.3 Run tests with coverage:
   mvn test jacoco:report

9. OPENTELEMETRY TRACING
========================

9.1 OpenTelemetry Spans:
   - Each error tracking creates a span named "track-stacktrace"
   - Spans include attributes: ip, error.type, error.message
   - Spans are exported to logging

9.2 Logging Configuration:
   - All OpenTelemetry traces are logged
   - Log level: INFO (configured in application.properties)

10. TROUBLESHOOTING
===================

10.1 Redis Connection Issues:
    - Verify Redis is running: redis-cli ping
    - Check Redis host/port in application.properties
    - Ensure port 6379 is accessible

10.2 Build Failures:
    - Ensure Java 17+ is installed: java -version
    - Ensure Maven is installed: mvn -version
    - Clear Maven cache: mvn clean

10.3 Swagger Not Loading:
    - Verify application is running: http://localhost:5080/api/health
    - Check firewall settings for port 5080
    - Check logs for any startup errors

10.4 Data Not Persisting:
    - Verify Redis is running and connected
    - Check Redis configuration in logs
    - Use redis-cli to verify data

11. PROJECT STRUCTURE
=====================

src/main/java/com/opentele/stacktrace/
├── Application.java                          (Main Spring Boot Application)
├── OpenTelemetryConfig.java                  (OpenTelemetry Configuration)
├── RedisConfig.java                          (Redis Configuration)
├── StackTraceController.java                 (REST API Endpoints with Swagger)
├── StackTraceData.java                       (Data Model)
├── StackTraceTrackerService.java             (Business Logic)
└── RedisStackTracePersistenceService.java    (Redis Persistence Logic)

src/test/java/com/opentele/stacktrace/
├── ApplicationTest.java
├── StackTraceControllerTest.java
└── StackTraceTrackerServiceTest.java

src/main/resources/
├── application.properties                    (Application Configuration)
└── how.html                                  (Documentation)

12. VERSION INFORMATION
=======================

- Spring Boot: 4.0.0
- Java Version: 17+
- OpenTelemetry Version: 1.36.0
- Redis Version: Latest (6.0+)
- Maven Version: 3.6+

13. FEATURES
============

✓ OpenTelemetry Integration
✓ Redis Data Persistence
✓ Swagger UI Documentation
✓ Stack Trace Tracking
✓ IP-based Filtering
✓ Automatic Timestamp Recording
✓ Spring Boot 4.0.0 Support
✓ Comprehensive API Endpoints
✓ Full Unit Test Coverage

14. SUPPORT
===========

For issues or questions:
- Check logs in target/logs/ directory
- Review OpenTelemetry documentation: https://opentelemetry.io
- Check Redis documentation: https://redis.io/docs
- Check Spring Data Redis docs: https://spring.io/projects/spring-data-redis

================================================================================
                        Setup Complete!
            Start Redis, run the application, and visit:
                   http://localhost:5080/swagger-ui.html
================================================================================
