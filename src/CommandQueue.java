import java.util.*;

public class CommandQueue<t> {
    LinkedList<Object> queue = new LinkedList<Object>();
    
    public synchronized void addWork(Object objs) {
        if (objs instanceof Object) {
            queue.addLast(objs);
            notify();
        }
    }

    public synchronized Object getWork() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        return queue.removeFirst();
    }
}