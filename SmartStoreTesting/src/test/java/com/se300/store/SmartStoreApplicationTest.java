package com.se300.store;

import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmartStoreApplicationTest {

    @Test
    @DisplayName("startNonBlocking starts Tomcat and stop shuts it down without error")
    void startNonBlockingThenStop() throws Exception {
        SmartStoreApplication app = new SmartStoreApplication();

        // This exercises startNonBlocking -> startServer(false)
        // including DataManager, repositories, services, controllers, and
        // Tomcat.start()
        app.startNonBlocking();

        // This exercises the stop() path with non-null tomcat
        app.stop();
    }

    @Test
    @DisplayName("stop does nothing when Tomcat is null")
    void stopWithNullTomcatDoesNothing() throws Exception {
        SmartStoreApplication app = new SmartStoreApplication();
        // tomcat is null by default; just ensure no exception
        app.stop();
    }

    @Test
    @DisplayName("shutdown with null Tomcat logs and does not throw")
    void shutdownWithNullTomcatDoesNotThrow() throws Exception {
        SmartStoreApplication app = new SmartStoreApplication();

        Method shutdown = SmartStoreApplication.class.getDeclaredMethod("shutdown");
        shutdown.setAccessible(true);

        // With tomcat still null, this should just run and not throw
        shutdown.invoke(app);

        // Just sanity check tomcat is still null after shutdown
        Field tomcatField = SmartStoreApplication.class.getDeclaredField("tomcat");
        tomcatField.setAccessible(true);
        Object tomcat = tomcatField.get(app);
        assertNull(tomcat);
    }

    @Test
    @DisplayName("shutdown catches exceptions from Tomcat.stop and logs error")
    void shutdownCatchesExceptionFromTomcat() throws Exception {
        SmartStoreApplication app = new SmartStoreApplication();

        // Mock Tomcat object and force stop() to throw
        Tomcat mockTomcat = mock(Tomcat.class);
        doThrow(new RuntimeException("boom")).when(mockTomcat).stop();

        // Inject mock into private 'tomcat' field
        Field tomcatField = SmartStoreApplication.class.getDeclaredField("tomcat");
        tomcatField.setAccessible(true);
        tomcatField.set(app, mockTomcat);

        // Call private shutdown(), which should:
        // - enter the "if (tomcat != null)" branch
        // - catch the exception and NOT rethrow it
        Method shutdown = SmartStoreApplication.class.getDeclaredMethod("shutdown");
        shutdown.setAccessible(true);
        shutdown.invoke(app);

        // Ensure stop() was actually called on our mock
        verify(mockTomcat).stop();

        // tomcat field should still be non-null (we don't assert destroy() behavior
        // here)
        Object tomcatAfter = tomcatField.get(app);
        assertNotNull(tomcatAfter);
    }
}
