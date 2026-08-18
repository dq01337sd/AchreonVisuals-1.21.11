package ez.minar.utils.helpers;

import ez.minar.system.events.EventBus;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.UpdateEvent;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SlownessManager {
    private static final List<SlowTask> slowTasks = new CopyOnWriteArrayList<>();

    static {
        EventBus.register(new SlownessManager());
    }

    public static void addTask(SlowTask task) {
        slowTasks.add(task);
    }

    public static boolean slowTasksIsEmpty() {
        return slowTasks.isEmpty();
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        Iterator<SlowTask> iterator = slowTasks.iterator();
        while (iterator.hasNext()) {
            SlowTask task = iterator.next();
            task.delay--;
            if (task.delay <= 0) {
                if (task.runnable != null) {
                    task.runnable.run();
                }
                slowTasks.remove(task);
            }
        }
    }

    public static class SlowTask {
        public long delay;
        public Runnable runnable;
        public double slowness;

        public SlowTask(long delay, Runnable runnable) {
            this.delay = delay;
            this.runnable = runnable;
            this.slowness = 0;
        }

        public SlowTask(long delay, double slowness, Runnable runnable) {
            this.delay = delay;
            this.slowness = slowness;
            this.runnable = runnable;
        }
    }
}
