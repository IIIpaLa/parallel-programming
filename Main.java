import java.util.*;
import java.util.concurrent.*;

public class Main {

    // Количество потоков
    public static final int THREADS = 50;

    // Количество операций в каждом потоке
    public static final int ITERATIONS = 100000;

    // Перевод наносекунд в секунды (для вывода времени)
    public static final double NSEC = 1_000_000_000.0;

    // Количество различных ключей 
    public static final int MAP_SIZE = 3;

    // Количество повторов измерения 
    public static final int SAMPLES = 5;

    public static void main(String[] args) {

        System.out.println("Коллекции:");

        // Запуск тестирования разных реализаций Map
        double hashMapTime = compute(new HashMap<>());
        double hashTableTime = compute(new Hashtable<>());
        double syncMapTime = compute(Collections.synchronizedMap(new HashMap<>()));
        double cHashMapTime = compute(new ConcurrentHashMap<>());

        // Вывод времени выполнения
        System.out.println("\nВремя выполнения:");
        System.out.printf("\tHashMap: %.3f с\n", hashMapTime);
        System.out.printf("\tHashtable: %.3f с\n", hashTableTime);
        System.out.printf("\tSynchronizedMap: %.3f с\n", syncMapTime);
        System.out.printf("\tConcurrentHashMap: %.3f с\n", cHashMapTime);
    }


    private static double compute(Map<String, Integer> map) {

        // Выводим тип коллекции, которую сейчас тестируем
        System.out.print("\t" + map.getClass().getSimpleName());

        long totalTime = 0;

        // Повторяем тест несколько раз для усреднения результата
        for (int k = 0; k < SAMPLES; k++) {

            // Очищаем Map перед каждым тестом
            map.clear();

            // Засекаем время начала теста
            long start = System.nanoTime();

            // Создаём пул потоков фиксированного размера
            ExecutorService executor = Executors.newFixedThreadPool(THREADS);

            // Список задач, которые будут выполнены потоками
            List<Callable<Void>> tasks = new ArrayList<>();

            // СОЗДАНИЕ ЗАДАЧ ДЛЯ ПОТОКОВ
            for (int i = 0; i < THREADS; i++) {

                tasks.add(() -> {

                    // Каждый поток выполняет ITERATIONS операций
                    for (int j = 0; j < ITERATIONS; j++) {

                        // Генерация ключа:
                        // всего MAP_SIZE ключей => высокая конкуренция между потоками
                        String key = "key" + (j % MAP_SIZE);

                         

                            // merge() выполняет атомарное обновление:
                            // если ключа нет → вставит 1
                            // если есть → увеличит значение на 1
                            map.merge(key, 1, Integer::sum);
                        
                    }

                    return null;
                });
            }

            // ЗАПУСК ВСЕХ ПОТОКОВ
            try {
                executor.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Завершаем пул потоков
            executor.shutdown();

            // Ждём завершения всех задач
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Засекаем конец выполнения
            long stop = System.nanoTime();

            // Суммируем время выполнения всех прогонов
            totalTime += (stop - start);
        }

        // Сумма всех значений в Map
        int total = map.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        // Ожидаемое значение:
        int expected = THREADS * ITERATIONS;

        System.out.printf(" | сумма = %d (ожидалось = %d)", total, expected);

        // Если сумма не совпала → значит была потеря данных (race condition)
        if (total != expected) {
            System.out.print(" ❌ ОШИБКА");
        } else {
            System.out.print(" ✅ ОК");
        }

        System.out.println();

        // Возвращаем среднее время выполнения в секундах
        return totalTime / (double) SAMPLES / NSEC;
    }
}