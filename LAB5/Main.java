import java.util.concurrent.*;    
import java.util.ArrayList;         
import java.util.List;              

public class Main {
    // Количество потоков, которые будут одновременно запущены
    public static final int THREADS = 4;
    
    // Количество разрешений семафора (сколько потоков могут работать одновременно)
    public static final int COUNT = 2;

    // Создаём экземпляр нашего самодельного семафора с COUNT разрешениями
    public static MySemaphore mySemaphore = new MySemaphore(COUNT);
    
    // Создаём экземпляр стандартного семафора Java с COUNT разрешениями
    public static Semaphore regSemaphore = new Semaphore(COUNT);

    public static void main(String[] args) {
        System.out.println("");
        System.out.println("Стандартный семафор:");
        // Запускаем тест со стандартным семафором
        runTask(regSemaphore, "Regular");

        System.out.println("");
        System.out.println("Мой семафор:");
        // Запускаем тест с самодельным семафором
        runTask(mySemaphore, "My");
    }
 
    public static void runTask(Semaphore semaphore, String name) {
        // Создаём пул потоков фиксированного размера (THREADS штук)
        ExecutorService es = Executors.newFixedThreadPool(THREADS);

        // Список задач, которые будут выполняться потоками
        List<Callable<String>> tasks = new ArrayList<>();
        
        // Создаём THREADS одинаковых задач
        for (int i = 0; i < THREADS; i++) {
            
            tasks.add(() -> {
                // Получаем имя текущего потока 
                String threadName = Thread.currentThread().getName();
                
                try {
                    // Сообщаем, что поток пытается получить доступ к семафору
                    System.out.println(threadName + " поток пытается войти");
                    
                    int remaining;  // Сколько разрешений осталось после захвата
                    
                    // Проверяем тип семафора
                    if (semaphore instanceof MySemaphore) {
                        remaining = ((MySemaphore) semaphore).acquireAndGet();
                    } else {
                        // Если это стандартный семафор - просто захватываем разрешение
                        semaphore.acquire();
                        // Запрашиваем, сколько разрешений осталось доступно
                        remaining = semaphore.availablePermits();
                    }
                    
                    // Поток успешно вошёл 
                    System.out.println(threadName + " Вошел, доступно разрешений: " + remaining);
                    
                    // Имитируем полезную работу
                    Thread.sleep(100);
                    
                    // Поток выходит из критической секции
                    System.out.println(threadName + " поток выходит");
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // Освобождаем разрешение семафора, увеличивая счётчик
                    semaphore.release();
                }
                return "Thread " + threadName + " done";
            });
        }
        
        try {
            // Запускаем все задачи в пуле потоков и ждём завершения ВСЕХ
            List<Future<String>> results = es.invokeAll(tasks);
            
            // Проходим по всем результатам
            for (Future<String> result : results) {
                result.get();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            // Если main был прерван или в задаче возникла ошибка
            e.printStackTrace();
        }
        
        // Корректно завершаем пул потоков (не принимает новые задачи)
        es.shutdown();
    }
}
