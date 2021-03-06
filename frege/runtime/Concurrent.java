/**
 * 
 */
package frege.runtime;


/**
 * Support for cpncurrency and parallelism.
 * 
 * @author ingo
 *
 */
public class Concurrent {

	/**
     * The fork/join pool for ad hoc parallelism used by Frege runtime system..
     * 
     * @see fork(), setFregeForkJoindPool()
     */
   private static java.util.concurrent.ForkJoinPool fjpool = null;

	/**
     * <p>Set the {@link java.util.concurrent.ForkJoinPool} that should be used by the
     * Frege runtime.</p>
     * 
     * If the fork/join pool of the Frege runtime is not yet set, 
     * it will be initialized with the argument. If, however, Frege's
     * fork/join pool is already in use, it will not be given up.
     * 
     * This should be used by external code that maintains its own fork/join pool
     * <b>before</b> calling into Frege code
     * to prevent creation of another fork/join pool by the Frege runtime.
     * 
     * Note that it is not necessary to do this when Frege code is called from code 
     * that itself runs in a fork/join pool. But, in any case, it does no harm.  
     * 
     * @param pool - a fork/join pool to use
     * @return The fork/join pool the Frege runtime will actually use. 
     */
    public static java.util.concurrent.ForkJoinPool
    		setFregeForkJoinPool(java.util.concurrent.ForkJoinPool pool) {
    	synchronized (Runtime.emptyString) {
    		if (fjpool == null) {
    			fjpool = pool;
    		}
    		return fjpool;
    	}
    }

	/**
     *  <p>Evaluate <code>e</code> in <code>const e</code> in parallel. 
     *  This is a helper function for the `par` operator.</p>
     *  
     *  <p>Applies some value to the argument (which must be a {@link Lambda} that ignores
     *  that argument), to abtain a  {@link Delayed} value.</p>
     *  
     *  <p>It then checks if we run in a {@link java.util.concurrent.ForkJoinPool}.
     *  If this is so, it {@link java.util.concurrent.ForkJoinTask#fork()}s, causing the 
     *  delayed value to be evaluated asynchronously.</p>
     *  
     *  <p>If we're not running in a {@link java.util.concurrent.ForkJoinPool}, 
     *  it checks if the pool 
     *  of the Frege runtime is already initialized. 
     *  If this is not the case, a new {@link java.util.concurrent.ForkJoinPool} 
     *  will be created.</p>
     *  
     *  <p>Finally, the delayed value will be submitted to the fork/join pool 
     *  for asynchronous execution.</p>
     *  
     *  <p>A {@link Delayed} has the property that it prevents itself from being evaluated
     *  more than once. It also blocks threads that attempt parallel execution. 
     *  Once evaluated, it remembers the result and subsequent invokactions of 
     *  {@link Delayed#call()} get the evaluated value immediatedly.</p>
     *  
     *  <p>The success of parallel evaluation therefore depends on the time between 
     *  construction of the delayed expression and the time 
     *  when the value will actually be used. Ideally, it takes some CPU ressources to 
     *  evaluate a parallel computation, and it so happens that the value is only needed
     *  after it has been evaluated, to avoid wait time in the main thread.</p>  
    
     *  @param val a {@link Lambda} value to be evaluated in a fork/join context
     *  @return true
     * 
     */
    final public static boolean fork(Lambda val) {
    	Lazy a = val.apply(true).result();
        if (java.util.concurrent.ForkJoinTask.inForkJoinPool())
        	java.util.concurrent.ForkJoinTask.adapt(a).fork();
        else synchronized (Runtime.emptyString) {		// make sure 2 threads can't do that
        	if (fjpool == null) {				        // at the same time
        		fjpool = new java.util.concurrent.ForkJoinPool(
        					2 * java.lang.Runtime.getRuntime().availableProcessors());
        	}
        	fjpool.submit(a);
        }
        return true;
    }

    /**
     * <p>Monitor wait on a given object.</p>
     * <p>Because {@link Object#wait} must be run in a synchronized block,
     * we cannot just introduce it as a native function in Frege.</p>
     * <p>Basically, Frege does not know soemthing like object identity.
     * It is, however, guaranteed that, in the presence of a top level definition like:</p>
     * <pre>
     * object = "object to wait on"
     * </pre>
     * <p>the name <code>object</code> will always refer to the same string object.</p>
     * 
     * <p>
     * It is possible, that two top level values that evaluate to the same string value:</p>
     * <pre>
     * obj1 = "object"
     * obj2 = "object"
     * </pre>
     * <p>could also be the same object at runtime, hence use different string constants if
     * you want guaranteed different objects.</p> 
     * 
     * @param it - some object
     * @throws InterruptedException
     * @author ingo
     */
	final public static void waitFor(Object it) throws InterruptedException {
    	synchronized (it) {
    		it.wait();
    	}
    }

	/**
	 * <p>Notify exactly one thread that is waiting on the object</p>
	 * 
	 * <p>Because {@link Object#notify()} must be run in a synchronized block,
	 * we cannot just introduce this as native function in Frege.</p>
	 * 
	 * <p>See the discussion for  {@link Concurrent#waitFor(Object)}</p>
	 * @param it - some object
	 * @author ingo
	 */
	final public static void notifyOne(Object it) {
    	synchronized (it) {
    		it.notify();
    	}
    }

	/**
	 * <p>Notify all threads that are waiting on the object</p>
	 * 
	 * <p>Because {@link Object#notifyAll()} must be run in a synchronized block,
	 * we cannot just introduce this as native function in Frege.</p>
	 * 
	 * <p>See the discussion for  {@link Concurrent#waitFor(Object)}</p>
	 * @param it - some object
	 * @author ingo
	 */
	final public static void notifyAll(Object it) {
    	synchronized (it) {
    		it.notifyAll();
    	}
    }

}
