import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    // Адрес и порт сервера, к которому подключаемся
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12346;

    public static void main(String[] args) {
        try {
            // 1. Устанавливаем соединение с сервером
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("✅ Подключено к серверу!");

            // Настраиваем потоки для общения через сокет
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Scanner для чтения ввода с клавиатуры пользователя
            Scanner scanner = new Scanner(System.in);

            new Thread(() -> {
                try {
                    String response;
                    // Бесконечно ждем сообщения от сервера
                    while ((response = in.readLine()) != null) {
                        // Как только пришло сообщение — сразу выводим его на экран
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    System.out.println("⚠️ Соединение с сервером разорвано");
                }
            }).start();


            // Ввод имени и отправка сообщений
            
            // Сначала просим пользователя ввести имя
            System.out.println("Введите ваше имя:");
            String username = scanner.nextLine();
            // Отправляем имя на сервер (сервер ждет его первым сообщением)
            out.println(username);

            // Цикл отправки сообщений
            String userInput;
            while (true) {
                // Ждем ввода от пользователя
                userInput = scanner.nextLine();
                
                // Если ввели /exit — выходим из цикла и завершаем программу
                if (userInput.equalsIgnoreCase("/exit")) {
                    break;
                }
                
                // Отправляем введенный текст на сервер
                out.println(userInput);
            }

            // 3. Корректное завершение работы клиента
            System.out.println("Отключение...");
            scanner.close();
            in.close();
            out.close();
            socket.close();

        } catch (IOException e) {
            System.err.println("❌ Ошибка подключения: " + e.getMessage());
        }
    }
}