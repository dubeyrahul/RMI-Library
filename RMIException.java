package rmi;
import java.lang.reflect.Method;
import java.lang.*;

/** RMI exceptions. */
public class RMIException extends Exception
{
    /** Creates an <code>RMIException</code> with the given message string. */
    public RMIException(String message)
    {
        super(message);
    }

    /** Creates an <code>RMIException</code> with a message string and the given
        cause. */
    public RMIException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /** Creates an <code>RMIException</code> from the given cause. */
    public RMIException(Throwable cause)
    {
        super(cause);
    }

    public static boolean isValidRemoteInterface(Class c) {
        // If it is not an interface at all then return false
        if(!c.isInterface())
            return false;

        // Check my super class interfaces for "isValidRemoteInterface"
        Class[] mySuperInterfaceClasses = c.getInterfaces();
        for(int i=0; i<mySuperInterfaceClasses.length; i++) {
            if(!isValidRemoteInterface(mySuperInterfaceClasses[i])) {
                return false;
            }
        }
//        for(Class class_: mySuperInterfaceClasses) {
//            if(!isValidRemoteInterface(class_))
//                return false;
//        }

        // Check my methods for Exception type:
        Method[] myMethods = c.getDeclaredMethods();
        for(Method method: myMethods) {

            if (!isThrowingRMI(method)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isThrowingRMI(Method method) {
        Class[] exceptionClasses = method.getExceptionTypes();
        for(Class eClass: exceptionClasses) {
            if(eClass.equals(RMIException.class)) {
                    return true;
            }
        }
        return false;
    }
}
