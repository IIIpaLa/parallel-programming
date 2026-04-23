#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <xmmintrin.h>  // SSE интринсики

#define ITERATIONS 1000000
#define ARRAY_SIZE 4

// Вариант 1: С использованием SSE через ассемблерную вставку
void sse_asm(float a[], float b[], float c[]) {
    asm volatile (
        "movups %[a], %%xmm0\n"      
        "movups %[b], %%xmm1\n"      
        "mulps %%xmm1, %%xmm0\n"     
        "movups %%xmm0, %[c]\n"      
        : 
        : [a]"m"(*a), [b]"m"(*b), [c]"m"(*c)
        : "%xmm0", "%xmm1", "memory"
    );
}

// Вариант 2: Обычное последовательное умножение
void scalar_mult(float a[], float b[], float c[]) {
    for (int i = 0; i < ARRAY_SIZE; i++) {
        c[i] = a[i] * b[i];
    }
}

// Функция для вывода массива
void print_array(float arr[], const char* name) {
    printf("%s = [", name);
    for (int i = 0; i < ARRAY_SIZE; i++) {
        printf("%.2f", arr[i]);
        if (i < ARRAY_SIZE - 1) printf(", ");
    }
    printf("]\n");
}

// Функция для проверки корректности
int check_result(float c1[], float c2[]) {
    for (int i = 0; i < ARRAY_SIZE; i++) {
        if (c1[i] != c2[i]) {
            return 0;  // ошибка
        }
    }
    return 1;  // всё правильно
}

int main() {
    float a[ARRAY_SIZE], b[ARRAY_SIZE];
    float c_asm[ARRAY_SIZE], c_scalar[ARRAY_SIZE];
    
    clock_t start, end;
    double time_asm, time_scalar;
    
    // Инициализация массивов случайными числами
    srand(time(NULL));
    for (int i = 0; i < ARRAY_SIZE; i++) {
        a[i] = (float)(rand() % 100) / 10.0f;  // числа от 0.0 до 9.9
        b[i] = (float)(rand() % 100) / 10.0f;  // числа от 0.0 до 9.9
    }
    
    printf("Исходные массивы:\n");
    print_array(a, "a");
    print_array(b, "b");
    printf("\n");
    
    // Проверка корректности всех методов
    sse_asm(a, b, c_asm);
    scalar_mult(a, b, c_scalar);
    
    printf("Результаты умножения:\n");
    print_array(c_asm, "c_asm (SSE)");
    print_array(c_scalar, "c_scalar");
    
    if (check_result(c_asm, c_scalar)) {
        printf("\n✓ Оба метода дают одинаковый результат\n");
    } else {
        printf("\n✗ Ошибка: результаты различаются!\n");
        return 1;
    }
    
    printf("\n--- Тестирование производительности (%d итераций) ---\n", ITERATIONS);
    
    // Тест SSE через ассемблер
    start = clock();
    for (int i = 0; i < ITERATIONS; i++) {
        sse_asm(a, b, c_asm);
    }
    end = clock();
    time_asm = ((double)(end - start)) / CLOCKS_PER_SEC * 1000;  // в миллисекундах
    
    // Тест скалярного умножения
    start = clock();
    for (int i = 0; i < ITERATIONS; i++) {
        scalar_mult(a, b, c_scalar);
    }
    end = clock();
    time_scalar = ((double)(end - start)) / CLOCKS_PER_SEC * 1000;
    
    // Вывод результатов производительности
    printf("SSE (asm):      %.3f ms\n", time_asm);
    printf("Scalar:         %.3f ms\n", time_scalar);
    
    return 0;
}