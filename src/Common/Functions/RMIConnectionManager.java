package Common.Functions;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public record RMIConnectionManager<T>(String host, int port, String serviceName) {

    public T connect(Class<T> stubClass) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            T candidate = stubClass.cast(registry.lookup(serviceName));

            // Test Connection with ping()
            if ((Boolean) candidate.getClass().getMethod("ping").invoke(candidate)) {
                //System.out.println("[RMI Connection Manager] Connected to Service '" + serviceName + "'.");
                return candidate;
            } else {
                System.err.println("[RMI Connection Manager] Service '" + serviceName + "' did not respond to ping.");
            }

        } catch (Exception e) {
            //DEBUG
            //System.err.println("[RMI Connection Manager] Error: Failed to connect to Service '" + serviceName + "': " + e.getMessage());
            //e.printStackTrace();
        }
        return null;
    }
}