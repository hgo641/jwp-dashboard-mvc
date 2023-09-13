package reflection;

import com.sun.nio.sctp.PeerAddressChangeNotification.AddressChangeEvent;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class Junit3TestRunner {

    @Test
    void run() throws Exception {
        Class<Junit3Test> clazz = Junit3Test.class;
        final var junit3 = (Junit3Test) clazz.getConstructor().newInstance();
        
        // TODO Junit3Test에서 test로 시작하는 메소드 실행
        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.getName().startsWith("test")) {
                method.invoke(junit3);
            }
        }
    }
}
