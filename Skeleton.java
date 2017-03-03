package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{

    // To hold the class of the interface for which skeleton will handle method call requests.
    private Class<T> interfaceObject;
    // To hold the actual server object that implements the above-mentioned interface.
    private T serverObjectImplInterface;

    // ServerSocket object
    private ServerSocket serverSocket;

    // InetSocketAddress object
    private InetSocketAddress inetSocketAddress;

    // Some flags for tracking results of call:
    public static final int RESULT_GOOD = 0;
    public static final int RESULT_BAD = 1;

    // To keep track of if server has stopped listening
    private int threadState = 1; //1 if stopped 0 if running
    /** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server)
    {
        if(c == null || server == null) {
            throw new NullPointerException("Interface class and server cannot be null");
        }
        else if(RMIException.isValidRemoteInterface(c)) {
            this.interfaceObject = c;
            this.serverObjectImplInterface = server;
            this.inetSocketAddress = null;
        }
        else {
            throw new Error("All methods of this interface should throw RMI Exception");
        }
//        this(c, server, null);
    }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */



    public Skeleton(Class<T> c, T server, InetSocketAddress address)
    {
        if(server!=null && RMIException.isValidRemoteInterface(c)) {
            this.interfaceObject = c;
            this.serverObjectImplInterface = server;
            this.inetSocketAddress = address;
        }
        else if(!RMIException.isValidRemoteInterface(c)) {
            throw new Error("All methods of class: "+c.getName()+" are suppose to throw RMI Exception");
        }
        else if(server == null) {
            throw new NullPointerException("Server is null, not allowed");
        }

    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {

    }

    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        return false;

    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws RMIException
    {

        if(threadState==0) {
            return;
        }

        if(inetSocketAddress == null) {
            inetSocketAddress = new InetSocketAddress(7000);
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(inetSocketAddress);
        } catch(Exception exception) {
            throw new RMIException(exception);
        }
        threadState = 0;

        new Thread(new Runnable() {

            @Override
            public void run() {
                while(threadState == 0 && !serverSocket.isClosed()) {
                    try {
                        System.out.println("Server is running at:"+inetSocketAddress.toString());
                        Socket clientSocket = serverSocket.accept();
                        new ServerThreadCreator(interfaceObject, serverObjectImplInterface, clientSocket,
                                Skeleton.this).start();
                    }catch (SocketException e) {
                        e.printStackTrace();
                    }catch (Exception e) {
                        listen_error(e);
                        e.printStackTrace();
                    }
                }
            }
        }).start();
//        throw new UnsupportedOperationException("not implemented");
    }

    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop()
    {

        if(threadState == 1) {
            if(serverSocket.isClosed()) {
                return;
            }
        }

        threadState = 1;
        try {
            serverSocket.close();
            Thread.sleep(3);
            serverSocket.notifyAll();
            while(!serverSocket.isClosed());
            this.stopped(null);
        }catch (Exception e) {
            stopped(e);
        }
    }

    public InetSocketAddress getInetSocketAddress() {
        if(serverSocket!=null && serverSocket.isBound()) {
            return new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());
        }
        return inetSocketAddress;
    }
    public class ServerThreadCreator<T> extends Thread {
        private Socket mClientSocket;
        private T mServer;
        private Class<T> mInterfaceObject;
        private Skeleton<T> mSkeleton;

        public ServerThreadCreator(Class<T> c, T server, Socket clientSocket, Skeleton<T> s) {
            this.mClientSocket = clientSocket;
            this.mServer = server;
            this.mInterfaceObject = c;
            this.mSkeleton = s;
        }

        private Boolean validInterface(Class<T> c,String mClassName) {
            Class[] mySuperInterfaceClasses = c.getInterfaces();

            if (mClassName.equals(c.getName()))
                return true;
            int size = mySuperInterfaceClasses.length;
            int i = 0;
            while(i<size) {
                if(mClassName.equals(mySuperInterfaceClasses[i].getName())) {
                    if(mClassName!=null) {
                        return true;
                    }
                }
                i++;
            }
            return false;
        }
        /**
         * Handle remote client request.
         */
        private synchronized void waitForClient() {
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            try {
                out = new ObjectOutputStream(mClientSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(mClientSocket.getInputStream());

                // prepare for method invoke
                String interfaceName = (String) in.readObject();
                String methodName  = (String) in.readObject();

                Class[] parameterTypesList  = (Class[]) in.readObject();
                Object[] parameterValues  = (Object[]) in.readObject();

                if(validInterface(mInterfaceObject, interfaceName)) {
                    // invoke method on server
                    Method method = mInterfaceObject.getMethod(methodName, parameterTypesList);
                    Object result = method.invoke(mServer, parameterValues);

                    // write the result back to client
                    out.writeObject(Skeleton.RESULT_GOOD);
                    out.writeObject(result);

                    mClientSocket.close();
                }
                else {
                    out.writeObject(Skeleton.RESULT_BAD);
                    out.writeObject(new RMIException("Interface method mismatch"));
                    mClientSocket.close();
                    return;
                }

            } catch (InvocationTargetException e) {
                try {
                    out.writeObject(Skeleton.RESULT_BAD);
                    out.writeObject(e.getCause());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            } catch (Exception e) {
                try {
                    mSkeleton.service_error(new RMIException("ServiceError"));
                    out.writeObject(Skeleton.RESULT_BAD);
                    out.writeObject(e);
                } catch (IOException e1) {
                    // ignore
                }
            } finally {
                try {
                    if(mClientSocket != null && !mClientSocket.isClosed()) {
                        mClientSocket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public synchronized void run() {

            waitForClient();
        }
    }
}
