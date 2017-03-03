package rmi;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
/** RMI stub factory.

    <p>
    RMI stubs hide network communication with the remote server and provide a
    simple object-like interface to their users. This class provides methods for
    creating stub objects dynamically, when given pre-defined interfaces.

    <p>
    The network address of the remote server is set when a stub is created, and
    may not be modified afterwards. Two stubs are equal if they implement the
    same interface and carry the same remote server address - and would
    therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub
{
    /** Creates a stub, given a skeleton with an assigned adress.

        <p>
        The stub is assigned the address of the skeleton. The skeleton must
        either have been created with a fixed address, or else it must have
        already been started.

        <p>
        This method should be used when the stub is created together with the
        skeleton. The stub may then be transmitted over the network to enable
        communication with the skeleton.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose network address is to be used.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned an
                                      address by the user and has not yet been
                                      started.
        @throws UnknownHostException When the skeleton address is a wildcard and
                                     a port is assigned, but no address can be
                                     found for the local host.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
        throws UnknownHostException
    {
        // Doing all the null checks
        if(c==null || skeleton == null) {
            throw new NullPointerException("Interface class and skeleton cannot be null");
        }
        if(!RMIException.isValidRemoteInterface(c)) {
            throw new Error("The interface given is not a Remote Interface (not throwing RMI Exception)");
        }
        InetSocketAddress remoteAddress = skeleton.getInetSocketAddress();
        if(remoteAddress == null) {
            throw new IllegalStateException();
        }
        return CreateProxyInterface(c, remoteAddress);

    }

    /** Creates a stub, given a skeleton with an assigned address and a hostname
        which overrides the skeleton's hostname.

        <p>
        The stub is assigned the port of the skeleton and the given hostname.
        The skeleton must either have been started with a fixed port, or else
        it must have been started to receive a system-assigned port, for this
        method to succeed.

        <p>
        This method should be used when the stub is created together with the
        skeleton, but firewalls or private networks prevent the system from
        automatically assigning a valid externally-routable address to the
        skeleton. In this case, the creator of the stub has the option of
        obtaining an externally-routable address by other means, and specifying
        this hostname to this method.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose port is to be used.
        @param hostname The hostname with which the stub will be created.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned a
                                      port.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname)
    {
        // checking for edge cases
        if(skeleton == null || c == null || hostname == null) {
            throw new NullPointerException("The interface class, skeleton, or hostname cannot be null");
        }

        InetSocketAddress address  = skeleton.getInetSocketAddress();
        if(address == null) {
            throw new IllegalStateException("No port exists for this skeleton");
        }

        if(!RMIException.isValidRemoteInterface(c)) {
            throw new Error("The interface given is not a Remote Interface");
        }

        InetSocketAddress remoteAddress = new InetSocketAddress(hostname, address.getPort());

        return CreateProxyInterface(c, remoteAddress);
    }

    /** Creates a stub, given the address of a remote server.

        <p>
        This method should be used primarily when bootstrapping RMI. In this
        case, the server is already running on a remote host but there is
        not necessarily a direct way to obtain an associated stub.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param address The network address of the remote skeleton.
        @return The stub created.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
        // checking for edge cases
        if(address == null || c == null) {
            throw new NullPointerException("The interface class and address cannot be null");
        }

        if(!RMIException.isValidRemoteInterface(c)) {
            throw new Error("The interface given is not a Remote Interface.");
        }

        return CreateProxyInterface(c, address);
    }


    private static <T> T CreateProxyInterface(Class<T> c, InetSocketAddress address) {
        Class[] classArr = new Class[]{c};
        ClassLoader classLoader = c.getClassLoader();
        StubInvocationHandler stubInvocationHandler = new StubInvocationHandler(address, c.getName());
        return (T) Proxy.newProxyInstance(classLoader, classArr, stubInvocationHandler);

    }


//    public static <T> T lookup(Class<T> c, String key, InetSocketAddress address) {
//        // Looking up for remote object in RMIregistry
//        RMIInterface registryOfStub = Stub.create(RMIInterface.class,address);
//        try {
//            InetSocketAddress tempAddress = (InetSocketAddress) registryOfStub.lookupIP(key);
//            return (T)CreateProxyInterface(c, tempAddress);
//
//        }
//        catch(Exception e) {
//        }
//        return null;
//    }


//    public static <T> void bind(InetSocketAddress registryAddress, String key, InetSocketAddress address) {
//        // Binding remote object with RMIregistry
//        RMIInterface registryOfStub = Stub.create(RMIInterface.class,registryAddress);
//        try {
//            System.out.println("key is: "+key);
//            System.out.println("Address is:"+address);
//            registryOfStub.bind(key,address);
//        }
//        catch (Exception e) {
//            if(registryOfStub == null) {
//                System.out.println("registryOfStub is null");
//            }
//            else {
//                System.out.println("registryOfStub is not null");
//            }
//            e.printStackTrace();
//        }
//    }
    static class StubInvocationHandler implements InvocationHandler, Serializable {

        // remote Address of Skeleton
        public InetSocketAddress remoteAddress;
        // remote interface className
        public String className;

        // Constructor
        public StubInvocationHandler(InetSocketAddress address, String cName) {
            this.remoteAddress = address;
            this.className = cName;
            System.out.println(this.remoteAddress.toString());
        }


//    private static final long serialVersionUID = -713214545986L;

        // It takes proxy object, method and arguments and return the output of RMI call
        // It creates a network connection with Skeleton, passes the function name and returns the result
        @SuppressWarnings("resource")
        private Object remoteInvoke(Object proxy, Method method, Object[] args) throws Throwable {

            Socket socket   = null;
            Integer res     = null;
            Object obj      = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            // Opening connection with Skeleton
            try {
                socket = new Socket();
                socket.connect(remoteAddress);
                System.out.println("Address in SIH: "+remoteAddress.toString());

                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();

                in = new ObjectInputStream(socket.getInputStream());

                // Sending ClassName, methods and argument list
                out.writeObject(method.getDeclaringClass().getName());
                out.writeObject(method.getName());
                out.writeObject(method.getParameterTypes());
                out.writeObject(args);
                out.flush();

                // Reading the object back
                res     = (Integer) in.readObject();
                obj     = in.readObject();

                // Closing the socket
                socket.close();
                // Catching the Exception
            } catch (Exception e) {
                throw new RMIException(e);
            } finally {
                if(socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }

            if(res == Skeleton.RESULT_BAD) {
                throw (Throwable) obj;
            }

            return obj;
        }

        // Equals method
        private Boolean equals(Object proxy, Method method, Object[] args) {
            // Verified attributes of equal methods
            if(args == null || args[0] == null) {
                return false;
            }
            if(args.length != 1) {
                return false;
            }

            Object obj = args[0];
            if(obj == null) {
                return false;
            }

            if(proxy.getClass().equals(obj.getClass()) == false) {
                return false;
            }

            InvocationHandler handler = Proxy.getInvocationHandler(obj);
            if(!(handler instanceof StubInvocationHandler)) {
                return false;
            }

            if(!Proxy.isProxyClass(obj.getClass())) {
                return false;
            }

            if(!remoteAddress.equals(((StubInvocationHandler) handler).remoteAddress)) {
                return false;
            }

            return Boolean.TRUE;
        }

        // toString Method
        @Override
        public String toString() {
            String cName = className;
            String hName = remoteAddress.getHostString();
            String pNum = String.valueOf(remoteAddress.getPort());
            String returnVal = "ClassName: "+cName;
            returnVal+= " HostName: "+hName;
            returnVal+= " Port: "+pNum;
            return returnVal;
        }

        private int hashCode(StubInvocationHandler handler, Object proxy) {
            int handlerCode = handler.remoteAddress.hashCode();
            int proxyCode = proxy.getClass().hashCode();
            return (handlerCode+proxyCode);
        }

        // Performs a localInvoke on methods equals or toString or hashCode
        private Object localInvoke(Object proxy, Method method, Object[] args) throws Throwable{

            // Calls toString method
            if(method.getName().equals("toString")) {
                return toString();
            }

            // return true if two proxies were created for the same skeleton
            if(method.getName().equals("equals"))  {
                return equals(proxy, method, args);
            }

            StubInvocationHandler handler = (StubInvocationHandler) Proxy.getInvocationHandler(proxy);

            // Calls hashCode method
            if(method.getName().equals("hashCode")) {
                return hashCode(handler, proxy);
            }

            return method.invoke(handler, args);
        }

        // invoke is called on any method call to proxy object
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                // if remoteCall then does a remoteInvoke else local invoke
                if(RMIException.isThrowingRMI(method)) {
                    return remoteInvoke(proxy, method, args);
                } else {
                    return localInvoke(proxy, method, args);
                }
            } catch (Exception e) {
                throw e;
            }
        }

    }
}
