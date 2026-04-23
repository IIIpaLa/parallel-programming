import java.util.concurrent.Semaphore;     
import java.util.concurrent.atomic.AtomicInteger;  


public class MySemaphore extends Semaphore {


    private final AtomicInteger permits;

    public MySemaphore(int initialPermits) {
        // Вызываем конструктор родительского класса Semaphore
    
        super(initialPermits);
        
        // Инициализируем атомарный счётчик начальным значением
        this.permits = new AtomicInteger(initialPermits);
    }

    public int acquireAndGet() throws InterruptedException {
        // Запоминаем имя потока для вывода сообщений
        String threadName = Thread.currentThread().getName();

        // Бесконечный цикл - будем пытаться до тех пор, пока не получится
        while (true) {
            // Атомарно читаем текущее количество разрешений
            int current = permits.get();

            if (current == 0) {
                Thread.yield(); 
                // Продолжаем цикл - пытаемся снова
                continue;
            }

            // Пытаемся атомарно уменьшить счётчик с current до current-1
            if (permits.compareAndSet(current, current - 1)) {
                // Вычисляем, сколько осталось после захвата
                int after = current - 1;
                
                // Выводим информацию: было -> стало
                System.out.println(threadName + " Захватил permit (" + current + " -> " + after + ")");
                
                // Возвращаем оставшееся количество разрешений
                return after;
            }
            
            // Проверяем, не был ли поток прерван извне
            if (Thread.currentThread().isInterrupted()) {
                // Если прерван - выбрасываем исключение, как требует спецификация Semaphore
                throw new InterruptedException();
            }
        }
    }

    @Override
    public void acquire() throws InterruptedException {
        acquireAndGet();  
    }

    @Override
    public void release() {
        // Получаем имя текущего потока
        String threadName = Thread.currentThread().getName();
        
        int after = permits.incrementAndGet();
        
        // Выводим информацию: новое количество разрешений
        System.out.println(threadName + " ОСВОБОДИЛ permit: " + after);
    }
}
