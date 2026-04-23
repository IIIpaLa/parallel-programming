import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    
    private static final int PORT = 12346;

    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Сервер запущен на порту " + PORT);

            // Чтение команд с консоли сервера (администратора)
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                // Бесконечный цикл: сервер всегда готов принять команду от админа
                while (true) {
                    try {
                        // Ждем ввода текста от администратора
                        String serverMessage = scanner.nextLine();
                        // Рассылаем введенный текст всем подключенным клиентам
                        broadcast("[SERVER]: " + serverMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start(); // Запускаем поток

            // Прием новых подключений от клиентов
            while (true) {
                // accept() блокирует выполнение и ждет, пока кто-то подключится
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Новый клиент подключён: " + clientSocket.getInetAddress());

                // Создаем обработчик для этого конкретного клиента
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                
                // Добавляем клиента в общий список
                clients.add(clientHandler);
                
                // Запускаем обработчик в отдельном потоке, чтобы он не блокировал прием других клиентов
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод рассылки сообщения всем клиентам из списка

    public static void broadcast(String message) {

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }


    // Метод удаления клиента из списка

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }


    private static class ClientHandler implements Runnable {
        private final Socket socket;      // Сокет соединения с клиентом
        private PrintWriter out;          // Поток для отправки данных клиенту
        private BufferedReader in;        // Поток для чтения данных от клиента
        private String username;          // Имя пользователя

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Инициализируем потоки ввода-вывода
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 1. Запрашиваем имя у клиента
                out.println("Введите ваше имя:");
                // readLine() блокирует поток и ждет, пока клиент отправит имя
                username = in.readLine();
                if (username == null || username.isEmpty()) username = "Anonymous";

                System.out.println(" " + username + " подключился");
                // Уведомляем всех, что новый пользователь зашел
                broadcast("📢 [" + username + "] присоединился к чату");

                // 2. Чтения сообщений от клиента
                String message;
                // Цикл работает, пока клиент не отключится (readLine вернет null)
                while ((message = in.readLine()) != null) {
                    System.out.println("[" + username + "]: " + message);
                    // Пересылаем сообщение от этого клиента всем остальным
                    broadcast("[" + username + "]: " + message);
                }
            } catch (IOException e) {
                // Если возникла ошибка чтения (клиент закрыл программу)
                System.out.println("️ Клиент " + username + " отключился");
            } finally {
                // Блок finally выполняется всегда, даже при ошибке
                closeConnection();
            }
        }

        // Метод отправки сообщения конкретному клиенту
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        // Метод корректного завершения соединения
        private void closeConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
                
                // Удаляем себя из списка активных клиентов на сервере
                removeClient(this);
                
                if (username != null) {
                    broadcast("📢 [" + username + "] покинул чат");
                    System.out.println("👋 " + username + " отключился");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}