#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <math.h>
#include <sys/time.h>


int counter = 0;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

/**
 * Функция для измерения текущего времени в секундах
 */
double get_time() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec + tv.tv_usec / 1000000.0;
}

/**
 * Длительная вычислительная операция
 */
void long_running_task(int task_num) {
    // Вычисление квадратных корней - процессорозатратная операция
    double result = 0;
    for (int i = 0; i < 1e8; i++) {
        result += sqrt(i);
    }
    
    if (result < 0) printf("Error in task %d\n", task_num);
}


/**
 * Функция для потока в многопоточной версии
 */
void *thread_task(void *arg) {
    int thread_num = *((int*)arg);
    
    printf("\tПоток %d начал работу\n", thread_num);
    
    pthread_mutex_lock(&mutex);
    counter++;
    printf("\t\tПоток %d увеличил счетчик до %d\n", thread_num, counter);
    pthread_mutex_unlock(&mutex);
    
    // Та же длительная операция, что и в последовательной версии
    long_running_task(thread_num);
    
    printf("\tПоток %d завершил работу\n", thread_num);
    
    free(arg);
    return NULL;
}


/**
 * СРАВНЕНИЕ ПРОИЗВОДИТЕЛЬНОСТИ
 */
void compare_performance(int n) {
    printf("\n=== СРАВНЕНИЕ ПРОИЗВОДИТЕЛЬНОСТИ ===\n");
    printf("Количество потоков: %d\n", n);
    printf("Каждая задача: 100 млн итераций с sqrt()\n\n");
    
    // 1. Последовательное выполнение
    printf("Замер последовательного выполнения...\n");
    double seq_start = get_time();
    
    for (int i = 0; i < n; i++) {
        long_running_task(i);
    }
    
    double seq_end = get_time(); // вычисление времени
    double seq_time = seq_end - seq_start;
    printf("  Время: %.3f сек\n", seq_time);
    
    // 2. Многопоточное выполнение
    printf("Замер многопоточного выполнения...\n");
    
    pthread_t threads[n];
    counter = 0;  
    
    double par_start = get_time();
    
    // СОЗДАНИЕ ПОТОКОВ
    for (int i = 0; i < n; i++) {
        int *thread_num = (int*)malloc(sizeof(int));
        *thread_num = i;
        pthread_create(&threads[i], NULL, thread_task, thread_num);
    }
    
    for (int i = 0; i < n; i++) {
        pthread_join(threads[i], NULL);
    }
    
    double par_end = get_time();
    double par_time = par_end - par_start;
    printf("  Время: %.3f сек\n", par_time);
    
    // 3. Анализ результатов
    printf("\n--- АНАЛИЗ РЕЗУЛЬТАТОВ ---\n");
    double speedup = seq_time / par_time;
    double efficiency = (seq_time / (par_time * n)) * 100;
    
    printf("Ускорение: %.2fx\n", speedup);
    printf("Эффективность: %.1f%%\n", efficiency);
    
}

int main(int argc, char** argv) {
        
    int n = atoi(argv[1]);
    
    printf("ПРОГРАММА ДЛЯ СРАВНЕНИЯ ПОСЛЕДОВАТЕЛЬНОГО И МНОГОПОТОЧНОГО ВЫПОЛНЕНИЯ\n");
    printf("=====================================================================\n");
    printf("Параметры: %d задач(и) по 100 млн итераций sqrt()\n\n", n);
    
    
    // Cравнение производительности
    counter = 0;  // Сброс счетчика
    compare_performance(n);
    
    // Очистка
    pthread_mutex_destroy(&mutex);
    
    printf("\nПрограмма завершена.\n");
    return 0;
}

// gcc -O2 -pthread Pthreads.c -o Pthreads -lm
// ./Pthreads 1
